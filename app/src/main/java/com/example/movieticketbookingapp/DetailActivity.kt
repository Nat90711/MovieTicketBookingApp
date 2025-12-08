package com.example.movieticketbookingapp

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton

class DetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        // 1. Nhận dữ liệu từ màn hình trước
        // (Lưu ý: "movie_data" là key chúng ta sẽ dùng bên Adapter)
        val movie = intent.getParcelableExtra<Movie>("movie_data")

        if (movie != null) {
            setupViews(movie)
        } else {
            Toast.makeText(this, "Lỗi tải dữ liệu phim!", Toast.LENGTH_SHORT).show()
            finish()
        }

        // 2. Xử lý nút Back
        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        // 3. Xử lý nút Mua vé
        findViewById<MaterialButton>(R.id.btnBuyTicket).setOnClickListener {
            val intent = Intent(this, BookingActivity::class.java)
            // Truyền tiếp dữ liệu phim sang để hiển thị poster
            intent.putExtra("movie_data", movie)
            startActivity(intent)
        }
    }

    private fun setupViews(movie: Movie) {
        val imgPoster: ImageView = findViewById(R.id.imgPoster)
        val tvTitle: TextView = findViewById(R.id.tvMovieTitle)
        val tvDuration: TextView = findViewById(R.id.tvDuration)
        val tvRating: TextView = findViewById(R.id.tvRating)
        val tvGenre: TextView = findViewById(R.id.tvGenre)
        val tvDesc: TextView = findViewById(R.id.tvDescription)

        tvTitle.text = movie.title
        tvDuration.text = movie.duration
        tvRating.text = "${movie.rating}/10"
        tvGenre.text = movie.genre
        tvDesc.text = movie.description

        Glide.with(this)
            .load(movie.posterUrl)
            .centerCrop()
            .into(imgPoster)
    }
}