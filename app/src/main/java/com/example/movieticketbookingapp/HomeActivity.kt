package com.example.movieticketbookingapp

import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.CompositePageTransformer
import androidx.viewpager2.widget.MarginPageTransformer
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlin.math.abs

class HomeActivity : AppCompatActivity() {

    private lateinit var vpBanner: ViewPager2
    private lateinit var rvNowShowing: RecyclerView
    private lateinit var bottomNav: BottomNavigationView

    private lateinit var layoutIndicators: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        initViews()
        setupBanner()
        setupNowShowingList()
        setupBottomNavigation()
    }

    private fun initViews() {
        vpBanner = findViewById(R.id.vpHotMovies)
        rvNowShowing = findViewById(R.id.rvNowShowing)
        bottomNav = findViewById(R.id.bottomNavigation)
        layoutIndicators = findViewById(R.id.layoutIndicators)
    }
    private fun setupBanner() {
        val movies = MockData.getAllMovies()
        val adapter = BannerAdapter(movies) { clickedMovie ->
            val intent = Intent(this, DetailActivity::class.java)
            intent.putExtra("movie_data", clickedMovie)
            startActivity(intent)
        }
        vpBanner.adapter = adapter

        setupIndicators(movies.size)

        vpBanner.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                setCurrentIndicator(position)
            }
        })

        // --- Tạo hiệu ứng Slide đẹp mắt (Optional) ---
        vpBanner.offscreenPageLimit = 3
        vpBanner.getChildAt(0).overScrollMode = RecyclerView.OVER_SCROLL_NEVER

        val compositePageTransformer = CompositePageTransformer()
        compositePageTransformer.addTransformer(MarginPageTransformer(40)) // Khoảng cách giữa các ảnh
        compositePageTransformer.addTransformer { page, position ->
            val r = 1 - abs(position)
            page.scaleY = 0.85f + r * 0.15f // Hiệu ứng thu nhỏ 2 bên
        }
        vpBanner.setPageTransformer(compositePageTransformer)
    }

    private fun setupIndicators(count: Int) {
        val indicators = arrayOfNulls<ImageView>(count)
        val layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        layoutParams.setMargins(8, 0, 8, 0) // Khoảng cách giữa các chấm

        // Xóa các view cũ nếu có (để tránh bị nhân đôi khi load lại)
        layoutIndicators.removeAllViews()

        for (i in indicators.indices) {
            indicators[i] = ImageView(this)
            indicators[i]?.setImageDrawable(
                androidx.core.content.ContextCompat.getDrawable(
                    this,
                    R.drawable.bg_indicator_selector // File drawable vừa tạo
                )
            )
            indicators[i]?.layoutParams = layoutParams
            layoutIndicators.addView(indicators[i])
        }

        // Mặc định chọn cái đầu tiên
        setCurrentIndicator(0)
    }

    // === HÀM ĐỔI MÀU CHẤM TRÒN ===
    private fun setCurrentIndicator(position: Int) {
        val childCount = layoutIndicators.childCount
        for (i in 0 until childCount) {
            val imageView = layoutIndicators.getChildAt(i) as ImageView
            // Nếu vị trí i trùng với position đang chọn thì set Selected = true (Màu trắng)
            // Ngược lại thì false (Màu xám)
            imageView.isSelected = (i == position)
        }
    }

    private fun setupNowShowingList() {
        // Đảo ngược danh sách để khác Banner một chút
        val movies = MockData.getNowShowing()

        val adapter = MovieAdapter(movies)
        rvNowShowing.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvNowShowing.adapter = adapter
    }

    private fun setupBottomNavigation() {
        bottomNav.selectedItemId = R.id.nav_home

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
                R.id.nav_movie -> {
                    // Mở màn hình MovieActivity
                    startActivity(Intent(this, MovieActivity::class.java))
                    overridePendingTransition(0, 0)
                    // Không finish() HomeActivity để khi back lại không bị load lại từ đầu (tuỳ logic của bạn)
                    true
                }
                // ... các case khác
                else -> false
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (bottomNav.selectedItemId != R.id.nav_home) {
            bottomNav.selectedItemId = R.id.nav_home
        }
    }
}