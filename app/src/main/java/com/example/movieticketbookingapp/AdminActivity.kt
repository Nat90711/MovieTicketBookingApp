package com.example.movieticketbookingapp

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton

class AdminActivity : AppCompatActivity() {

    private lateinit var rvMovies: RecyclerView
    private lateinit var etSearch: EditText
    private lateinit var btnAdd: MaterialButton
    private lateinit var adapter: AdminMovieAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)

        initViews()
        setupRecyclerView()
        setupSearch()

        // Sự kiện thêm phim
        btnAdd.setOnClickListener {
            startActivity(Intent(this, AddMovieActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        // Cập nhật lại list khi quay lại (vì có thể vừa thêm mới)
        adapter.updateData(MockData.getAllMovies())
    }

    private fun initViews() {
        rvMovies = findViewById(R.id.rvAdminMovies)
        etSearch = findViewById(R.id.etSearch)
        btnAdd = findViewById(R.id.btnAddMovie)
    }

    private fun setupRecyclerView() {
        val movies = MockData.getAllMovies()

        adapter = AdminMovieAdapter(movies,
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
        rvMovies.layoutManager = LinearLayoutManager(this) // List dọc
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
        AlertDialog.Builder(this)
            .setTitle("Xóa phim")
            .setMessage("Bạn có chắc muốn xóa phim '${movie.title}' không?")
            .setPositiveButton("Xóa") { _, _ ->
                MockData.deleteMovie(movie)
                adapter.updateData(MockData.getAllMovies()) // Cập nhật lại list
                Toast.makeText(this, "Đã xóa!", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Hủy", null)
            .show()
    }
}