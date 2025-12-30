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
import com.example.movieticketbookingapp.adapter.SeatStatus
import com.example.movieticketbookingapp.adapter.UserSeatAdapter
import com.example.movieticketbookingapp.model.Movie
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import java.text.DecimalFormat

class SeatSelectionActivity : AppCompatActivity() {

    // Views & Firebase
    private lateinit var rvSeat: RecyclerView
    private lateinit var btnConfirm: MaterialButton
    private lateinit var tvSeatInfo: TextView
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var roomName: String = ""

    // Data flow
    private var showtimeId: String = ""
    private var movie: Movie? = null

    // --- DỮ LIỆU MỚI TỪ MÀN HÌNH CHỌN LOẠI VÉ ---
    private var isCoupleMode = false      // Chế độ: True (chỉ chọn đôi), False (chỉ chọn đơn)
    private var totalTickets = 0          // Số lượng VÉ đã mua
    private var maxSeatsToSelect = 0      // Số lượng GHẾ cần chọn (Vé đôi x2)
    private var fixedTotalPrice = 0.0     // Tổng tiền đã chốt
    private var ticketTypeDetails = ""    // Chuỗi mô tả (VD: "1 Người lớn, 1 HSSV")

    // Logic ghế
    private val allSeats = ArrayList<Seat>()
    private val selectedSeats = ArrayList<Seat>()
    private var totalCols = 10

    // Logic trạng thái ghế
    private var listSoldSeats: Set<Long> = HashSet()
    private var listLockedSeats: Set<Long> = HashSet()
    private var myLockedSeatIds: ArrayList<String> = ArrayList()

    private lateinit var adapter: UserSeatAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_booking_seat_select)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        initViews()

        // 1. Nhận dữ liệu
        movie = intent.getParcelableExtra("movie_data")
        showtimeId = intent.getStringExtra("showtime_id") ?: ""

        // --- NHẬN DATA TỪ SELECT TICKET ACTIVITY ---
        isCoupleMode = intent.getBooleanExtra("is_couple_mode", false)
        totalTickets = intent.getIntExtra("total_quantity", 0)
        fixedTotalPrice = intent.getDoubleExtra("total_price", 0.0)
        ticketTypeDetails = intent.getStringExtra("ticket_type_details") ?: ""

        // Tính toán số ghế cần chọn:
        // - Vé Đôi: 1 vé = 2 ghế thực tế trên sơ đồ
        // - Vé Đơn: 1 vé = 1 ghế
        maxSeatsToSelect = if (isCoupleMode) totalTickets * 2 else totalTickets

        // 2. Load cấu hình phòng
        loadRoomConfiguration()

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        setupConfirmButton()

        // Cập nhật text hiển thị ban đầu
        updateSeatInfoText()
    }

    private fun initViews() {
        rvSeat = findViewById(R.id.rvSeat)
        btnConfirm = findViewById(R.id.btnConfirmSeat)
        tvSeatInfo = findViewById(R.id.tvSeatInfo)
    }

    private fun loadRoomConfiguration() {
        db.collection("showtimes").document(showtimeId).get()
            .addOnSuccessListener { showtimeDoc ->
                val roomId = showtimeDoc.getString("roomId") ?: ""
                roomName = showtimeDoc.getString("roomName") ?: "Unknown Room"
                // Không cần lấy giá basePrice nữa vì đã có fixedTotalPrice

                if (roomId.isNotEmpty()) {
                    fetchRoomData(roomId)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Lỗi tải suất chiếu!", Toast.LENGTH_SHORT).show()
            }
    }

    private fun fetchRoomData(roomId: String) {
        db.collection("rooms").document(roomId).get()
            .addOnSuccessListener { roomDoc ->
                if (roomDoc.exists()) {
                    totalCols = roomDoc.getLong("totalCols")?.toInt() ?: 10
                    val seatConfigList = roomDoc.get("seatConfiguration") as? List<Long> ?: emptyList()
                    val configIntList = seatConfigList.map { it.toInt() }

                    initSeatListFromConfig(configIntList, totalCols)
                }
            }
    }

    private fun initSeatListFromConfig(config: List<Int>, cols: Int) {
        allSeats.clear()
        for (i in config.indices) {
            val type = if (i < config.size) config[i] else 0
            val status = if (type == -1) SeatStatus.AISLE else SeatStatus.AVAILABLE
            // Giá của từng ghế set = 0 vì ta dùng giá tổng cố định
            allSeats.add(Seat(id = i, status = status, type = type, price = 0.0))
        }

        setupRecyclerView(cols)
        clearMyOldLocks()
        startRealtimeUpdates()
    }

    private fun setupRecyclerView(cols: Int) {
        // --- TRUYỀN THÊM isCoupleMode VÀO ADAPTER ---
        adapter = UserSeatAdapter(allSeats, cols, isCoupleMode) { clickedSeat ->
            handleSeatClick(clickedSeat)
        }
        rvSeat.adapter = adapter
        rvSeat.layoutManager = GridLayoutManager(this, cols)
    }

    private fun handleSeatClick(seat: Seat) {
        if (seat.status == SeatStatus.BOOKED || seat.status == SeatStatus.HELD) return

        val isSelecting = (seat.status == SeatStatus.AVAILABLE)

        if (isSelecting) {
            // --- KIỂM TRA SỐ LƯỢNG GHẾ GIỚI HẠN ---
            // Nếu là ghế đôi thì tính là 2 ghế, ghế đơn là 1
            val seatsToAdd = if (seat.type == 2) 2 else 1

            if (selectedSeats.size + seatsToAdd > maxSeatsToSelect) {
                Toast.makeText(this, "Bạn đã mua $totalTickets vé, chỉ được chọn $maxSeatsToSelect ghế!", Toast.LENGTH_SHORT).show()
                return
            }
        }

        if (seat.type != 2) {
            toggleSeatSelection(seat, isSelecting)
        } else {
            val partnerSeat = findCouplePartner(seat)
            toggleSeatSelection(seat, isSelecting)
            if (partnerSeat != null && partnerSeat.status != SeatStatus.BOOKED && partnerSeat.status != SeatStatus.HELD) {
                toggleSeatSelection(partnerSeat, isSelecting)
            }
        }

        updateSeatInfoText()
    }

    private fun toggleSeatSelection(seat: Seat, isSelecting: Boolean) {
        if (isSelecting) {
            seat.status = SeatStatus.SELECTED
            if (!selectedSeats.contains(seat)) selectedSeats.add(seat)
        } else {
            seat.status = SeatStatus.AVAILABLE
            selectedSeats.remove(seat)
        }
        adapter.notifyItemChanged(seat.id)
    }

    private fun findCouplePartner(seat: Seat): Seat? {
        val pos = seat.id
        val rowStart = (pos / totalCols) * totalCols
        var consecutiveDoubles = 0
        var i = pos - 1
        while (i >= rowStart && allSeats[i].type == 2) {
            consecutiveDoubles++
            i--
        }
        val isLeft = (consecutiveDoubles % 2 == 0)
        return if (isLeft) {
            if (pos + 1 < allSeats.size && allSeats[pos+1].type == 2) allSeats[pos+1] else null
        } else {
            if (pos - 1 >= 0 && allSeats[pos-1].type == 2) allSeats[pos-1] else null
        }
    }

    private fun updateSeatInfoText() {
        val count = selectedSeats.size
        // Hiển thị: Đã chọn / Tổng cần chọn
        val formatter = DecimalFormat("#,###")
        val priceString = formatter.format(fixedTotalPrice)

        tvSeatInfo.text = "$count/$maxSeatsToSelect ghế - $priceString VND"

        // Disable nút Confirm nếu chưa chọn đủ
        if (count == maxSeatsToSelect) {
            btnConfirm.isEnabled = true
            btnConfirm.alpha = 1.0f
            btnConfirm.text = "XÁC NHẬN"
        } else {
            btnConfirm.isEnabled = false
            btnConfirm.alpha = 0.5f
            btnConfirm.text = "CHỌN ĐỦ $maxSeatsToSelect GHẾ"
        }
    }

    private fun setupConfirmButton() {
        btnConfirm.setOnClickListener {
            if (selectedSeats.size != maxSeatsToSelect) {
                Toast.makeText(this, "Vui lòng chọn đủ ghế!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            performBookingTransaction()
        }
    }

    private fun performBookingTransaction() {
        val userId = auth.currentUser?.uid ?: return
        val expireTimeSeconds = Timestamp.now().seconds + 300
        val expireTimestamp = Timestamp(expireTimeSeconds, 0)

        // 1. Chuẩn bị dữ liệu ghế (Gộp tên A1-A2)
        val seatsToLock = ArrayList(selectedSeats)
        seatsToLock.sortBy { it.id }

        val seatIds = ArrayList<String>()
        val seatNames = ArrayList<String>()
        val processedDoubleSeatIds = HashSet<Int>()

        for (seat in seatsToLock) {
            seatIds.add(seat.id.toString())

            if (processedDoubleSeatIds.contains(seat.id)) continue

            val currentName = calculateSeatName(seat.id)
            // --------------------

            if (seat.type == 2) {
                var partnerSeat: Seat? = null
                if (isLeftSeatOfCouple(seat.id) && seatsToLock.any { it.id == seat.id + 1 }) {
                    partnerSeat = seatsToLock.find { it.id == seat.id + 1 }
                }

                if (partnerSeat != null) {
                    // --- SỬA CẢ ĐOẠN TÊN GHẾ ĐÔI ---
                    // Cũ: val partnerColNum = ...
                    // Mới: Tính tên cho ghế partner luôn
                    val partnerName = calculateSeatName(partnerSeat.id)

                    // Ghép lại: VD: "A1-A2"
                    seatNames.add("$currentName-$partnerName")

                    processedDoubleSeatIds.add(partnerSeat.id)
                } else {
                    seatNames.add(currentName)
                }
            } else {
                seatNames.add(currentName)
            }
        }

        val showtimeRef = db.collection("showtimes").document(showtimeId)
        val locksRef = showtimeRef.collection("locks")

        // 2. Transaction giữ ghế
        db.runTransaction { transaction ->
            // Check locked
            for (seat in seatsToLock) {
                val seatIdStr = seat.id.toString()
                val lockDoc = transaction.get(locksRef.document(seatIdStr))
                if (lockDoc.exists()) {
                    val holderId = lockDoc.getString("userId")
                    val expireAt = lockDoc.getTimestamp("expireAt")?.seconds ?: 0
                    val currentTime = Timestamp.now().seconds
                    if (expireAt > currentTime && holderId != userId) {
                        throw com.google.firebase.firestore.FirebaseFirestoreException(
                            "Ghế đã bị người khác chọn!",
                            com.google.firebase.firestore.FirebaseFirestoreException.Code.ABORTED
                        )
                    }
                }
            }
            // Clear old locks
            for (oldId in myLockedSeatIds) {
                if (!seatIds.contains(oldId)) transaction.delete(locksRef.document(oldId))
            }
            // New locks
            for (seat in seatsToLock) {
                val lockData = hashMapOf("userId" to userId, "expireAt" to expireTimestamp)
                transaction.set(locksRef.document(seat.id.toString()), lockData)
            }
            null
        }.addOnSuccessListener {
            myLockedSeatIds.clear()
            myLockedSeatIds.addAll(seatIds)

            // Lấy duration từ movie
            val durationString = movie?.duration.toString() // Nếu model Movie chưa có trường duration, bạn cần thêm vào hoặc sửa chỗ này

            // 3. Chuyển sang ReviewTicketActivity
            val intent = Intent(this, FoodSelectionActivity::class.java)

            intent.putExtra("movie_data", movie)
            intent.putExtra("showtime_id", showtimeId)
            intent.putExtra("movie_title", movie?.title)
            intent.putExtra("poster_url", movie?.posterUrl)

            // --- CẬP NHẬT: TRUYỀN DỮ LIỆU ĐÃ CÓ SẴN ---
            intent.putExtra("duration", durationString)

            val typeSet = HashSet<String>()
            for (seat in selectedSeats) {
                when (seat.type) {
                    1 -> typeSet.add("VIP")
                    2 -> typeSet.add("Couple")
                    else -> typeSet.add("Standard")
                }
            }
            // Kết quả sẽ là: "Standard", "VIP", hoặc "Standard, VIP"
            val seatTypeString = typeSet.joinToString(", ")
            intent.putExtra("seat_type", seatTypeString)

            intent.putExtra("cinema_name", getIntent().getStringExtra("cinema_name"))
            intent.putExtra("selected_date", getIntent().getStringExtra("selected_date"))
            intent.putExtra("selected_time", getIntent().getStringExtra("selected_time"))
            intent.putExtra("room_name", roomName)

            intent.putStringArrayListExtra("seat_ids", seatIds)
            intent.putStringArrayListExtra("seat_names", seatNames)

            intent.putExtra("total_price", fixedTotalPrice)
            intent.putExtra("expire_time_seconds", expireTimeSeconds)

            startActivity(intent)

        }.addOnFailureListener { e ->
            if (e is com.google.firebase.firestore.FirebaseFirestoreException &&
                e.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.ABORTED
            ) {
                Toast.makeText(this, e.message, Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "Lỗi kết nối: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun isLeftSeatOfCouple(pos: Int): Boolean {
        val rowStart = (pos / totalCols) * totalCols
        var consecutiveDoubles = 0
        var i = pos - 1
        while (i >= rowStart && allSeats[i].type == 2) {
            consecutiveDoubles++
            i--
        }
        return (consecutiveDoubles % 2 == 0)
    }

    // Các hàm realtime cũ giữ nguyên logic
    private fun startRealtimeUpdates() {
        val showtimeRef = db.collection("showtimes").document(showtimeId)
        val locksRef = showtimeRef.collection("locks")

        showtimeRef.addSnapshotListener { snapshot, e ->
            if (e != null || snapshot == null) return@addSnapshotListener
            val bookedList = snapshot.get("bookedSeats") as? List<Long> ?: emptyList()
            listSoldSeats = bookedList.toHashSet()
            refreshSeatMap()
        }

        locksRef.addSnapshotListener { snapshot, e ->
            if (e != null || snapshot == null) return@addSnapshotListener
            val tempLocked = HashSet<Long>()
            val currentTime = Timestamp.now().seconds
            val myUserId = auth.currentUser?.uid
            for (doc in snapshot.documents) {
                val expireAt = doc.getTimestamp("expireAt")?.seconds ?: 0
                val holderId = doc.getString("userId")
                val seatId = doc.id.toLongOrNull()
                if (seatId != null && expireAt > currentTime && holderId != myUserId) {
                    tempLocked.add(seatId)
                }
            }
            listLockedSeats = tempLocked
            refreshSeatMap()
        }
    }

    private fun refreshSeatMap() {
        var isChanged = false

        for (seat in allSeats) {
            // Bỏ qua lối đi
            if (seat.status == SeatStatus.AISLE) continue

            val id = seat.id.toLong()

            // 1. Ưu tiên cao nhất: Ghế đã bán (Trong danh sách Sold)
            if (listSoldSeats.contains(id)) {
                if (seat.status != SeatStatus.BOOKED) {
                    seat.status = SeatStatus.BOOKED
                    // Nếu ghế này mình lỡ chọn trước khi nó load xong -> Bỏ chọn
                    selectedSeats.remove(seat)
                    isChanged = true
                }
            }
            // 2. Ưu tiên nhì: Ghế đang bị người khác giữ (Trong danh sách Locked)
            else if (listLockedSeats.contains(id)) {
                if (seat.status != SeatStatus.HELD) {
                    seat.status = SeatStatus.HELD
                    selectedSeats.remove(seat)
                    isChanged = true
                }
            }
            // 3. Còn lại: Nếu không phải Sold cũng không phải Held
            else {
                // Chỉ reset về AVAILABLE nếu nó đang bị kẹt ở trạng thái BOOKED hoặc HELD cũ.
                // TUYỆT ĐỐI KHÔNG reset nếu nó đang là SELECTED (do chính user đang chọn).
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

    private fun clearMyOldLocks() {
        val userId = auth.currentUser?.uid ?: return
        val locksRef = db.collection("showtimes").document(showtimeId).collection("locks")
        locksRef.whereEqualTo("userId", userId).get().addOnSuccessListener { snapshot ->
            if (!snapshot.isEmpty) {
                val batch = db.batch()
                for (doc in snapshot.documents) batch.delete(doc.reference)
                batch.commit()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing && myLockedSeatIds.isNotEmpty()) {
            val batch = db.batch()
            val locksRef = db.collection("showtimes").document(showtimeId).collection("locks")
            for (id in myLockedSeatIds) {
                batch.delete(locksRef.document(id))
            }
            batch.commit()
        }
    }

    // Hàm tính tên ghế chuẩn (Bỏ qua lối đi AISLE)
    private fun calculateSeatName(seatId: Int): String {
        val rowIndex = seatId / totalCols
        val rowChar = (rowIndex + 65).toChar() // A, B, C...

        var realSeatCount = 0
        val startOfRow = rowIndex * totalCols

        // Chạy vòng lặp từ đầu hàng đến vị trí ghế hiện tại
        for (i in startOfRow..seatId) {
            // Nếu vị trí i KHÔNG PHẢI lối đi -> Thì mới đếm
            // Lưu ý: Kiểm tra trong danh sách allSeats
            if (i < allSeats.size && allSeats[i].status != SeatStatus.AISLE) {
                realSeatCount++
            }
        }
        return "$rowChar$realSeatCount"
    }
}