package com.example.movieticketbookingapp

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import java.text.DecimalFormat

class ReviewTicketActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_review_ticket)

        // 1. Nhận dữ liệu từ Intent
        val movie = intent.getParcelableExtra<Movie>("movie_data")
        val cinema = intent.getStringExtra("cinema_name") ?: "Unknown Cinema"
        val date = intent.getStringExtra("selected_date") ?: "01"
        val time = intent.getStringExtra("selected_time") ?: "00:00"
        val seats = intent.getStringArrayListExtra("selected_seats") ?: arrayListOf()
        val totalPrice = intent.getDoubleExtra("total_price", 0.0)

        // 2. Ánh xạ View
        val imgPoster: ImageView = findViewById(R.id.imgPoster)
        val tvMovieTitle: TextView = findViewById(R.id.tvMovieTitle)
        val tvCinema: TextView = findViewById(R.id.tvCinema)
        val tvDateTime: TextView = findViewById(R.id.tvDateTime)

        val tvTicketCount: TextView = findViewById(R.id.tvTicketCount)
        val tvPrice: TextView = findViewById(R.id.tvPrice)
        val tvSeatNo: TextView = findViewById(R.id.tvSeatNo)
        val tvTotalPrice: TextView = findViewById(R.id.tvTotalPrice)
        val btnPay: MaterialButton = findViewById(R.id.btnPay)

        // 3. Hiển thị dữ liệu
        if (movie != null) {
            tvMovieTitle.text = movie.title
            Glide.with(this).load(movie.posterUrl).centerCrop().into(imgPoster)
        }

        val daySuffix = when (date) {
            "1", "21", "31" -> "st"
            "2", "22" -> "nd"
            "3", "23" -> "rd"
            else -> "th"
        }

        val formattedDate = "${date}${daySuffix} November, 2025"

        tvCinema.text = cinema
        tvDateTime.text = "$time, $formattedDate"

        // Hiển thị chi tiết vé
        tvTicketCount.text = seats.size.toString()
        tvSeatNo.text = seats.joinToString(", ") // Nối danh sách ghế thành chuỗi: "5, 6, 7"

        // Format tiền tệ
        val formatter = DecimalFormat("#,### VND")
        val priceString = formatter.format(totalPrice)

        tvPrice.text = priceString
        tvTotalPrice.text = priceString

        // 4. Xử lý nút Back
        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        // 5. Xử lý nút PAY
        btnPay.setOnClickListener {
            // Giả lập thanh toán thành công
            Toast.makeText(this, "Thanh toán thành công! Chúc bạn xem phim vui vẻ 🍿", Toast.LENGTH_LONG).show()

            // Quay về trang chủ và xóa hết các màn hình đặt vé trước đó
            val intent = Intent(this, HomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }
}