package com.example.movieticketbookingapp.ui.movie

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.movieticketbookingapp.R
import com.example.movieticketbookingapp.adapter.MovieAdapter
import com.example.movieticketbookingapp.model.Movie
import com.example.movieticketbookingapp.ui.movie.DetailActivity
import com.google.firebase.firestore.FirebaseFirestore

class MovieListActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var rvMovieList: RecyclerView
    private lateinit var tvTitle: TextView
    private var movieList = ArrayList<Movie>()
    private lateinit var adapter: MovieAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_movie_list)

        db = FirebaseFirestore.getInstance()
        rvMovieList = findViewById(R.id.rvMovieList)
        tvTitle = findViewById(R.id.tvTitle)

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        // 1. Nhận dữ liệu từ Intent (Biết xem loại nào)
        val status = intent.getStringExtra("status") ?: "now_showing"
        val title = intent.getStringExtra("title") ?: "Movies"

        tvTitle.text = title

        // 2. Setup RecyclerView dạng GRID 2 CỘT
        setupRecyclerView()

        // 3. Load dữ liệu theo status
        loadMoviesByStatus(status)
    }

    private fun setupRecyclerView() {
        adapter = MovieAdapter(movieList, R.layout.item_movie_grid) { movie ->
            val intent = Intent(this, DetailActivity::class.java)
            intent.putExtra("movie_data", movie)
            startActivity(intent)
        }

        rvMovieList.adapter = adapter
        rvMovieList.layoutManager = GridLayoutManager(this, 2)
    }

    private fun loadMoviesByStatus(status: String) {
        db.collection("movies")
            .whereEqualTo("status", status) // Lọc theo status (now_showing hoặc coming_soon)
            .get()
            .addOnSuccessListener { documents ->
                movieList.clear()
                for (doc in documents) {
                    val movie = doc.toObject(Movie::class.java)
                    movieList.add(movie)
                }
                adapter.notifyDataSetChanged()

                if (movieList.isEmpty()) {
                    Toast.makeText(this, "Chưa có phim nào trong mục này", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Lỗi tải dữ liệu", Toast.LENGTH_SHORT).show()
            }
    }
}