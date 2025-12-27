package com.example.movieticketbookingapp.ui.admin

import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.movieticketbookingapp.R
import com.example.movieticketbookingapp.model.Movie
import com.google.firebase.firestore.FirebaseFirestore

class EditMovieActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private var currentMovieId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_movie_edit)

        db = FirebaseFirestore.getInstance()

        val etTitle = findViewById<EditText>(R.id.etTitle)
        val etDuration = findViewById<EditText>(R.id.etDuration)
        val etID = findViewById<EditText>(R.id.etID)
        val etUrl = findViewById<EditText>(R.id.etPosterUrl)
        val etDesc = findViewById<EditText>(R.id.etDesc)
        val imgPreview = findViewById<ImageView>(R.id.imgPreview)
        val btnSave = findViewById<Button>(R.id.btnSave)
        val etDirector = findViewById<EditText>(R.id.etDirector)
        val etLanguage = findViewById<EditText>(R.id.etLanguage)
        val radioGroupStatus = findViewById<RadioGroup>(R.id.radioGroupStatus)
        val rbNowShowing = findViewById<RadioButton>(R.id.rbNowShowing)
        val rbComingSoon = findViewById<RadioButton>(R.id.rbComingSoon)
        val cbIsHot = findViewById<CheckBox>(R.id.cbIsHot)
        val etCast = findViewById<EditText>(R.id.etCast)

        // Thay Spinner bằng EditText
        val etGenre = findViewById<EditText>(R.id.etGenre)

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        // (Xóa đoạn setup Spinner cũ)

        val movie = intent.getParcelableExtra<Movie>("movie_data")

        if (movie != null) {
            currentMovieId = movie.id
            etID.setText(movie.id.toString())
            etTitle.setText(movie.title)
            etUrl.setText(movie.posterUrl)
            etDesc.setText(movie.description)
            etDirector.setText(movie.director)
            etLanguage.setText(movie.language)
            etCast.setText(movie.cast.joinToString(", "))


            if (movie.status == "coming_soon") {
                rbComingSoon.isChecked = true
            } else {
                rbNowShowing.isChecked = true
            }

            // Set Hot
            cbIsHot.isChecked = movie.hot

            val durationNumber = movie.duration.toString()
            etDuration.setText(durationNumber)

            Glide.with(this).load(movie.posterUrl).centerCrop().into(imgPreview)

            // Điền thể loại cũ vào ô nhập liệu
            etGenre.setText(movie.genre)
        }

        btnSave.setOnClickListener {
            val title = etTitle.text.toString().trim()
            val durationInput = etDuration.text.toString().trim()
            val formattedDuration = if (durationInput.isEmpty()) 0 else durationInput.toInt()
            val url = etUrl.text.toString().trim()
            val desc = etDesc.text.toString().trim()
            val director = etDirector.text.toString().trim()
            val language = etLanguage.text.toString().trim()
            val status = if (radioGroupStatus.checkedRadioButtonId == R.id.rbComingSoon) "coming_soon" else "now_showing"
            val isHot = cbIsHot.isChecked

            // Lấy dữ liệu nhập tay
            val genreInput = etGenre.text.toString().trim()

            val castInput = etCast.text.toString().trim()
            val castList = if (castInput.isEmpty()) emptyList() else castInput.split(",").map { it.trim() }

            if (title.isEmpty() || durationInput.isEmpty() || genreInput.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đủ thông tin!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val updatedMovie = Movie(
                id = currentMovieId,
                title = title,
                posterUrl = url,
                duration = formattedDuration,
                genre = genreInput, // Lưu text mới
                description = desc,
                director = director,
                language = language,
                status = status,
                hot = isHot,
                cast = castList
            )

            db.collection("movies").document(currentMovieId.toString())
                .set(updatedMovie)
                .addOnSuccessListener {
                    Toast.makeText(this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Lỗi cập nhật: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}