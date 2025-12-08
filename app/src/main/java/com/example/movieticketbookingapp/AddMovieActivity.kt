package com.example.movieticketbookingapp

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class AddMovieActivity : AppCompatActivity() {

    private lateinit var spinnerGenre: Spinner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_movie)

        val etTitle = findViewById<EditText>(R.id.etTitle)
        val etDuration = findViewById<EditText>(R.id.etDuration)
        val etUrl = findViewById<EditText>(R.id.etPosterUrl)
        val etDesc = findViewById<EditText>(R.id.etDesc)
        val btnSave = findViewById<Button>(R.id.btnSave)
        spinnerGenre = findViewById(R.id.spinnerGenre)

        setupGenreSpinner()

        btnSave.setOnClickListener {
            val title = etTitle.text.toString().trim()
            val durationInput = etDuration.text.toString().trim()
            val url = etUrl.text.toString().trim()
            val desc = etDesc.text.toString().trim()
            val selectedGenre = spinnerGenre.selectedItem.toString()

            // Validate dữ liệu
            if (title.isEmpty() || durationInput.isEmpty() || url.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đủ thông tin!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Xử lý thời lượng (Ví dụ nhập "90" -> lưu thành "1h 30m" hoặc giữ nguyên "90 phút")
            // Ở đây tôi giữ nguyên định dạng user nhập + chữ "phút"
            val formattedDuration = "$durationInput phút"

            // Tạo ID mới (Lấy max ID hiện tại + 1)
            val newId = (MockData.getAllMovies().maxOfOrNull { it.id } ?: 0) + 1

            val newMovie = Movie(
                id = newId,
                title = title,
                posterUrl = url,
                rating = 0.0, // Phim mới chưa có rating
                duration = formattedDuration,
                genre = selectedGenre,
                description = desc.ifEmpty { "Chưa có mô tả." }
            )

            // Lưu vào MockData
            MockData.addMovie(newMovie)

            Toast.makeText(this, "Thêm phim thành công!", Toast.LENGTH_SHORT).show()
            finish() // Đóng màn hình
        }

        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish() // Đóng màn hình hiện tại để quay về Admin Dashboard
        }
    }

    private fun setupGenreSpinner() {
        // Tạo danh sách thể loại giả định
        val genres = listOf("Đời thường", "Hành động", "Viễn tưởng", "Hoạt hình", "Kinh dị", "Tình cảm")

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, genres)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerGenre.adapter = adapter
    }
}