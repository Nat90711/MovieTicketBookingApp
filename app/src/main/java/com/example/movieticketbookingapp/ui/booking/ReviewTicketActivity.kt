package com.example.movieticketbookingapp.ui.booking

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.movieticketbookingapp.ui.main.HomeActivity
import com.example.movieticketbookingapp.R
import com.example.movieticketbookingapp.model.Movie
import com.google.android.material.button.MaterialButton
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.DecimalFormat

class ReviewTicketActivity : AppCompatActivity() {

    // Khai báo Firebase
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var expireTimeSeconds: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_booking_review)

        // Khởi tạo Firebase
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // 1. Nhận dữ liệu từ Intent
        val movie = intent.getParcelableExtra<Movie>("movie_data")
        val cinema = intent.getStringExtra("cinema_name") ?: "Unknown Cinema"
        val roomName = intent.getStringExtra("room_name") ?: ""
        val date = intent.getStringExtra("selected_date") ?: "01"
        val time = intent.getStringExtra("selected_time") ?: "00:00"
        val duration = intent.getStringExtra("duration") ?: "N/A"
        val seatType = intent.getStringExtra("seat_type") ?: "Standard"
        val totalPrice = intent.getDoubleExtra("total_price", 0.0)
        expireTimeSeconds = intent.getLongExtra("expire_time_seconds", 0)
        val foods = intent.getStringArrayListExtra("selected_foods") ?: arrayListOf()
        val foodPrice = intent.getDoubleExtra("food_price", 0.0)

        // Nhận ID suất chiếu và Danh sách ID ghế (để lưu DB)
        val showtimeId = intent.getStringExtra("showtime_id") ?: ""
        val seatIds = intent.getStringArrayListExtra("seat_ids") ?: arrayListOf()
        val seatNames = intent.getStringArrayListExtra("seat_names") ?: arrayListOf()

        // 2. Ánh xạ View
        val imgPoster: ImageView = findViewById(R.id.imgPoster)
        val tvMovieTitle: TextView = findViewById(R.id.tvMovieTitle)
        val tvCinema: TextView = findViewById(R.id.tvCinema)
        val tvDateTime: TextView = findViewById(R.id.tvDateTime)

        val tvTicketCount: TextView = findViewById(R.id.tvTicketCount)
        val tvTicketType: TextView = findViewById(R.id.tvTicketType)
        val tvPrice: TextView = findViewById(R.id.tvPrice)
        val tvFoodInfo: TextView = findViewById(R.id.tvFoodInfo)
        val tvSeatNo: TextView = findViewById(R.id.tvSeatNo)
        val tvTotalPrice: TextView = findViewById(R.id.tvTotalPrice)
        val btnPay: MaterialButton = findViewById(R.id.btnPay)


        // 3. Hiển thị dữ liệu lên màn hình
        if (movie != null) {
            tvMovieTitle.text = movie.title
            Glide.with(this).load(movie.posterUrl).centerCrop().into(imgPoster)
        }

        if (foods.isNotEmpty()) {
            tvFoodInfo.text = foods.joinToString(", ")
            tvFoodInfo.visibility = View.VISIBLE
        } else {
            tvFoodInfo.text = "Không chọn đồ ăn"
        }

        tvCinema.text = cinema
        tvDateTime.text = "$time, $date"

        // Hiển thị chi tiết vé
        tvTicketCount.text = seatNames.size.toString()
        tvSeatNo.text = seatNames.joinToString(", ") // Ví dụ: "A1, A2"

        // Format tiền tệ
        val formatter = DecimalFormat("#,### VND")
        val priceString = formatter.format(totalPrice)

        tvTicketType.text = seatType

        tvPrice.text = priceString
        tvTotalPrice.text = priceString

        // 4. Xử lý nút Back
        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        btnPay.setOnClickListener {
            val intent = Intent(this, PaymentActivity::class.java)

            // Truyền TẤT CẢ dữ liệu cần thiết sang màn hình thanh toán
            intent.putExtra("total_price", totalPrice)
            intent.putExtra("showtime_id", showtimeId)
            intent.putStringArrayListExtra("seat_ids", seatIds)     // ID ghế (để lưu DB)
            intent.putStringArrayListExtra("seat_names", seatNames) // Tên ghế (để hiển thị)

            intent.putExtra("movie_title", movie?.title)
            intent.putExtra("cinema_name", cinema)
            intent.putExtra("date_time", "$time, $date")
            intent.putExtra("poster_url", movie?.posterUrl)
            intent.putExtra("expire_time_seconds", expireTimeSeconds)
            intent.putExtra("duration", duration)
            intent.putExtra("seat_type", seatType)
            intent.putExtra("room_name", roomName)
            intent.putStringArrayListExtra("selected_foods", foods)

            startActivity(intent)
        }
    }

    private fun saveBookingHistory(movieTitle: String, cinema: String, dateTime: String, seats: ArrayList<String>, price: Double) {
        val userId = auth.currentUser?.uid ?: return

        val bookingData = hashMapOf(
            "userId" to userId,
            "movieTitle" to movieTitle,
            "cinema" to cinema,
            "dateTime" to dateTime,
            "seats" to seats, // Lưu tên ghế (A1, A2) để user dễ đọc
            "totalPrice" to price,
            "bookingTime" to Timestamp.now()
        )

        db.collection("bookings")
            .add(bookingData)
            .addOnSuccessListener {
                Toast.makeText(this, "Thanh toán thành công! Vé đã được lưu.", Toast.LENGTH_LONG).show()

                // Quay về trang chủ (Xóa hết stack cũ để không Back lại được màn chọn ghế)
                val intent = Intent(this, HomeActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Lỗi lưu lịch sử: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}