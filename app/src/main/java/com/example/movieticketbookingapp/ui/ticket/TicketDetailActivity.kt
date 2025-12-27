package com.example.movieticketbookingapp.ui.ticket

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.movieticketbookingapp.R
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import java.text.DecimalFormat

class TicketDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ticket_detail)

        // 1. Nhận dữ liệu từ Intent
        val movieTitle = intent.getStringExtra("movie_title") ?: "Unknown"
        val posterUrl = intent.getStringExtra("poster_url")
        val cinema = intent.getStringExtra("cinema_name") ?: "Cinestar"
        val dateTime = intent.getStringExtra("date_time") ?: "" // Ví dụ: "15:15, 12th Nov..."
        val seats = intent.getStringArrayListExtra("seat_names") ?: arrayListOf()
        val totalPrice = intent.getDoubleExtra("total_price", 0.0)
        val orderId = intent.getStringExtra("booking_id") ?: System.currentTimeMillis().toString()
        val paymentMethod = intent.getStringExtra("payment_method") ?: "ZaloPay"

        // Tách chuỗi ngày giờ (nếu cần xử lý riêng)
        // Ở đây mình giả định chuỗi dateTime gửi sang dạng "20:20, 15th Nov"
        val timeParts = dateTime.split(", ")
        val timeShow = if (timeParts.isNotEmpty()) timeParts[0] else ""
        val dateShow = if (timeParts.size > 1) timeParts[1] else ""

        // 2. Ánh xạ View
        val imgPoster: ImageView = findViewById(R.id.imgPoster)
        val imgQRCode: ImageView = findViewById(R.id.imgQRCode)

        findViewById<TextView>(R.id.tvMovieTitle).text = movieTitle
        findViewById<TextView>(R.id.tvCinema).text = cinema
        findViewById<TextView>(R.id.tvDate).text = dateShow
        findViewById<TextView>(R.id.tvTime).text = timeShow

        findViewById<TextView>(R.id.tvTicketCount).text = seats.size.toString()
        findViewById<TextView>(R.id.tvSeatNo).text = seats.joinToString(", ")

        findViewById<TextView>(R.id.tvOrderId).text = orderId
        findViewById<TextView>(R.id.tvPaymentMethod).text = paymentMethod

        val formatter = DecimalFormat("#,### VND")
        findViewById<TextView>(R.id.tvTotalPrice).text = formatter.format(totalPrice)

        // Load Poster
        if (posterUrl != null) {
            Glide.with(this).load(posterUrl).centerCrop().into(imgPoster)
        }

        // 3. TẠO MÃ QR CODE
        try {
            val barcodeEncoder = BarcodeEncoder()

            val qrContent = "TICKET|$orderId|$movieTitle|$seats"
            val bitmap = barcodeEncoder.encodeBitmap(qrContent, BarcodeFormat.QR_CODE, 400, 400)
            imgQRCode.setImageBitmap(bitmap)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Back button
        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }
    }
}