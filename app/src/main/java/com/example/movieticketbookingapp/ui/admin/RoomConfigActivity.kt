package com.example.movieticketbookingapp.ui.admin

import android.graphics.Color
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.movieticketbookingapp.R
import com.example.movieticketbookingapp.adapter.AdminSeatAdapter
import com.example.movieticketbookingapp.adapter.SEAT_TYPE_AISLE
import com.example.movieticketbookingapp.adapter.SEAT_TYPE_DOUBLE
import com.example.movieticketbookingapp.adapter.SEAT_TYPE_STANDARD
import com.example.movieticketbookingapp.adapter.SEAT_TYPE_VIP
import com.example.movieticketbookingapp.model.CinemaRoom
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FirebaseFirestore

class RoomConfigActivity : AppCompatActivity() {

    // Views
    private lateinit var rvSeatConfig: RecyclerView
    private lateinit var btnSaveRoom: MaterialButton

    // Buttons chọn chế độ
    private lateinit var btnModeStandard: MaterialButton
    private lateinit var btnModeVIP: MaterialButton
    private lateinit var btnModeAisle: MaterialButton
    private lateinit var btnModeDouble: MaterialButton
    private var isEditMode = false

    // Data nhận từ màn hình trước
    private var roomName = ""
    private var roomType = ""
    private var roomId = "" // ID này có thể do Admin nhập hoặc tự gen
    private var rows = 0
    private var cols = 0

    // Logic Adapter & Data ghế
    private lateinit var adapter: AdminSeatAdapter
    private val seatConfigList = ArrayList<Int>() // List lưu trạng thái ghế
    private var currentMode = SEAT_TYPE_STANDARD // Mặc định là chế độ ghế thường

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_room_config)

        // 1. Nhận dữ liệu intent
        getDataFromIntent()

        // 2. Ánh xạ View
        initViews()

        // 3. Khởi tạo dữ liệu ghế ban đầu
        initSeatData()

        // 4. Cài đặt RecyclerView Grid
        setupRecyclerView()

        // 5. Cài đặt sự kiện click
        setupEvents()

        // Cập nhật giao diện nút bấm mặc định
        updateButtonUI(currentMode)
    }

    private fun getDataFromIntent() {
        roomId = intent.getStringExtra("room_id") ?: ""
        roomName = intent.getStringExtra("room_name") ?: ""
        roomType = intent.getStringExtra("room_type") ?: ""
        rows = intent.getIntExtra("rows", 0)
        cols = intent.getIntExtra("cols", 0)
        isEditMode = intent.getBooleanExtra("is_edit_mode", false)
    }

    private fun initViews() {
        rvSeatConfig = findViewById(R.id.rvSeatConfig)
        btnSaveRoom = findViewById(R.id.btnSaveRoom)

        btnModeStandard = findViewById(R.id.btnModeStandard)
        btnModeVIP = findViewById(R.id.btnModeVIP)
        btnModeAisle = findViewById(R.id.btnModeAisle)
        btnModeDouble = findViewById(R.id.btnModeDouble)

        // Nút Back trên header
        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }
    }

    private fun initSeatData() {
        seatConfigList.clear()
        val existingConfig = intent.getIntegerArrayListExtra("seat_config")

        if (existingConfig != null && existingConfig.isNotEmpty()) {
            // --- TRƯỜNG HỢP SỬA (EDIT MODE) ---
            // Kiểm tra xem kích thước có khớp không (đề phòng lỗi data cũ)
            if (existingConfig.size == rows * cols) {
                seatConfigList.addAll(existingConfig)
                // Cập nhật giao diện ngay
                return
            }
        }

        // --- TRƯỜNG HỢP TẠO MỚI (CREATE MODE) hoặc lỗi data ---
        val totalSeats = rows * cols
        for (i in 0 until totalSeats) {
            seatConfigList.add(SEAT_TYPE_STANDARD)
        }
    }

    private fun setupRecyclerView() {
        // QUAN TRỌNG: set SpanCount bằng đúng số cột (cols) admin nhập
        val layoutManager = GridLayoutManager(this, cols)
        rvSeatConfig.layoutManager = layoutManager

        adapter = AdminSeatAdapter(seatConfigList, cols) { position ->
            // Khi click vào ghế -> Đổi trạng thái theo currentMode
            handleSeatClick(position)
        }
        rvSeatConfig.adapter = adapter
    }

    private fun handleSeatClick(position: Int) {
        val oldStatus = seatConfigList[position]

        // --- TRƯỜNG HỢP A: TẠO GHẾ ĐÔI MỚI ---
        if (currentMode == SEAT_TYPE_DOUBLE) {
            // 1. Kiểm tra biên: Không thể tạo ở cột cuối
            if ((position + 1) % cols == 0) {
                Toast.makeText(this, "Không đủ chỗ tạo ghế đôi ở cuối hàng!", Toast.LENGTH_SHORT).show()
                return
            }

            // Chúng ta chuẩn bị tạo cặp đôi tại [position] và [position + 1]
            // Cần dọn dẹp các mối quan hệ cũ của 2 vị trí này (nếu có)

            // CHECK VỊ TRÍ BÊN TRÁI (position):
            // Nếu vị trí này đang là ghế đôi, và nó là "đuôi" (Right) của cặp [position-1, position]
            // -> Thì ghế [position-1] sẽ bị lẻ loi -> Reset nó về Thường
            if (seatConfigList[position] == SEAT_TYPE_DOUBLE && !isLeftSeatOfCouple(position)) {
                if (position - 1 >= 0) {
                    seatConfigList[position - 1] = SEAT_TYPE_STANDARD
                    adapter.notifyItemChanged(position - 1)
                }
            }

            // CHECK VỊ TRÍ BÊN PHẢI (position + 1):
            // Nếu vị trí này đang là ghế đôi, và nó là "đầu" (Left) của cặp [position+1, position+2]
            // -> Thì ghế [position+2] sẽ bị lẻ loi -> Reset nó về Thường
            // Lưu ý: Phải check bounds
            if (position + 1 < seatConfigList.size && seatConfigList[position + 1] == SEAT_TYPE_DOUBLE) {
                // Cẩn thận: isLeftSeatOfCouple tính toán dựa trên dữ liệu hiện tại.
                // Tại thời điểm này dữ liệu chưa đổi nên check vẫn chính xác.
                if (isLeftSeatOfCouple(position + 1)) {
                    if (position + 2 < seatConfigList.size) {
                        seatConfigList[position + 2] = SEAT_TYPE_STANDARD
                        adapter.notifyItemChanged(position + 2)
                    }
                }
            }

            // 2. Sau khi dọn dẹp, thiết lập cặp đôi mới
            if (position + 1 < seatConfigList.size) {
                seatConfigList[position] = SEAT_TYPE_DOUBLE
                seatConfigList[position + 1] = SEAT_TYPE_DOUBLE

                adapter.notifyItemChanged(position)
                adapter.notifyItemChanged(position + 1)
            }
        }

        // --- TRƯỜNG HỢP B: XÓA/SỬA (Logic giữ nguyên như lần trước) ---
        else {
            if (oldStatus != SEAT_TYPE_DOUBLE) {
                seatConfigList[position] = currentMode
                adapter.notifyItemChanged(position)
            } else {
                // Xóa cả cặp
                if (isLeftSeatOfCouple(position)) {
                    // Xóa mình + Phải
                    seatConfigList[position] = currentMode
                    adapter.notifyItemChanged(position)
                    if (position + 1 < seatConfigList.size && seatConfigList[position + 1] == SEAT_TYPE_DOUBLE) {
                        seatConfigList[position + 1] = currentMode
                        adapter.notifyItemChanged(position + 1)
                    }
                } else {
                    // Xóa mình + Trái
                    seatConfigList[position] = currentMode
                    adapter.notifyItemChanged(position)
                    if (position - 1 >= 0 && seatConfigList[position - 1] == SEAT_TYPE_DOUBLE) {
                        seatConfigList[position - 1] = currentMode
                        adapter.notifyItemChanged(position - 1)
                    }
                }
            }
        }
    }

    private fun isLeftSeatOfCouple(pos: Int): Boolean {
        val rowStart = (pos / cols) * cols
        var consecutiveDoubles = 0
        var i = pos - 1
        while (i >= rowStart && seatConfigList[i] == SEAT_TYPE_DOUBLE) {
            consecutiveDoubles++
            i--
        }
        // Chẵn ghế đôi phía trước -> Mình là đầu cặp (Trái)
        return (consecutiveDoubles % 2 == 0)
    }

    private fun setupEvents() {
        // Chọn chế độ Ghế Thường
        btnModeStandard.setOnClickListener {
            currentMode = SEAT_TYPE_STANDARD
            updateButtonUI(SEAT_TYPE_STANDARD)
        }

        // Chọn chế độ Ghế VIP
        btnModeVIP.setOnClickListener {
            currentMode = SEAT_TYPE_VIP
            updateButtonUI(SEAT_TYPE_VIP)
        }

        btnModeDouble.setOnClickListener {
            currentMode = SEAT_TYPE_DOUBLE
            updateButtonUI(SEAT_TYPE_DOUBLE)
        }

        // Chọn chế độ Lối đi
        btnModeAisle.setOnClickListener {
            currentMode = SEAT_TYPE_AISLE
            updateButtonUI(SEAT_TYPE_AISLE)
        }

        // Lưu phòng lên Firebase
        btnSaveRoom.setOnClickListener {
            saveRoomToFirebase()
        }


    }

    // Hàm đổi màu nút bấm để biết đang chọn chế độ nào
    private fun updateButtonUI(mode: Int) {
        // Reset stroke (viền)
        btnModeStandard.strokeWidth = 0
        btnModeVIP.strokeWidth = 0
        btnModeAisle.strokeWidth = 0
        btnModeDouble.strokeWidth = 0

        // Highlight nút đang chọn bằng viền dày
        val activeStrokeWidth = 4 // dp (hoặc pixel tùy chỉnh)
        val activeColor = Color.WHITE

        when (mode) {
            SEAT_TYPE_STANDARD -> btnModeStandard.strokeWidth = activeStrokeWidth
            SEAT_TYPE_VIP -> btnModeVIP.strokeWidth = activeStrokeWidth
            SEAT_TYPE_AISLE -> btnModeAisle.strokeWidth = activeStrokeWidth
            SEAT_TYPE_DOUBLE -> btnModeDouble.strokeWidth = activeStrokeWidth
        }
    }

    private fun saveRoomToFirebase() {
        // Validation cơ bản (Giữ nguyên)
        if (roomId.isBlank() || roomName.isBlank() || rows <= 0 || cols <= 0) {
            Toast.makeText(this, "Vui lòng kiểm tra lại thông tin!", Toast.LENGTH_SHORT).show()
            return
        }

        val db = FirebaseFirestore.getInstance()
        val docRef = db.collection("rooms").document(roomId)

        // LOGIC MỚI:
        if (isEditMode) {
            // --- NẾU ĐANG SỬA ---
            // Không cần kiểm tra trùng lặp (vì chắc chắn nó tồn tại rồi).
            // Cho phép ghi đè (Update) luôn.
            performSave(db)
        } else {
            // --- NẾU ĐANG TẠO MỚI ---
            // Phải kiểm tra xem ID đã có chưa để tránh ghi đè nhầm phòng khác.
            docRef.get().addOnSuccessListener { document ->
                if (document.exists()) {
                    Toast.makeText(this, "Lỗi: ID phòng '$roomId' đã tồn tại! Vui lòng chọn ID khác.", Toast.LENGTH_LONG).show()
                } else {
                    performSave(db)
                }
            }.addOnFailureListener {
                Toast.makeText(this, "Lỗi kiểm tra: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Hàm phụ để thực hiện lưu (tách ra cho code gọn)
    private fun performSave(db: FirebaseFirestore) {
        val newRoom = CinemaRoom(
            roomId = roomId,
            roomName = roomName,
            type = roomType,
            totalRows = rows,
            totalCols = cols,
            seatConfiguration = seatConfigList
        )

        db.collection("rooms").document(roomId)
            .set(newRoom)
            .addOnSuccessListener {
                Toast.makeText(this, "Cấu hình thành công!", Toast.LENGTH_LONG).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Lỗi khi lưu: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}