package com.example.movieticketbookingapp.ui.main

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.movieticketbookingapp.R
import com.example.movieticketbookingapp.adapter.BannerAdapter
import com.example.movieticketbookingapp.adapter.MovieAdapter
import com.example.movieticketbookingapp.adapter.SearchAdapter
import com.example.movieticketbookingapp.model.Movie
import com.example.movieticketbookingapp.ui.movie.DetailActivity
import com.example.movieticketbookingapp.ui.movie.MovieActivity
import com.example.movieticketbookingapp.ui.movie.MovieListActivity
import com.example.movieticketbookingapp.ui.ticket.MyTicketsActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.math.abs

class HomeActivity : AppCompatActivity() {

    private lateinit var vpBanner: ViewPager2
    private lateinit var rvNowShowing: RecyclerView
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var layoutIndicators: LinearLayout
    private lateinit var tvHotMovie: TextView
    private lateinit var etSearch: EditText
    private lateinit var rvSearchResults: RecyclerView
    private lateinit var searchAdapter: SearchAdapter
    private lateinit var btnSeeAllNowShowing: ImageView

    // Firebase
    private lateinit var db: FirebaseFirestore
    private var allMovies = ArrayList<Movie>()

    private val sliderHandler = Handler(Looper.getMainLooper())
    private val sliderRunnable = Runnable {
        // Kiểm tra nếu Adapter có dữ liệu
        val itemCount = vpBanner.adapter?.itemCount ?: 0
        if (itemCount > 0) {
            var nextItem = vpBanner.currentItem + 1
            // Nếu hết trang thì quay lại 0
            if (nextItem >= itemCount) {
                nextItem = 0
            }
            vpBanner.setCurrentItem(nextItem, true)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_home)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
            }
        })

        db = FirebaseFirestore.getInstance()

        initViews()
        setupBottomNav()
        setupSearchLogic()
        setupClickEvents()

        loadMoviesRealtime()
    }

    override fun onPause() {
        super.onPause()
        // Khi app ẩn xuống, dừng auto scroll để tiết kiệm pin
        sliderHandler.removeCallbacks(sliderRunnable)
    }

    override fun onResume() {
        super.onResume()
        if (bottomNav.selectedItemId != R.id.nav_home) {
            bottomNav.selectedItemId = R.id.nav_home
        }
        // Khi app hiện lại, tiếp tục auto scroll (sau 5s)
        sliderHandler.postDelayed(sliderRunnable, 5000)
    }
    // ------------------------------------

    private fun initViews() {
        vpBanner = findViewById(R.id.vpHotMovies)
        rvNowShowing = findViewById(R.id.rvNowShowing)
        bottomNav = findViewById(R.id.bottomNavigation)
        layoutIndicators = findViewById(R.id.layoutIndicators)
        tvHotMovie = findViewById(R.id.tvHotMovie)
        etSearch = findViewById(R.id.etSearch)
        rvSearchResults = findViewById(R.id.rvSearchResults)
        btnSeeAllNowShowing = findViewById(R.id.btnSeeAllNowShowing)
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
                            Log.e("FirebaseError", "Lỗi convert phim: ${doc.id}", err)
                        }
                    }
                    allMovies.sortByDescending { it.id }
                    updateUI()
                }
            }
    }

    private fun updateUI() {
        val bannerList = allMovies.filter{it.hot}
        setupBanner(bannerList)

        val nowShowingMovies = allMovies.filter { it.status == "now_showing" }
        setupNowShowingList(nowShowingMovies)
    }

    private fun setupBanner(movies: List<Movie>) {
        if (movies.isEmpty()) return

        val adapter = BannerAdapter(movies) { clickedMovie ->
            openDetailActivity(clickedMovie)
        }
        vpBanner.adapter = adapter

        setupIndicators(movies.size)

        // Đăng ký sự kiện lướt banner
        vpBanner.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                setCurrentIndicator(position)

                sliderHandler.removeCallbacks(sliderRunnable)
                sliderHandler.postDelayed(sliderRunnable, 5000)
            }
        })

        val child = vpBanner.getChildAt(0) as RecyclerView
        child.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // Khi ngón tay chạm vào -> Dừng Auto Scroll
                    sliderHandler.removeCallbacks(sliderRunnable)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    // Khi nhấc ngón tay ra -> Chạy lại Auto Scroll (sau 5s)
                    sliderHandler.postDelayed(sliderRunnable, 5000)
                }
            }
            false
        }
        // -------------------------------------------------

        vpBanner.setPageTransformer { page, position ->
            val r = 1 - abs(position)
            page.scaleY = 0.85f + r * 0.15f
        }
    }

    private fun setupClickEvents() {
        btnSeeAllNowShowing.setOnClickListener {
            val intent = Intent(this, MovieListActivity::class.java)
            intent.putExtra("status", "now_showing")
            intent.putExtra("title", "Now Showing")
            startActivity(intent)
        }
    }

    private fun setupNowShowingList(movies: List<Movie>) {
        val adapter = MovieAdapter(movies) { clickedMovie ->
            openDetailActivity(clickedMovie)
        }
        rvNowShowing.adapter = adapter
        rvNowShowing.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
    }

    private fun openDetailActivity(movie: Movie) {
        val intent = Intent(this, DetailActivity::class.java)
        intent.putExtra("movie_data", movie)
        startActivity(intent)
    }

    private fun setupIndicators(count: Int) {
        val indicators = arrayOfNulls<ImageView>(count)
        val sizePx = (8 * resources.displayMetrics.density).toInt()
        val layoutParams = LinearLayout.LayoutParams(sizePx, sizePx)
        layoutParams.setMargins(8, 0, 8, 0)
        layoutIndicators.removeAllViews()

        for (i in indicators.indices) {
            indicators[i] = ImageView(this)
            indicators[i]?.setImageDrawable(
                ContextCompat.getDrawable(
                    this,
                    R.drawable.bg_indicator_selector
                )
            )
            indicators[i]?.layoutParams = layoutParams
            layoutIndicators.addView(indicators[i])
        }
        if (count > 0) setCurrentIndicator(0)
    }

    private fun setCurrentIndicator(position: Int) {
        val childCount = layoutIndicators.childCount
        for (i in 0 until childCount) {
            val imageView = layoutIndicators.getChildAt(i) as ImageView
            imageView.isSelected = (i == position)
        }
    }

    private fun setupBottomNav() {
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
                R.id.nav_movie -> {
                    startActivity(Intent(this, MovieActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
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

    private fun setupSearchLogic() {
        searchAdapter = SearchAdapter(arrayListOf()) { clickedMovie ->
            openDetailActivity(clickedMovie)
        }
        rvSearchResults.adapter = searchAdapter
        rvSearchResults.layoutManager = LinearLayoutManager(this)

        etSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().trim()
                filterAndShowDropdown(query)
            }

            override fun afterTextChanged(s: android.text.Editable?) {}
        })
    }

    private fun filterAndShowDropdown(query: String) {
        if (query.isEmpty()) {
            rvSearchResults.visibility = android.view.View.GONE
        } else {
            val filteredList = ArrayList<Movie>()
            for (movie in allMovies) {
                if (movie.title.lowercase().contains(query.lowercase())) {
                    filteredList.add(movie)
                }
            }

            if (filteredList.isNotEmpty()) {
                rvSearchResults.visibility = android.view.View.VISIBLE
                searchAdapter.updateList(filteredList)
            } else {
                rvSearchResults.visibility = android.view.View.GONE
            }
        }
    }

    override fun dispatchTouchEvent(ev: android.view.MotionEvent?): Boolean {
        if (ev?.action == android.view.MotionEvent.ACTION_DOWN) {
            val v = currentFocus
            if (v is android.widget.EditText) {
                val outRect = android.graphics.Rect()
                v.getGlobalVisibleRect(outRect)
                if (!outRect.contains(ev.rawX.toInt(), ev.rawY.toInt())) {
                    v.clearFocus()
                    val imm = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                    imm.hideSoftInputFromWindow(v.windowToken, 0)
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }
}