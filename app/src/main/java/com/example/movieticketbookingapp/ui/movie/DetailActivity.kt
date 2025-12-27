package com.example.movieticketbookingapp.ui.movie

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.movieticketbookingapp.R
import com.example.movieticketbookingapp.model.Movie
import com.example.movieticketbookingapp.ui.booking.BookingActivity
import com.google.android.material.button.MaterialButton

class DetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_movie_detail)

        val btnBuyTicket = findViewById<MaterialButton>(R.id.btnBuyTicket)

        // 1. Nhận dữ liệu từ màn hình trước
        // (Lưu ý: "movie_data" là key chúng ta sẽ dùng bên Adapter)
        val movie = intent.getParcelableExtra<Movie>("movie_data")

        if (movie != null) {
            setupViews(movie)
            if (movie.status == "coming_soon") {
                btnBuyTicket.visibility = View.GONE
            } else {
                btnBuyTicket.visibility = View.VISIBLE
                btnBuyTicket.setOnClickListener {
                    // Code chuyển sang màn hình chọn ghế/thanh toán
                    val intent = Intent(this, BookingActivity::class.java)
                    intent.putExtra("movie_data", movie)
                    startActivity(intent)
                }
            }
        } else {
            Toast.makeText(this, "Lỗi tải dữ liệu phim!", Toast.LENGTH_SHORT).show()
            finish()
        }

        // 2. Xử lý nút Back
        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }
    }

    private fun setupViews(movie: Movie) {
        val imgPoster: ImageView = findViewById(R.id.imgPoster)
        val tvTitle: TextView = findViewById(R.id.tvMovieTitle)
        val tvDuration: TextView = findViewById(R.id.tvDuration)
        val tvGenre: TextView = findViewById(R.id.tvGenre)
        val tvDesc: TextView = findViewById(R.id.tvDescription)
        val tvDirector = findViewById<TextView>(R.id.tvDirector)
        val tvLanguage = findViewById<TextView>(R.id.tvLanguage)
        val tvCast = findViewById<TextView>(R.id.tvCast)


        tvDirector.text = movie.director
        tvLanguage.text = movie.language
        tvCast.text = movie.cast.joinToString(", ")
        tvTitle.text = movie.title
        tvDuration.text = "${movie.duration}m"
        tvGenre.text = movie.genre
        tvDesc.text = movie.description

        Glide.with(this)
            .load(movie.posterUrl)
            .centerCrop()
            .into(imgPoster)
    }
}