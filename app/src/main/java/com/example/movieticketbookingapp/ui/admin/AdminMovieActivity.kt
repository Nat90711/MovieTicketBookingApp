package com.example.movieticketbookingapp.ui.admin

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.movieticketbookingapp.R
import com.example.movieticketbookingapp.adapter.AdminMovieAdapter
import com.example.movieticketbookingapp.model.Movie
import com.example.movieticketbookingapp.ui.auth.LoginActivity
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AdminMovieActivity : AppCompatActivity() {

    private lateinit var rvMovies: RecyclerView
    private lateinit var etSearch: EditText
    private lateinit var btnAdd: MaterialButton
    private lateinit var adapter: AdminMovieAdapter

    // Firebase
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private var movieList = ArrayList<Movie>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_movie_list)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        initViews()
        setupRecyclerView()
        setupSearch()

        listenToMoviesRealtime()

        findViewById<android.view.View>(R.id.btnBack).setOnClickListener {
            finish()
        }

        btnAdd.setOnClickListener {
            startActivity(Intent(this, AddMovieActivity::class.java))
        }
    }

    private fun initViews() {
        rvMovies = findViewById(R.id.rvAdminMovies)
        etSearch = findViewById(R.id.etSearch)
        btnAdd = findViewById(R.id.btnAddMovie)
    }

    private fun setupRecyclerView() {
        adapter = AdminMovieAdapter(
            movieList,
            onEditClick = { movie ->
                val intent = Intent(this, EditMovieActivity::class.java)
                intent.putExtra("movie_data", movie)
                startActivity(intent)
            },
            onDeleteClick = { movie ->
                showDeleteDialog(movie)
            }
        )
        rvMovies.adapter = adapter
        rvMovies.layoutManager = LinearLayoutManager(this)
    }

    private fun listenToMoviesRealtime() {
        db.collection("movies")
            .addSnapshotListener { snapshots, e ->
                if (e != null) return@addSnapshotListener
                if (snapshots != null) {
                    movieList.clear()
                    for (document in snapshots) {
                        val movie = document.toObject(Movie::class.java)
                        movieList.add(movie)
                    }
                    movieList.sortByDescending { it.id }
                    adapter.updateData(movieList)
                }
            }
    }

    private fun setupSearch() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                adapter.filter(s.toString())
            }
        })
    }

    private fun showDeleteDialog(movie: Movie) {
        // Kiểm tra xem phim này có đang được xếp lịch chiếu không
        db.collection("showtimes")
            .whereEqualTo("movieId", movie.id)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    // --- RÀNG BUỘC: CHẶN XÓA NẾU CÒN LỊCH CHIẾU ---
                    showErrorDialog(
                        "Không thể xóa phim!",
                        "Phim '${movie.title}' đang có ${documents.size()} suất chiếu trong hệ thống.\n\nVui lòng xóa hết các suất chiếu của phim này trước khi xóa phim."
                    )
                } else {
                    // --- KHÔNG CÓ RÀNG BUỘC -> HIỆN HỘP THOẠI XÁC NHẬN ---
                    performDeleteMovie(movie)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Lỗi kiểm tra dữ liệu: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Hàm xóa thật
    private fun performDeleteMovie(movie: Movie) {
        AlertDialog.Builder(this)
            .setTitle("Xóa Phim")
            .setMessage("Bạn chắc chắn muốn xóa phim '${movie.title}'?\nHành động này không thể hoàn tác.")
            .setPositiveButton("Xóa") { _, _ ->
                db.collection("movies").document(movie.id.toString())
                    .delete()
                    .addOnSuccessListener {
                        Toast.makeText(this, "Đã xóa thành công!", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Lỗi khi xóa: ${it.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    // Hàm hiển thị thông báo
    private fun showErrorDialog(title: String, message: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setIcon(R.drawable.ic_warning)
            .setPositiveButton("Đã hiểu", null)
            .show()
    }
}