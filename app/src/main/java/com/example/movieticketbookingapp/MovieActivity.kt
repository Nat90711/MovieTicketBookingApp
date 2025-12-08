package com.example.movieticketbookingapp

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager // Import cái này
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView

class MovieActivity : AppCompatActivity() {

    private lateinit var rvNowShowing: RecyclerView
    private lateinit var rvComingSoon: RecyclerView
    private lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_movie)

        initViews()
        setupMovies()
        setupBottomNavigation()
    }

    private fun initViews() {
        rvNowShowing = findViewById(R.id.rvNowShowing)
        rvComingSoon = findViewById(R.id.rvComingSoon)
        bottomNav = findViewById(R.id.bottomNavigation)
        bottomNav.selectedItemId = R.id.nav_movie
    }

    private fun setupMovies() {
        // --- 1. Now Showing (Lướt ngang) ---
        val nowShowingList = MockData.getNowShowing()
        val adapterNow = MovieAdapter(nowShowingList)

        // SỬA Ở ĐÂY: Dùng LinearLayoutManager HORIZONTAL
        rvNowShowing.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvNowShowing.adapter = adapterNow

        // --- 2. Coming Soon (Lướt ngang) ---
        val comingSoonList = MockData.getComingSoon()
        val adapterComing = MovieAdapter(comingSoonList)

        // SỬA Ở ĐÂY: Dùng LinearLayoutManager HORIZONTAL
        rvComingSoon.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvComingSoon.adapter = adapterComing
    }

    private fun setupBottomNavigation() {
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, HomeActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.nav_movie -> true
                R.id.nav_cart -> true // TODO
                R.id.nav_profile -> true // TODO
                else -> false
            }
        }
    }
}