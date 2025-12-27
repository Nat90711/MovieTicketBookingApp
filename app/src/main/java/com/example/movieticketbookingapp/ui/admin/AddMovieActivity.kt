package com.example.movieticketbookingapp.ui.admin

import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.movieticketbookingapp.R
import com.example.movieticketbookingapp.model.Movie
import com.google.firebase.firestore.FirebaseFirestore

class AddMovieActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_movie_add)

        db = FirebaseFirestore.getInstance()

        val etTitle = findViewById<EditText>(R.id.etTitle)
        val etDuration = findViewById<EditText>(R.id.etDuration)
        val etUrl = findViewById<EditText>(R.id.etPosterUrl)
        val etDesc = findViewById<EditText>(R.id.etDesc)
        val btnSave = findViewById<Button>(R.id.btnSave)
        val etDirector = findViewById<EditText>(R.id.etDirector)
        val etLanguage = findViewById<EditText>(R.id.etLanguage)
        val radioGroupStatus = findViewById<RadioGroup>(R.id.radioGroupStatus)
        val cbIsHot = findViewById<CheckBox>(R.id.cbIsHot)
        val etCast = findViewById<EditText>(R.id.etCast)

        val etGenre = findViewById<EditText>(R.id.etGenre)

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        btnSave.setOnClickListener {
            val title = etTitle.text.toString().trim()
            val durationInput = etDuration.text.toString().trim()
            val formattedDuration = if (durationInput.isEmpty()) 0 else durationInput.toInt()
            val url = etUrl.text.toString().trim()
            val desc = etDesc.text.toString().trim()
            val director = etDirector.text.toString().trim()
            val language = etLanguage.text.toString().trim()
            // Lấy Status
            val status = if (radioGroupStatus.checkedRadioButtonId == R.id.rbComingSoon) "coming_soon" else "now_showing"
            // Lấy isHot
            val isHot = cbIsHot.isChecked
            // Lấy Cast
            val castInput = etCast.text.toString().trim()
            // Tách chuỗi bằng dấu phẩy và xóa khoảng trắng thừa
            // Ví dụ: "A, B , C" -> ["A", "B", "C"]
            val castList = if (castInput.isEmpty()) emptyList() else castInput.split(",").map { it.trim() }

            // Lấy dữ liệu nhập tay từ EditText
            val genreInput = etGenre.text.toString().trim()

            if (title.isEmpty() || durationInput.isEmpty() || url.isEmpty() || genreInput.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đủ thông tin!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val newId = (System.currentTimeMillis() / 1000).toInt()

            val newMovie = Movie(
                id = newId,
                title = title,
                posterUrl = url,
                duration = formattedDuration,
                genre = genreInput, // Lưu chuỗi người dùng nhập
                description = desc.ifEmpty { "Chưa có mô tả." },
                director = director,
                cast = castList,
                language = language,
                status = status,
                hot = isHot
            )

            db.collection("movies").document(newId.toString())
                .set(newMovie)
                .addOnSuccessListener {
                    Toast.makeText(this, "Thêm phim thành công!", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}