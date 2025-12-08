package com.example.movieticketbookingapp

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
import com.google.android.material.button.MaterialButton

class BookingActivity : AppCompatActivity() {

    private lateinit var rvTime: RecyclerView
    private lateinit var rvDate: RecyclerView
    private lateinit var imgPoster: ImageView
    private lateinit var tvMovieTitle: TextView
    private lateinit var btnContinue: MaterialButton

    // Biến lưu giá trị đang chọn để gửi đi tiếp (nếu cần)
    private var selectedTime: String? = "15:15" // Mặc định chọn cái đầu
    private var selectedDate: String? = "12"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_booking)

        initViews()

        val movie = intent.getParcelableExtra<Movie>("movie_data")
        if (movie != null) {
            setupMovieInfo(movie)
        }

        setupLists()

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        btnContinue.setOnClickListener {
            val movie = intent.getParcelableExtra<Movie>("movie_data")

            if (movie != null) {
                val intent = Intent(this, SeatSelectionActivity::class.java)

                // 1. Truyền tiếp object Phim sang màn hình chọn ghế
                intent.putExtra("movie_data", movie)

                // 2. Truyền thông tin Lịch chiếu đã chọn (để sau này in lên vé)
                intent.putExtra("selected_date", selectedDate)
                intent.putExtra("selected_time", selectedTime)
                intent.putExtra("cinema_name", "Cinestar Sinh Viên (Bình Dương)") // Hoặc lấy từ Spinner

                startActivity(intent)
            } else {
                Toast.makeText(this, "Lỗi dữ liệu phim!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun initViews() {
        rvTime = findViewById(R.id.rvTime)
        rvDate = findViewById(R.id.rvDate)
        imgPoster = findViewById(R.id.imgPoster)
        tvMovieTitle = findViewById(R.id.tvMovieTitle)
        btnContinue = findViewById(R.id.btnContinue)
    }

    private fun setupMovieInfo(movie: Movie) {
        tvMovieTitle.text = movie.title
        Glide.with(this).load(movie.posterUrl).centerCrop().into(imgPoster)
    }

    private fun setupLists() {
        val times = listOf("15:15", "16:30", "18:10", "19:20", "20:20", "21:25")
        // Truyền callback (lambda) để khi click thì cập nhật biến selectedTime
        val timeAdapter = TimeAdapter(times) { time ->
            selectedTime = time
        }
        rvTime.adapter = timeAdapter
        rvTime.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        val dates = listOf("FRI" to "12", "SAT" to "13", "SUN" to "14", "MON" to "15", "TUE" to "16")
        val dateAdapter = DateAdapter(dates) { date ->
            selectedDate = date
        }
        rvDate.adapter = dateAdapter
        rvDate.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
    }

    // --- ADAPTER GIỜ (Có xử lý click) ---
    inner class TimeAdapter(
        private val times: List<String>,
        private val onItemClick: (String) -> Unit
    ) : RecyclerView.Adapter<TimeAdapter.ViewHolder>() {

        private var selectedPosition = 0 // Mặc định chọn cái đầu tiên

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvTime: TextView = view.findViewById(R.id.tvTime)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_time_slot, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.tvTime.text = times[position]

            // LOGIC ĐỔI MÀU
            if (selectedPosition == position) {
                // Đang chọn: Nền đen, Chữ trắng
                holder.tvTime.setBackgroundResource(R.drawable.bg_time_slot_selected)
                holder.tvTime.setTextColor(Color.WHITE)
            } else {
                // Không chọn: Nền viền (cũ), Chữ đen
                holder.tvTime.setBackgroundResource(R.drawable.bg_time_slot_selector)
                holder.tvTime.setTextColor(Color.BLACK)
            }

            // XỬ LÝ CLICK
            holder.itemView.setOnClickListener {
                val previousItem = selectedPosition
                selectedPosition = holder.adapterPosition

                // Chỉ load lại 2 item bị thay đổi để tối ưu (hoặc dùng notifyDataSetChanged() cho nhanh)
                notifyItemChanged(previousItem)
                notifyItemChanged(selectedPosition)

                // Gửi dữ liệu ra ngoài
                onItemClick(times[position])
            }
        }

        override fun getItemCount() = times.size
    }

    // --- ADAPTER NGÀY (Có xử lý click) ---
    inner class DateAdapter(
        private val dates: List<Pair<String, String>>,
        private val onItemClick: (String) -> Unit
    ) : RecyclerView.Adapter<DateAdapter.ViewHolder>() {

        private var selectedPosition = 0

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
            holder.tvDayOfWeek.text = dates[position].first
            holder.tvDayOfMonth.text = dates[position].second

            // LOGIC ĐỔI MÀU
            if (selectedPosition == position) {
                holder.container.setBackgroundResource(R.drawable.bg_time_slot_selected)
                holder.tvDayOfWeek.setTextColor(Color.WHITE)
                holder.tvDayOfMonth.setTextColor(Color.WHITE)
            } else {
                holder.container.setBackgroundResource(R.drawable.bg_time_slot_selector)
                holder.tvDayOfWeek.setTextColor(Color.GRAY) // Thứ màu xám
                holder.tvDayOfMonth.setTextColor(Color.BLACK)
            }

            // XỬ LÝ CLICK
            holder.itemView.setOnClickListener {
                val previousItem = selectedPosition
                selectedPosition = holder.adapterPosition
                notifyItemChanged(previousItem)
                notifyItemChanged(selectedPosition)

                onItemClick(dates[position].second)
            }
        }

        override fun getItemCount() = dates.size
    }
}