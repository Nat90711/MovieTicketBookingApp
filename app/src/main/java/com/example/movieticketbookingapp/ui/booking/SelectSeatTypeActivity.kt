package com.example.movieticketbookingapp.ui.booking

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.movieticketbookingapp.R
import com.example.movieticketbookingapp.model.Movie
import com.google.android.material.button.MaterialButton
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class SelectTicketTypeActivity : AppCompatActivity() {

    // Data nhận từ màn hình trước
    private var movie: Movie? = null
    private var showtimeId: String = ""
    private var selectedDate: String = "" // Định dạng: "dd/MM/yyyy"
    private var selectedTime: String = "" // Định dạng: "HH:mm"
    private var cinemaName: String = ""

    // Biến đếm số lượng vé đang chọn
    private var adultCount = 0
    private var studentCount = 0
    private var coupleCount = 0

    // --- CẤU HÌNH GIÁ VÉ ---
    private var PRICE_ADULT = 68000.0   // Giá cố định
    private var PRICE_STUDENT = 49000.0 // Giá mặc định (suất sau 22h)
    private var PRICE_COUPLE = 0.0      // = Adult * 2

    // Views
    private lateinit var layoutAdult: View
    private lateinit var layoutStudent: View
    private lateinit var layoutCouple: View
    private lateinit var tvTotalPrice: TextView
    private lateinit var btnContinue: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_ticket_type)

        getIntentData()
        initViews()

        // 1. TÍNH TOÁN GIÁ DỰA TRÊN NGÀY & GIỜ
        calculateDynamicPrices()

        // 2. SETUP GIAO DIỆN HIỂN THỊ
        setupTicketOptions()

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        btnContinue.setOnClickListener {
            handleContinue()
        }
    }

    private fun getIntentData() {
        movie = intent.getParcelableExtra("movie_data")
        showtimeId = intent.getStringExtra("showtime_id") ?: ""
        selectedDate = intent.getStringExtra("selected_date") ?: ""
        selectedTime = intent.getStringExtra("selected_time") ?: ""
        cinemaName = intent.getStringExtra("cinema_name") ?: ""
    }

    private fun initViews() {
        layoutAdult = findViewById(R.id.layoutTicketAdult)
        layoutStudent = findViewById(R.id.layoutTicketStudent)
        layoutCouple = findViewById(R.id.layoutTicketCouple)
        tvTotalPrice = findViewById(R.id.tvTotalPrice)
        btnContinue = findViewById(R.id.btnContinue)
    }

    // --- LOGIC TÍNH GIÁ VÉ ĐỘNG ---
    private fun calculateDynamicPrices() {
        // 1. Giá Người Lớn & Vé Đôi (Cố định)
        PRICE_ADULT = 68000.0
        PRICE_COUPLE = PRICE_ADULT * 2 // 136.000

        // 2. Kiểm tra điều kiện giảm giá cho HSSV
        val isMonday = checkIsMonday(selectedDate)
        val isBefore22h = checkIsBefore22h(selectedTime)

        // LOGIC:
        // - Nếu là Thứ 2 -> 45k
        // - HOẶC Nếu suất chiếu trước 22:00 -> 45k
        // - Còn lại (Suất đêm sau 22h các ngày thường) -> 49k
        if (isMonday || isBefore22h) {
            PRICE_STUDENT = 45000.0
        } else {
            PRICE_STUDENT = 49000.0
        }
    }

    // Kiểm tra có phải Thứ 2 không
    private fun checkIsMonday(dateStr: String): Boolean {
        if (dateStr.isEmpty()) return false
        try {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val date = sdf.parse(dateStr)
            val calendar = Calendar.getInstance()
            if (date != null) {
                calendar.time = date
                return calendar.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    // Kiểm tra có phải trước 22:00 (10h tối) không
    private fun checkIsBefore22h(timeStr: String): Boolean {
        if (timeStr.isEmpty()) return false
        try {
            // timeStr có dạng "HH:mm" (ví dụ "09:30", "20:00", "23:15")
            val parts = timeStr.split(":")
            if (parts.isNotEmpty()) {
                val hour = parts[0].toInt()
                // Nhỏ hơn 22 tức là từ 00:00 đến 21:59 -> Trả về true
                return hour < 22
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    private fun setupTicketOptions() {
        val formatter = DecimalFormat("#,###")

        // 1. Vé Người Lớn
        setupSingleOption(layoutAdult, "NGƯỜI LỚN", "ĐƠN", "${formatter.format(PRICE_ADULT)} đ") { delta ->
            if (canChangeQuantity(isCouple = false)) {
                val newCount = adultCount + delta
                if (newCount >= 0) {
                    adultCount = newCount
                    updateUI()
                }
            }
        }

        // 2. Vé HSSV (Giá đã tính toán ở trên)
        setupSingleOption(layoutStudent, "HSSV - U22", "ĐƠN", "${formatter.format(PRICE_STUDENT)} đ") { delta ->
            if (canChangeQuantity(isCouple = false)) {
                val newCount = studentCount + delta
                if (newCount >= 0) {
                    // Cảnh báo nếu bắt đầu chọn vé HSSV
                    if (studentCount == 0 && delta > 0) {
                        showStudentWarning {
                            studentCount = newCount
                            updateUI()
                        }
                    } else {
                        studentCount = newCount
                        updateUI()
                    }
                }
            }
        }

        // 3. Vé Đôi
        setupSingleOption(layoutCouple, "VÉ ĐÔI (2 Ghế)", "ĐÔI", "${formatter.format(PRICE_COUPLE)} đ") { delta ->
            if (canChangeQuantity(isCouple = true)) {
                val newCount = coupleCount + delta
                if (newCount >= 0) {
                    coupleCount = newCount
                    updateUI()
                }
            }
        }
    }

    private fun setupSingleOption(view: View, name: String, desc: String, price: String, onAction: (Int) -> Unit) {
        view.findViewById<TextView>(R.id.tvTicketName).text = name
        view.findViewById<TextView>(R.id.tvTicketDescription).text = desc
        view.findViewById<TextView>(R.id.tvTicketPrice).text = price
        view.findViewById<ImageView>(R.id.btnPlus).setOnClickListener { onAction(1) }
        view.findViewById<ImageView>(R.id.btnMinus).setOnClickListener { onAction(-1) }
    }

    private fun canChangeQuantity(isCouple: Boolean): Boolean {
        if (isCouple) {
            if (adultCount > 0 || studentCount > 0) {
                Toast.makeText(this, "Bạn chỉ được chọn một loại vé: Đơn hoặc Đôi", Toast.LENGTH_SHORT).show()
                return false
            }
        } else {
            if (coupleCount > 0) {
                Toast.makeText(this, "Bạn chỉ được chọn một loại vé: Đơn hoặc Đôi", Toast.LENGTH_SHORT).show()
                return false
            }
        }
        return true
    }

    private fun showStudentWarning(onConfirm: () -> Unit) {
        // Hiển thị thông báo chính sách giá rõ ràng cho user
        val message = "Vui lòng xuất trình thẻ HSSV/CCCD dưới 22 tuổi tại quầy.\n\n" +
                "Nếu bạn không xuất trình được giấy tờ hợp lệ, rạp có quyền TỪ CHỐI cho vào phòng chiếu và KHÔNG HOÀN TIỀN."

        AlertDialog.Builder(this)
            .setTitle("Lưu ý vé HSSV")
            .setMessage(message)
            .setPositiveButton("Đồng ý") { _, _ -> onConfirm() }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun updateUI() {
        layoutAdult.findViewById<TextView>(R.id.tvQuantity).text = adultCount.toString()
        layoutStudent.findViewById<TextView>(R.id.tvQuantity).text = studentCount.toString()
        layoutCouple.findViewById<TextView>(R.id.tvQuantity).text = coupleCount.toString()

        val total = (adultCount * PRICE_ADULT) + (studentCount * PRICE_STUDENT) + (coupleCount * PRICE_COUPLE)
        val formatter = DecimalFormat("#,###")
        tvTotalPrice.text = "${formatter.format(total)} VNĐ"

        updateButtonStates()
    }

    private fun updateButtonStates() {
        val hasSingle = (adultCount > 0 || studentCount > 0)
        val hasCouple = (coupleCount > 0)
        layoutCouple.alpha = if (hasSingle) 0.5f else 1.0f
        layoutAdult.alpha = if (hasCouple) 0.5f else 1.0f
        layoutStudent.alpha = if (hasCouple) 0.5f else 1.0f
    }

    private fun handleContinue() {
        val totalTickets = adultCount + studentCount + coupleCount
        if (totalTickets == 0) {
            Toast.makeText(this, "Vui lòng chọn vé!", Toast.LENGTH_SHORT).show()
            return
        }

        val totalPrice = (adultCount * PRICE_ADULT) + (studentCount * PRICE_STUDENT) + (coupleCount * PRICE_COUPLE)
        val isCoupleMode = (coupleCount > 0)

        val intent = Intent(this, SeatSelectionActivity::class.java)

        // Truyền dữ liệu cũ
        intent.putExtra("movie_data", movie)
        intent.putExtra("showtime_id", showtimeId)
        intent.putExtra("selected_date", selectedDate)
        intent.putExtra("selected_time", selectedTime)
        intent.putExtra("cinema_name", cinemaName)

        // Truyền dữ liệu mới
        intent.putExtra("is_couple_mode", isCoupleMode)
        val limit = if (isCoupleMode) coupleCount else (adultCount + studentCount)
        intent.putExtra("total_quantity", limit)
        intent.putExtra("total_price", totalPrice)

        // Tạo chuỗi mô tả
        val details = ArrayList<String>()
        if (adultCount > 0) details.add("$adultCount Người lớn")
        if (studentCount > 0) details.add("$studentCount HSSV")
        if (coupleCount > 0) details.add("$coupleCount Vé đôi")
        intent.putExtra("ticket_type_details", details.joinToString(", "))

        startActivity(intent)
    }
}