package com.example.movieticketbookingapp

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide

class EditMovieActivity : AppCompatActivity() {

    private lateinit var spinnerGenre: Spinner
    private var currentMovieId: Int = 0 // Lưu ID để update

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_movie)

        // 1. Ánh xạ View
        val etTitle = findViewById<EditText>(R.id.etTitle)
        val etDuration = findViewById<EditText>(R.id.etDuration)
        val etID = findViewById<EditText>(R.id.etID)
        val etUrl = findViewById<EditText>(R.id.etPosterUrl)
        val etDesc = findViewById<EditText>(R.id.etDesc)
        val imgPreview = findViewById<ImageView>(R.id.imgPreview)
        val btnSave = findViewById<Button>(R.id.btnSave)
        spinnerGenre = findViewById(R.id.spinnerGenre)

        // 2. Setup Spinner (giống bên Add)
        val genres = listOf("Đời thường", "Hành động", "Viễn tưởng", "Hoạt hình", "Kinh dị", "Tình cảm")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, genres)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerGenre.adapter = adapter

        // 3. Nhận dữ liệu phim cần sửa
        val movie = intent.getParcelableExtra<Movie>("movie_data")

        if (movie != null) {
            currentMovieId = movie.id
            etID.setText(movie.id.toString())
            etTitle.setText(movie.title)
            etUrl.setText(movie.posterUrl)
            etDesc.setText(movie.description)

            // Xử lý Duration: "109 phút" -> Lấy số "109" để hiện lên ô nhập
            val durationNumber = movie.duration.replace(Regex("[^0-9]"), "")
            etDuration.setText(durationNumber)

            // Load ảnh preview
            Glide.with(this).load(movie.posterUrl).centerCrop().into(imgPreview)

            // Chọn đúng thể loại trong Spinner
            val genreIndex = genres.indexOf(movie.genre)
            if (genreIndex != -1) {
                spinnerGenre.setSelection(genreIndex)
            }
        }

        // 4. Xử lý nút Lưu
        btnSave.setOnClickListener {
            val title = etTitle.text.toString().trim()
            val durationInput = etDuration.text.toString().trim()
            val url = etUrl.text.toString().trim()
            val desc = etDesc.text.toString().trim()
            val selectedGenre = spinnerGenre.selectedItem.toString()

            if (title.isEmpty() || durationInput.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đủ thông tin!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Tạo object mới với thông tin đã sửa (QUAN TRỌNG: Giữ nguyên ID)
            val updatedMovie = Movie(
                id = currentMovieId, // Giữ ID cũ
                title = title,
                posterUrl = url,
                rating = movie?.rating ?: 0.0,
                duration = "$durationInput phút",
                genre = selectedGenre,
                description = desc
            )

            // Gọi hàm update
            MockData.updateMovie(updatedMovie)

            Toast.makeText(this, "Đã cập nhật phim!", Toast.LENGTH_SHORT).show()
            finish()
        }

        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }
    }
}