package com.example.movieticketbookingapp.ui.booking

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.movieticketbookingapp.R
import com.example.movieticketbookingapp.model.Movie
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class BookingActivity : AppCompatActivity() {

    private lateinit var rvTime: RecyclerView
    private lateinit var rvDate: RecyclerView
    private lateinit var imgPoster: ImageView
    private lateinit var tvMovieTitle: TextView
    private lateinit var btnContinue: MaterialButton
    private lateinit var db: FirebaseFirestore
    private lateinit var tvEmptyTime: TextView

    // Dữ liệu
    private var movie: Movie? = null
    private var selectedDateFull: String = ""
    private var selectedShowtimeId: String = ""
    private var selectedTime: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_booking_main)

        db = FirebaseFirestore.getInstance()
        initViews()

        // 1. Nhận dữ liệu phim
        movie = intent.getParcelableExtra("movie_data")
        if (movie != null) {
            setupMovieInfo(movie!!)
        }

        // 2. Setup danh sách ngày
        setupDateList()

        // 3. Setup danh sách giờ
        setupTimeList()

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        // 4. Xử lý nút Continue
        btnContinue.setOnClickListener {
            // KIỂM TRA: Bắt buộc phải chọn suất chiếu mới được đi tiếp
            if (selectedShowtimeId.isNotEmpty()) {
                val intent = Intent(this, SelectTicketTypeActivity::class.java)
                intent.putExtra("movie_data", movie)
                intent.putExtra("showtime_id", selectedShowtimeId)
                intent.putExtra("selected_date", selectedDateFull)
                intent.putExtra("selected_time", selectedTime)
                intent.putExtra("cinema_name", "Cinestar Sinh Viên")
                startActivity(intent)
            } else {
                Toast.makeText(this, "Vui lòng chọn suất chiếu!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun initViews() {
        rvTime = findViewById(R.id.rvTime)
        rvDate = findViewById(R.id.rvDate)
        imgPoster = findViewById(R.id.imgPoster)
        tvMovieTitle = findViewById(R.id.tvMovieTitle)
        btnContinue = findViewById(R.id.btnContinue)
        tvEmptyTime = findViewById(R.id.tvEmptyTime)
    }

    private fun setupMovieInfo(movie: Movie) {
        tvMovieTitle.text = movie.title
        Glide.with(this).load(movie.posterUrl).centerCrop().into(imgPoster)
    }

    data class DateItem(val dayOfWeek: String, val dayOfMonth: String, val fullDate: String)

    private fun setupDateList() {
        val dateList = ArrayList<DateItem>()
        val calendar = Calendar.getInstance()
        val dayFormat = SimpleDateFormat("EEE", Locale.ENGLISH)
        val dateFormat = SimpleDateFormat("dd", Locale.ENGLISH)
        val fullFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        for (i in 0..6) {
            val dayOfWeek = dayFormat.format(calendar.time).uppercase()
            val dayOfMonth = dateFormat.format(calendar.time)
            val fullDate = fullFormat.format(calendar.time)

            dateList.add(DateItem(dayOfWeek, dayOfMonth, fullDate))
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        val adapter = DateAdapter(dateList) { dateItem ->
            selectedDateFull = dateItem.fullDate
            // Reset suất chiếu cũ khi chọn ngày mới
            selectedShowtimeId = ""
            selectedTime = ""
            loadShowtimesForDate(selectedDateFull)
        }
        rvDate.adapter = adapter
        rvDate.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
    }

    data class ShowtimeItem(val id: String, val time: String)

    private fun setupTimeList() {
        val adapter = TimeAdapter(listOf()) { showtime ->
            selectedShowtimeId = showtime.id
            selectedTime = showtime.time
        }
        rvTime.adapter = adapter
        rvTime.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
    }

    private fun loadShowtimesForDate(date: String) {
        val movieId = movie?.id ?: return

        db.collection("showtimes")
            .whereEqualTo("movieId", movieId)
            .whereEqualTo("date", date)
            .get()
            .addOnSuccessListener { documents ->
                val showtimeList = ArrayList<ShowtimeItem>()

                for (doc in documents) {
                    val id = doc.id
                    val time = doc.getString("time") ?: ""
                    if (time.isNotEmpty()) {
                        showtimeList.add(ShowtimeItem(id, time))
                    }
                }

                showtimeList.sortBy { it.time }

                if (showtimeList.isEmpty()) {
                    rvTime.visibility = View.GONE
                    tvEmptyTime.visibility = View.VISIBLE
                    tvEmptyTime.text = "Không có suất chiếu ngày $date"
                } else {
                    rvTime.visibility = View.VISIBLE
                    tvEmptyTime.visibility = View.GONE

                    if (rvTime.adapter is TimeAdapter) {
                        (rvTime.adapter as TimeAdapter).updateData(showtimeList)
                    }

                    selectedShowtimeId = ""
                    selectedTime = ""
                    // ----------------------------------------
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Lỗi tải lịch chiếu: ${it.message}", Toast.LENGTH_SHORT).show()
                rvTime.visibility = View.GONE
                tvEmptyTime.visibility = View.VISIBLE
                tvEmptyTime.text = "Lỗi kết nối!"
            }
    }

    // --- ADAPTERS ---

    inner class DateAdapter(
        private val dates: List<DateItem>,
        private val onItemClick: (DateItem) -> Unit
    ) : RecyclerView.Adapter<DateAdapter.ViewHolder>() {

        private var selectedPosition = -1

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val container: LinearLayout = view.findViewById(R.id.itemDate)
            val tvDayOfWeek: TextView = view.findViewById(R.id.tvDayOfWeek)
            val tvDayOfMonth: TextView = view.findViewById(R.id.tvDayOfMonth)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_date_slot, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = dates[position]
            holder.tvDayOfWeek.text = item.dayOfWeek
            holder.tvDayOfMonth.text = item.dayOfMonth

            if (selectedPosition == position) {
                holder.container.setBackgroundResource(R.drawable.bg_time_slot_selected)
                holder.tvDayOfWeek.setTextColor(Color.WHITE)
                holder.tvDayOfMonth.setTextColor(Color.WHITE)
            } else {
                holder.container.setBackgroundResource(R.drawable.bg_time_slot_selector)
                val defaultColor = ContextCompat.getColor(holder.itemView.context, R.color.text_primary)
                holder.tvDayOfWeek.setTextColor(defaultColor)
                holder.tvDayOfMonth.setTextColor(defaultColor)
            }

            holder.itemView.setOnClickListener {
                val previousItem = selectedPosition
                selectedPosition = holder.adapterPosition
                notifyItemChanged(previousItem)
                notifyItemChanged(selectedPosition)
                onItemClick(item)
            }
        }

        override fun getItemCount() = dates.size
    }

    inner class TimeAdapter(
        private var showtimes: List<ShowtimeItem>,
        private val onItemClick: (ShowtimeItem) -> Unit
    ) : RecyclerView.Adapter<TimeAdapter.ViewHolder>() {

        private var selectedPosition = -1

        fun updateData(newList: List<ShowtimeItem>) {
            this.showtimes = newList
            this.selectedPosition = -1 // Reset lại trạng thái chưa chọn gì
            notifyDataSetChanged()
        }

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvTime: TextView = view.findViewById(R.id.tvTime)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_time_slot, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            if (showtimes.isEmpty()) return

            val item = showtimes[position]
            holder.tvTime.text = item.time

            if (selectedPosition == position) {
                holder.tvTime.setBackgroundResource(R.drawable.bg_time_slot_selected)
                holder.tvTime.setTextColor(Color.WHITE)
            } else {
                holder.tvTime.setBackgroundResource(R.drawable.bg_time_slot_selector)
                val defaultColor = ContextCompat.getColor(holder.itemView.context, R.color.text_primary)
                holder.tvTime.setTextColor(defaultColor)
            }

            holder.itemView.setOnClickListener {
                val previousItem = selectedPosition
                selectedPosition = holder.adapterPosition
                notifyItemChanged(previousItem)
                notifyItemChanged(selectedPosition)
                onItemClick(item)
            }
        }

        override fun getItemCount() = showtimes.size
    }
}