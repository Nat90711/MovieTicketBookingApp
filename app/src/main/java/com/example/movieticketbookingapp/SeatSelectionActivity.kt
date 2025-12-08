package com.example.movieticketbookingapp

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import java.text.DecimalFormat

class SeatSelectionActivity : AppCompatActivity() {

    private lateinit var rvSeat: RecyclerView
    private lateinit var btnConfirm: MaterialButton
    private lateinit var tvTitle: TextView
    private lateinit var tvSeatInfo: TextView

    // Biến lưu danh sách ghế đang chọn
    private val selectedSeats = ArrayList<Seat>()
    private val pricePerSeat = 50000.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_seat_selection)

        initViews()

        // Nhận dữ liệu phim
        val movie = intent.getParcelableExtra<Movie>("movie_data")
        val time = intent.getStringExtra("selected_time")

        if (movie != null) {
            tvTitle.text = movie.title
        }

        setupSeatGrid()

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        btnConfirm.setOnClickListener {
            if (selectedSeats.isEmpty()) {
                Toast.makeText(this, "Vui lòng chọn ít nhất 1 ghế!", Toast.LENGTH_SHORT).show()
            } else {
                val intent = Intent(this, ReviewTicketActivity::class.java)

                // Truyền lại Object Phim & Thông tin rạp
                intent.putExtra("movie_data", movie) // biến movie đã lấy ở onCreate
                intent.putExtra("cinema_name", getIntent().getStringExtra("cinema_name"))
                intent.putExtra("selected_date", getIntent().getStringExtra("selected_date"))
                intent.putExtra("selected_time", getIntent().getStringExtra("selected_time"))

                // Truyền danh sách ghế đã chọn (Chỉ lấy ID hoặc Tên ghế)
                val seatNames = ArrayList<String>()
                for (seat in selectedSeats) {
                    // Giả sử convert ID thành tên ghế (Ví dụ ID 0 -> A1).
                    // Ở đây mình để tạm ID ghế để bạn dễ test
                    seatNames.add("Ghế ${seat.id}")
                }
                intent.putStringArrayListExtra("selected_seats", seatNames)

                // Truyền tổng tiền
                val total = selectedSeats.size * 50000.0 // Giá vé 50k
                intent.putExtra("total_price", total)

                startActivity(intent)
            }
        }
    }

    private fun initViews() {
        rvSeat = findViewById(R.id.rvSeat)
        btnConfirm = findViewById(R.id.btnConfirmSeat)
        tvTitle = findViewById(R.id.tvTitle)
        tvSeatInfo = findViewById(R.id.tvSeatInfo)
    }

    private fun setupSeatGrid() {
        val seatList = ArrayList<Seat>()

        // Cấu hình lưới: 10 Hàng x 10 Cột
        val totalRows = 10
        val totalColumns = 10

        for (i in 0 until (totalRows * totalColumns)) {
            val column = i % totalColumns // Lấy chỉ số cột (từ 0 đến 9)

            // --- LOGIC XÁC ĐỊNH LOẠI GHẾ ---
            val status = when {
                // Cột thứ 3 (index 2) và Cột thứ 8 (index 7) là Lối đi
                column == 2 || column == 7 -> SeatStatus.AISLE

                // Giả lập một vài ghế đã có người đặt (Booked)
                i == 15 || i == 35 || i == 44 || i == 55 -> SeatStatus.BOOKED

                // Còn lại là ghế trống
                else -> SeatStatus.AVAILABLE
            }

            seatList.add(Seat(i, status))
        }

        // Setup Adapter
        val adapter = SeatAdapter(seatList) { clickedSeat ->
            // Logic thêm/xóa ghế khỏi danh sách đã chọn
            if (clickedSeat.status == SeatStatus.SELECTED) {
                selectedSeats.add(clickedSeat)
            } else {
                selectedSeats.remove(clickedSeat)
            }
            updateSeatInfoText()
        }

        rvSeat.adapter = adapter

        // QUAN TRỌNG: Chia thành 10 cột để khớp với logic lối đi ở trên
        rvSeat.layoutManager = GridLayoutManager(this, 10)
    }

    private fun updateSeatInfoText() {
        val count = selectedSeats.size
        val total = count * pricePerSeat

        // Format tiền cho đẹp (Ví dụ: 100.000 đ)
        val formatter = DecimalFormat("#,###")
        val totalString = formatter.format(total)

        if (count == 0) {
            tvSeatInfo.text = "0 ghế - 0 VND"
        } else {
            tvSeatInfo.text = "$count ghế - ${totalString} VND"
        }
    }
}