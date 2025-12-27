package com.example.movieticketbookingapp.ui.booking

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.movieticketbookingapp.R
import com.example.movieticketbookingapp.adapter.Seat
import com.example.movieticketbookingapp.adapter.SeatAdapter
import com.example.movieticketbookingapp.adapter.SeatStatus
import com.example.movieticketbookingapp.model.Movie
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FirebaseFirestore
import java.text.DecimalFormat

class SeatSelectionActivity : AppCompatActivity() {

    private lateinit var rvSeat: RecyclerView
    private lateinit var btnConfirm: MaterialButton
    private lateinit var tvTitle: TextView
    private lateinit var tvSeatInfo: TextView

    // Firebase
    private lateinit var db: FirebaseFirestore
    private var showtimeId: String = ""

    // Dữ liệu ghế
    private val allSeats = ArrayList<Seat>() // Danh sách quản lý toàn bộ 100 ghế
    private val selectedSeats = ArrayList<Seat>() // Danh sách ghế đang chọn
    private val pricePerSeat = 50000.0 // Giá vé mặc định
    private var listSoldSeats: Set<Long> = HashSet()   // Danh sách ghế đã bán thật (BookedSeats)
    private var listLockedSeats: Set<Long> = HashSet() // Danh sách ghế đang bị người khác giữ (Locks)
    private var myLockedSeatIds: ArrayList<String> = ArrayList()

    // Adapter
    private lateinit var adapter: SeatAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_booking_seat_select)

        // 1. Khởi tạo Firestore
        db = FirebaseFirestore.getInstance()

        initViews()

        // 2. Nhận dữ liệu từ BookingActivity
        val movie = intent.getParcelableExtra<Movie>("movie_data")
        showtimeId = intent.getStringExtra("showtime_id") ?: "" // ID suất chiếu để lắng nghe realtime
        clearMyOldLocks()


        // 3. Tạo danh sách ghế (Ban đầu giả định là Trống hết)
        initSeatList()

        // 4. Setup giao diện lưới ghế
        setupRecyclerView()

        // Nếu có ai đặt ghế, hàm này sẽ chạy và cập nhật giao diện ngay lập tức
        if (showtimeId.isNotEmpty()) {
            clearMyOldLocks()
            startRealtimeUpdates()
        }

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        // 6. Xử lý nút Xác nhận
        btnConfirm.setOnClickListener {
            if (selectedSeats.isEmpty()) {
                Toast.makeText(this, "Vui lòng chọn ít nhất 1 ghế!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val showtimeRef = db.collection("showtimes").document(showtimeId)
            val locksRef = showtimeRef.collection("locks")

            // 1. CHUẨN BỊ DỮ LIỆU (Tính toán trước các thông tin cần thiết)
            val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            val expireTimeSeconds = com.google.firebase.Timestamp.now().seconds + 300
            val expireTimestamp = com.google.firebase.Timestamp(expireTimeSeconds, 0)

            val seatsToLock = ArrayList(selectedSeats) // Copy danh sách ghế muốn mua
            val seatIds = ArrayList<String>()
            val seatNames = ArrayList<String>()

            // Vòng lặp này chỉ để tính toán Tên ghế (A1, B2) và ID để gửi sang màn hình sau
            // Không thực hiện lệnh DB nào ở đây cả
            for (seat in seatsToLock) {
                seatIds.add(seat.id.toString())

                // Logic tính tên ghế (Code cũ của bạn giữ nguyên)
                val rowChar = (seat.id / 10 + 65).toChar()
                val colIndex = seat.id % 10
                var realSeatNumber = colIndex + 1
                if (colIndex > 2) realSeatNumber -= 1
                if (colIndex > 7) realSeatNumber -= 1
                seatNames.add("$rowChar$realSeatNumber")
            }

            // 2. CHẠY TRANSACTION (Thay thế cho Batch)
            db.runTransaction { transaction ->
                // --- BƯỚC A: ĐỌC VÀ KIỂM TRA (READ) ---
                // Kiểm tra xem tất cả ghế mình chọn có bị ai tranh mất chưa
                for (seat in seatsToLock) {
                    val seatIdStr = seat.id.toString()
                    val lockDoc = transaction.get(locksRef.document(seatIdStr)) // Đọc dữ liệu mới nhất

                    if (lockDoc.exists()) {
                        val holderId = lockDoc.getString("userId")
                        val expireAt = lockDoc.getTimestamp("expireAt")?.seconds ?: 0
                        val currentTime = com.google.firebase.Timestamp.now().seconds

                        // Nếu ghế đang bị người khác giữ và còn hạn -> HỦY GIAO DỊCH NGAY
                        if (expireAt > currentTime && holderId != userId) {
                            throw com.google.firebase.firestore.FirebaseFirestoreException(
                                "Ghế ${seatNames[seatsToLock.indexOf(seat)]} đã có người chọn!",
                                com.google.firebase.firestore.FirebaseFirestoreException.Code.ABORTED
                            )
                        }
                    }
                }

                // --- BƯỚC B: GHI DỮ LIỆU (WRITE) ---
                // Nếu code chạy được xuống đây nghĩa là ghế an toàn -> Tiến hành ghi

                // B1. Xóa các ghế cũ (Logic dọn dẹp ghế cũ của bạn đưa vào Transaction cho an toàn)
                for (oldId in myLockedSeatIds) {
                    // Nếu ghế cũ không nằm trong danh sách ghế mới -> Xóa nó đi
                    if (!seatIds.contains(oldId)) {
                        transaction.delete(locksRef.document(oldId))
                    }
                }

                // B2. Ghi khóa mới cho các ghế đang chọn
                for (seat in seatsToLock) {
                    val lockData = hashMapOf(
                        "userId" to userId,
                        "expireAt" to expireTimestamp
                    )
                    transaction.set(locksRef.document(seat.id.toString()), lockData)
                }

                null // Return null báo hiệu thành công
            }.addOnSuccessListener {
                // --- THÀNH CÔNG ---
                // Cập nhật lại danh sách theo dõi cục bộ
                myLockedSeatIds.clear()
                myLockedSeatIds.addAll(seatIds)

                // Chuyển màn hình (Code cũ giữ nguyên)
                val intent = Intent(this, ReviewTicketActivity::class.java)
                intent.putExtra("movie_data", movie)
                intent.putExtra("movie_title", movie?.title)
                intent.putExtra("poster_url", movie?.posterUrl)
                intent.putExtra("cinema_name", getIntent().getStringExtra("cinema_name"))
                intent.putExtra("selected_date", getIntent().getStringExtra("selected_date"))
                intent.putExtra("selected_time", getIntent().getStringExtra("selected_time"))
                intent.putExtra("showtime_id", showtimeId)
                intent.putStringArrayListExtra("seat_ids", seatIds)
                intent.putStringArrayListExtra("seat_names", seatNames)
                intent.putExtra("total_price", selectedSeats.size * pricePerSeat)
                intent.putExtra("expire_time_seconds", expireTimeSeconds)

                startActivity(intent)

            }.addOnFailureListener { e ->
                // --- THẤT BẠI ---
                btnConfirm.isEnabled = true
                btnConfirm.text = "SELECT SEAT"

                if (e is com.google.firebase.firestore.FirebaseFirestoreException &&
                    e.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.ABORTED) {
                    // Lỗi do tranh chấp ghế (Transaction tự hủy)
                    Toast.makeText(this, e.message, Toast.LENGTH_LONG).show()
                    // Lúc này hàm Realtime Updates ở dưới sẽ tự chạy và tô vàng ghế bị mất
                } else {
                    // Lỗi mạng hoặc lỗi khác
                    Toast.makeText(this, "Lỗi kết nối: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun initViews() {
        rvSeat = findViewById(R.id.rvSeat)
        btnConfirm = findViewById(R.id.btnConfirmSeat)
        tvTitle = findViewById(R.id.tvTitle)
        tvSeatInfo = findViewById(R.id.tvSeatInfo)
    }

    // Tạo danh sách 100 ghế ban đầu (Chưa biết cái nào đã đặt)
    private fun initSeatList() {
        allSeats.clear()
        val totalRows = 10
        val totalColumns = 10

        for (i in 0 until (totalRows * totalColumns)) {
            val column = i % totalColumns

            // Logic Lối đi (Cột 2 và 7)
            val status = if (column == 2 || column == 7) {
                SeatStatus.AISLE
            } else {
                SeatStatus.AVAILABLE // Mặc định là Available, sẽ update lại sau khi load từ Firebase
            }
            allSeats.add(Seat(i, status))
        }
    }

    private fun setupRecyclerView() {
        adapter = SeatAdapter(allSeats) { clickedSeat ->
            // Logic chọn/bỏ chọn ghế
            if (clickedSeat.status == SeatStatus.SELECTED) {
                selectedSeats.add(clickedSeat)
            } else {
                selectedSeats.remove(clickedSeat)
            }
            updateSeatInfoText()
        }
        rvSeat.adapter = adapter
        rvSeat.layoutManager = GridLayoutManager(this, 10) // 10 cột
    }

    private fun startRealtimeUpdates() {
        val showtimeRef = db.collection("showtimes").document(showtimeId)
        val locksRef = showtimeRef.collection("locks")

        // 1. LẮNG NGHE GHẾ ĐÃ BÁN (BOOKED)
        showtimeRef.addSnapshotListener { snapshot, e ->
            if (e != null || snapshot == null) return@addSnapshotListener

            val bookedList = snapshot.get("bookedSeats") as? List<Long> ?: emptyList()
            listSoldSeats = bookedList.toHashSet() // Lưu vào biến tạm

            // Mỗi khi dữ liệu thay đổi -> Vẽ lại ghế
            refreshSeatMap()
        }

        // 2. LẮNG NGHE GHẾ ĐANG GIỮ (LOCKS)
        locksRef.addSnapshotListener { snapshot, e ->
            if (e != null || snapshot == null) return@addSnapshotListener

            val tempLocked = HashSet<Long>()
            val currentTime = com.google.firebase.Timestamp.now().seconds
            val myUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid

            for (doc in snapshot.documents) {
                val expireAt = doc.getTimestamp("expireAt")?.seconds ?: 0
                val holderId = doc.getString("userId")
                val seatId = doc.id.toLongOrNull()

                // Logic lọc: Nếu còn hạn VÀ không phải của mình -> Coi là đang khóa
                if (seatId != null && expireAt > currentTime && holderId != myUserId) {
                    tempLocked.add(seatId)
                }
            }
            listLockedSeats = tempLocked // Lưu vào biến tạm

            // Mỗi khi dữ liệu thay đổi -> Vẽ lại ghế
            refreshSeatMap()
        }
    }

    private fun clearMyOldLocks() {
        val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return
        val locksRef = db.collection("showtimes").document(showtimeId).collection("locks")

        // Tìm tất cả ghế đang được giữ bởi MÌNH
        locksRef.whereEqualTo("userId", userId).get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.isEmpty) {
                    val batch = db.batch()
                    for (doc in snapshot.documents) {
                        batch.delete(doc.reference)
                    }
                    batch.commit()
                    // Log: Đã dọn dẹp ghế kẹt
                }
            }
    }

    private fun refreshSeatMap() {
        var isChanged = false

        for (seat in allSeats) {
            // Bỏ qua lối đi
            if (seat.status == SeatStatus.AISLE) continue

            val id = seat.id.toLong()

            // 1. Ưu tiên kiểm tra GHẾ ĐÃ BÁN trước (Màu Xám)
            if (listSoldSeats.contains(id)) {
                if (seat.status != SeatStatus.BOOKED) {
                    seat.status = SeatStatus.BOOKED
                    selectedSeats.remove(seat) // Nếu mình đang chọn thì bỏ chọn
                    isChanged = true
                }
            }
            // 2. Nếu chưa bán, kiểm tra xem có ĐANG GIỮ không (Màu Vàng)
            else if (listLockedSeats.contains(id)) {
                if (seat.status != SeatStatus.HELD) {
                    seat.status = SeatStatus.HELD
                    selectedSeats.remove(seat) // Nếu mình đang chọn thì bỏ chọn
                    isChanged = true
                }
            }
            // 3. Nếu không bán, không giữ -> Trả về TRỐNG (Màu Trắng)
            else {
                // Chỉ reset nếu nó đang bị hiện sai là BOOKED hoặc HELD
                // (Giữ nguyên nếu nó đang là SELECTED do chính mình chọn)
                if (seat.status == SeatStatus.BOOKED || seat.status == SeatStatus.HELD) {
                    seat.status = SeatStatus.AVAILABLE
                    isChanged = true
                }
            }
        }

        if (isChanged) {
            adapter.notifyDataSetChanged()
            updateSeatInfoText()
        }
    }

    private fun updateSeatInfoText() {
        val count = selectedSeats.size
        val total = count * pricePerSeat
        val formatter = DecimalFormat("#,###")
        val totalString = formatter.format(total)

        if (count == 0) {
            tvSeatInfo.text = "0 ghế - 0 VND"
        } else {
            tvSeatInfo.text = "$count ghế - ${totalString} VND"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Nếu thoát màn hình (isFinishing) và đang giữ ghế
        if (isFinishing && myLockedSeatIds.isNotEmpty()) {
            val batch = db.batch()
            val locksRef = db.collection("showtimes").document(showtimeId).collection("locks")

            for (id in myLockedSeatIds) {
                batch.delete(locksRef.document(id))
            }
            batch.commit() // Gửi lệnh xóa ngầm, không cần chờ
        }
    }
}