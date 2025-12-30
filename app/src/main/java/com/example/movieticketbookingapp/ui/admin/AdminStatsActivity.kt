package com.example.movieticketbookingapp.ui.admin

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.movieticketbookingapp.R
import com.example.movieticketbookingapp.adapter.AdminStatAdapter
import com.example.movieticketbookingapp.model.Booking
import com.example.movieticketbookingapp.model.MovieStat
import com.google.firebase.firestore.FirebaseFirestore
import java.text.DecimalFormat

class AdminStatsActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var tvTotalRevenue: TextView
    private lateinit var tvTotalTickets: TextView
    private lateinit var rvStats: RecyclerView
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_stats)

        db = FirebaseFirestore.getInstance()
        initViews()

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        loadStatistics()
    }

    private fun initViews() {
        tvTotalRevenue = findViewById(R.id.tvTotalRevenue)
        tvTotalTickets = findViewById(R.id.tvTotalTickets)
        rvStats = findViewById(R.id.rvStats)
        progressBar = findViewById(R.id.progressBarLoading)

        rvStats.layoutManager = LinearLayoutManager(this)
    }

    private fun loadStatistics() {
        progressBar.visibility = View.VISIBLE

        // Lấy toàn bộ dữ liệu trong collection "bookings"
        db.collection("bookings")
            .get()
            .addOnSuccessListener { result ->
                progressBar.visibility = View.GONE

                val bookingList = ArrayList<Booking>()
                for (doc in result) {
                    val booking = doc.toObject(Booking::class.java)
                    bookingList.add(booking)
                }

                calculateAndDisplay(bookingList)
            }
            .addOnFailureListener {
                progressBar.visibility = View.GONE
                Toast.makeText(this, "Lỗi tải thống kê: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun calculateAndDisplay(bookings: List<Booking>) {
        var grandTotalRevenue = 0.0
        var grandTotalTickets = 0

        // Map để gom nhóm theo tên phim: "Tên phim" -> (Doanh thu, Số vé)
        val statsMap = HashMap<String, Pair<Double, Int>>()

        for (booking in bookings) {
            // 1. Cộng dồn tổng
            grandTotalRevenue += booking.totalPrice
            val ticketCount = booking.seats.size
            grandTotalTickets += ticketCount

            // 2. Cộng dồn theo phim
            val movieTitle = booking.movieTitle
            val currentStat = statsMap[movieTitle] ?: Pair(0.0, 0)

            val newRevenue = currentStat.first + booking.totalPrice
            val newCount = currentStat.second + ticketCount

            statsMap[movieTitle] = Pair(newRevenue, newCount)
        }

        // 3. Hiển thị số liệu tổng
        val formatter = DecimalFormat("#,### đ")
        tvTotalRevenue.text = formatter.format(grandTotalRevenue)
        tvTotalTickets.text = grandTotalTickets.toString()

        // 4. Chuyển Map thành List MovieStat để hiển thị lên RecyclerView
        val statList = ArrayList<MovieStat>()
        for ((title, data) in statsMap) {
            statList.add(MovieStat(title, data.first, data.second))
        }

        // Sắp xếp giảm dần theo doanh thu (đã định nghĩa trong compareTo của model)
        statList.sort()

        // 5. Đưa vào Adapter
        val adapter = AdminStatAdapter(statList)
        rvStats.adapter = adapter
    }
}