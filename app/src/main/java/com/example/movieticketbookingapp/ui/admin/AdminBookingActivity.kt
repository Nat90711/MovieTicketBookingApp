package com.example.movieticketbookingapp.ui.admin

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.movieticketbookingapp.R
import com.example.movieticketbookingapp.adapter.AdminBookingAdapter
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.zxing.integration.android.IntentIntegrator
import java.util.Calendar

class AdminBookingActivity : AppCompatActivity() {

    private lateinit var rvBookings: RecyclerView
    private lateinit var etSearch: EditText
    private lateinit var btnBack: ImageView
    private lateinit var cbFilterToday: CheckBox
    private lateinit var btnScanQR: CardView

    private lateinit var db: FirebaseFirestore
    private lateinit var adapter: AdminBookingAdapter
    private var fullList = ArrayList<Map<String, Any>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_booking_list)

        db = FirebaseFirestore.getInstance()
        initViews()

        setupRecyclerView()
        loadBookings() // Load dữ liệu ban đầu
        setupEvents()
    }

    private fun initViews() {
        rvBookings = findViewById(R.id.rvBookings)
        etSearch = findViewById(R.id.etSearchBooking)
        btnBack = findViewById(R.id.btnBack)
        cbFilterToday = findViewById(R.id.cbFilterToday)
        btnScanQR = findViewById(R.id.btnScanQR)

        btnBack.setOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        adapter = AdminBookingAdapter(listOf()) { booking ->
            // Khi bấm vào item -> Hiện dialog check-in
            showCheckInDialog(booking)
        }
        rvBookings.layoutManager = LinearLayoutManager(this)
        rvBookings.adapter = adapter
    }

    private fun setupEvents() {
        // 1. Sự kiện Tìm kiếm
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                applyFilters()
            }
        })

        // 2. Sự kiện Lọc Hôm Nay
        cbFilterToday.setOnCheckedChangeListener { _, _ ->
            applyFilters()
        }

        // 3. Sự kiện Quét QR
        btnScanQR.setOnClickListener {
            startQRScanner()
        }
    }

    // --- LOGIC LOAD DỮ LIỆU ---
    private fun loadBookings() {
        db.collection("bookings")
            .orderBy("bookingTime", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                fullList.clear()
                for (doc in result) {
                    val data = doc.data.toMutableMap()
                    data["id"] = doc.id // Lưu Document ID

                    // Gán mặc định status = "paid" nếu chưa có
                    if (!data.containsKey("status")) {
                        data["status"] = "paid"
                    }
                    fullList.add(data)
                }
                applyFilters() // Load xong thì filter ngay
            }
    }

    // --- LOGIC LỌC (SEARCH + DATE) ---
    private fun applyFilters() {
        val keyword = etSearch.text.toString().lowercase().trim()
        val isTodayOnly = cbFilterToday.isChecked

        // Lấy khoảng thời gian của ngày hôm nay
        val todayStart = getStartOfToday()
        val todayEnd = todayStart + 86400 // + 24h

        val filtered = fullList.filter { item ->
            val id = item["bookingId"].toString()
            val movie = item["movieTitle"].toString().lowercase()
            val customer = item["userName"]?.toString()?.lowercase() ?: ""

            // 1. Check Search Keyword (Mã vé, tên phim, tên khách)
            val matchSearch = id.contains(keyword) || movie.contains(keyword) || customer.contains(keyword)

            // 2. Check Date (Nếu có tích checkbox)
            var matchDate = true
            if (isTodayOnly) {
                val timestamp = item["bookingTime"] as? Timestamp
                if (timestamp != null) {
                    val seconds = timestamp.seconds
                    matchDate = (seconds in todayStart until todayEnd)
                }
            }

            matchSearch && matchDate
        }

        adapter.updateData(filtered)
    }

    private fun getStartOfToday(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis / 1000 // Trả về giây
    }

    // --- LOGIC QUÉT QR ---
    private fun startQRScanner() {
        val integrator = IntentIntegrator(this)
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
        integrator.setPrompt("Quét mã vé của khách hàng")
        integrator.setCameraId(0)
        integrator.setBeepEnabled(true)
        integrator.setBarcodeImageEnabled(false)
        integrator.setOrientationLocked(false)
        integrator.initiateScan()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            if (result.contents != null) {
                handleScannedContent(result.contents)
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun handleScannedContent(content: String) {
        // Format mã QR: "TICKET|orderId|movieTitle|..."
        try {
            val parts = content.split("|")
            if (parts.isNotEmpty() && parts[0] == "TICKET") {
                val bookingId = parts[1] // Lấy ID vé

                // Tìm vé trong danh sách fullList
                val foundBooking = fullList.find { it["bookingId"].toString() == bookingId }

                if (foundBooking != null) {
                    showCheckInDialog(foundBooking)
                } else {
                    Toast.makeText(this, "Không tìm thấy vé trong hệ thống!", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(this, "Mã QR không hợp lệ!", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Lỗi đọc mã: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // --- LOGIC CHECK-IN ---
    private fun showCheckInDialog(booking: Map<String, Any>) {
        val currentStatus = booking["status"]?.toString() ?: "paid"
        val docId = booking["id"].toString()

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Vé #${booking["bookingId"]}")

        var message = "Phim: ${booking["movieTitle"]}\n" +
                "Ghế: ${booking["seats"]}\n" +
                "Khách: ${booking["userName"] ?: "N/A"}\n\n" +
                "TRẠNG THÁI: "

        if (currentStatus == "checked_in") {
            message += "ĐÃ SỬ DỤNG!"
            builder.setIcon(android.R.drawable.ic_dialog_alert)
        } else if (currentStatus == "cancelled") {
            message += "ĐÃ HỦY!"
        } else {
            message += "HỢP LỆ!"
        }

        builder.setMessage(message)

        // Nếu vé chưa dùng -> Hiện nút Check-in
        if (currentStatus == "paid") {
            builder.setPositiveButton("CHECK-IN NGAY") { _, _ ->
                performCheckIn(docId, booking)
            }
        } else {
            builder.setPositiveButton("Đóng", null)
        }

        builder.setNegativeButton("Hủy", null)
        builder.show()
    }

    private fun performCheckIn(docId: String, bookingItem: Map<String, Any>) {
        db.collection("bookings").document(docId)
            .update("status", "checked_in")
            .addOnSuccessListener {
                Toast.makeText(this, "Check-in thành công!", Toast.LENGTH_SHORT).show()

                // Cập nhật list cục bộ để hiển thị ngay
                if (bookingItem is MutableMap) {
                    bookingItem["status"] = "checked_in"
                }
                applyFilters() // Refresh list
            }
            .addOnFailureListener {
                Toast.makeText(this, "Lỗi: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}