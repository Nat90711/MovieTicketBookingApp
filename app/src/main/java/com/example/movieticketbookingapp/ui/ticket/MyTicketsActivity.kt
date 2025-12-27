package com.example.movieticketbookingapp.ui.ticket

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.movieticketbookingapp.R
import com.example.movieticketbookingapp.adapter.BookingAdapter
import com.example.movieticketbookingapp.model.Booking
import com.example.movieticketbookingapp.ui.main.HomeActivity
import com.example.movieticketbookingapp.ui.main.ProfileActivity
import com.example.movieticketbookingapp.ui.ticket.TicketDetailActivity
import com.example.movieticketbookingapp.ui.movie.MovieActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MyTicketsActivity : AppCompatActivity() {

    // Views
    private lateinit var rvTickets: RecyclerView
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var tvEmpty: TextView

    // Data & Adapter
    private lateinit var adapter: BookingAdapter
    private val bookingList = ArrayList<Booking>()

    // Firebase
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_tickets)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Không làm gì cả -> Vô hiệu hóa nút Back vật lý
            }
        })

        // 1. Khởi tạo Firebase
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // 2. Khởi tạo giao diện
        initViews()
        setupBottomNav()
        setupRecyclerView()

        // 3. Tải dữ liệu
        loadMyTickets()
    }

    override fun onResume() {
        super.onResume()
        if (bottomNav.selectedItemId != R.id.nav_cart) {
            bottomNav.selectedItemId = R.id.nav_cart
        }
    }

    private fun initViews() {
        rvTickets = findViewById(R.id.rvTickets)
        bottomNav = findViewById(R.id.bottomNavigation)
        tvEmpty = findViewById(R.id.tvEmpty)
    }

    private fun setupRecyclerView() {
        rvTickets.layoutManager = LinearLayoutManager(this)

        // Setup Adapter với sự kiện click xem chi tiết
        adapter = BookingAdapter(bookingList) { booking ->
            val intent = Intent(this, TicketDetailActivity::class.java)
            intent.putExtra("movie_title", booking.movieTitle)
            intent.putExtra("cinema_name", booking.cinema)
            intent.putExtra("date_time", booking.dateTime)
            intent.putStringArrayListExtra("seat_names", ArrayList(booking.seats))
            intent.putExtra("total_price", booking.totalPrice)
            intent.putExtra("poster_url", booking.posterUrl)
            intent.putExtra("booking_id", booking.id)
            intent.putExtra("payment_method", booking.paymentMethod)
            startActivity(intent)
        }
        rvTickets.adapter = adapter
    }

    private fun loadMyTickets() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("bookings")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { documents ->
                bookingList.clear()
                for (doc in documents) {
                    try {
                        val booking = doc.toObject(Booking::class.java)
                        booking.id = doc.id
                        bookingList.add(booking)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                // Sắp xếp: Đảo ngược để vé mới nhất lên đầu
                // (Cách đơn giản thay vì tạo index orderBy trên Firebase)
                bookingList.reverse()
                adapter.notifyDataSetChanged()

                // Kiểm tra danh sách trống để hiện thông báo
                if (bookingList.isEmpty()) {
                    tvEmpty.visibility = View.VISIBLE
                    rvTickets.visibility = View.GONE
                } else {
                    tvEmpty.visibility = View.GONE
                    rvTickets.visibility = View.VISIBLE
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Lỗi tải vé: ${it.message}", Toast.LENGTH_SHORT).show()
                tvEmpty.visibility = View.VISIBLE
            }
    }

    private fun setupBottomNav() {
        // Mặc định chọn tab Cart
        bottomNav.selectedItemId = R.id.nav_cart

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, HomeActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.nav_movie -> {
                    startActivity(Intent(this, MovieActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.nav_cart -> true // Đang ở đây rồi
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