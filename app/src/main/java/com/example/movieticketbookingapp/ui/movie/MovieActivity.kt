package com.example.movieticketbookingapp.ui.movie

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.movieticketbookingapp.R
import com.example.movieticketbookingapp.adapter.MovieAdapter
import com.example.movieticketbookingapp.model.Movie
import com.example.movieticketbookingapp.ui.movie.DetailActivity
import com.example.movieticketbookingapp.ui.main.HomeActivity
import com.example.movieticketbookingapp.ui.ticket.MyTicketsActivity
import com.example.movieticketbookingapp.ui.main.ProfileActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.FirebaseFirestore

class MovieActivity : AppCompatActivity() {

    private lateinit var rvNowShowing: RecyclerView
    private lateinit var rvComingSoon: RecyclerView
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var btnSeeAllNowShowing: ImageView
    private lateinit var btnSeeAllComingSoon: ImageView

    // Firebase
    private lateinit var db: FirebaseFirestore
    private var allMovies = ArrayList<Movie>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_movie)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Không làm gì cả
            }
        })

        db = FirebaseFirestore.getInstance()

        initViews()
        setupBottomNav()
        setupClickEvents()

        // Tải dữ liệu thật từ Firebase
        loadMoviesRealtime()
    }

    // Xử lý giữ trạng thái icon Bottom Nav khi quay lại
    override fun onResume() {
        super.onResume()
        if (bottomNav.selectedItemId != R.id.nav_movie) {
            bottomNav.selectedItemId = R.id.nav_movie
        }
    }

    private fun initViews() {
        rvNowShowing = findViewById(R.id.rvNowShowing)
        rvComingSoon = findViewById(R.id.rvComingSoon)
        bottomNav = findViewById(R.id.bottomNavigation)
        btnSeeAllNowShowing = findViewById(R.id.btnSeeAllNowShowing)
        btnSeeAllComingSoon = findViewById(R.id.btnSeeAllComingSoon)
    }

    private fun setupClickEvents() {
        // 1. Click vào mũi tên Now Showing
        btnSeeAllNowShowing.setOnClickListener {
            val intent = Intent(this, MovieListActivity::class.java)
            intent.putExtra("status", "now_showing") // Gửi tag status
            intent.putExtra("title", "Now Showing") // Gửi tiêu đề hiển thị
            startActivity(intent)
        }

        // 2. Click vào mũi tên Coming Soon
        btnSeeAllComingSoon.setOnClickListener {
            val intent = Intent(this, MovieListActivity::class.java)
            intent.putExtra("status", "coming_soon") // Gửi tag status
            intent.putExtra("title", "Coming Soon") // Gửi tiêu đề hiển thị
            startActivity(intent)
        }
    }

    private fun loadMoviesRealtime() {
        db.collection("movies")
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    allMovies.clear()
                    for (doc in snapshots) {
                        try {
                            val movie = doc.toObject(Movie::class.java)
                            allMovies.add(movie)
                        } catch (err: Exception) {
                            // Nếu dữ liệu phim này bị lỗi, in ra log để biết mà sửa
                            Log.e("FirebaseError", "Lỗi convert phim: ${doc.id}", err)
                        }
                    }
                    allMovies.sortByDescending { it.id }
                    updateUI()
                }
            }
    }

    private fun updateUI() {
        val nowShowingList = allMovies.filter { it.status == "now_showing" }

        val adapterNow = MovieAdapter(nowShowingList) { clickedMovie ->
            openDetailActivity(clickedMovie)
        }
        rvNowShowing.adapter = adapterNow
        rvNowShowing.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        // LỌC DANH SÁCH "SẮP CHIẾU"
        // Chỉ lấy những phim có status là "coming_soon"
        val comingSoonList = allMovies.filter { it.status == "coming_soon" }

        val adapterComing = MovieAdapter(comingSoonList) { clickedMovie ->
            openDetailActivity(clickedMovie)
        }
        rvComingSoon.adapter = adapterComing
        rvComingSoon.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
    }

    private fun openDetailActivity(movie: Movie) {
        val intent = Intent(
            this,
            DetailActivity::class.java
        )
        intent.putExtra("movie_data", movie)
        startActivity(intent)
    }

    private fun setupBottomNav() {
        // Đặt mặc định là tab Movie
        bottomNav.selectedItemId = R.id.nav_movie

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    val intent = Intent(this, HomeActivity::class.java)
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.nav_movie -> true
                R.id.nav_cart -> {
                    startActivity(Intent(this, MyTicketsActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                else -> false
            }
        }
    }
}