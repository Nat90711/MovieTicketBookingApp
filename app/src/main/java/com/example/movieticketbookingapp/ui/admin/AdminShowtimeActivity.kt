package com.example.movieticketbookingapp.ui.admin

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.movieticketbookingapp.R
import com.example.movieticketbookingapp.model.Showtime
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FirebaseFirestore

class AdminShowtimeActivity : AppCompatActivity() {

    private lateinit var rvShowtimes: RecyclerView
    private lateinit var btnAdd: MaterialButton
    private lateinit var db: FirebaseFirestore
    private val showtimeList = ArrayList<Showtime>()
    private lateinit var adapter: ShowtimeAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_showtime_list)

        db = FirebaseFirestore.getInstance()
        rvShowtimes = findViewById(R.id.rvShowtimes)
        btnAdd = findViewById(R.id.btnAddShowtime)

        setupRecyclerView()

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        btnAdd.setOnClickListener {
            startActivity(Intent(this, AddShowtimeActivity::class.java))
        }

        listenToShowtimes()
    }

    private fun setupRecyclerView() {
        // Cập nhật Adapter nhận thêm onEditClick
        adapter = ShowtimeAdapter(
            showtimeList,
            onEditClick = { showtime ->
                // CHUYỂN SANG MÀN HÌNH SỬA
                val intent = Intent(this, EditShowtimeActivity::class.java)
                intent.putExtra("showtime_id", showtime.id)
                startActivity(intent)
            },
            onDeleteClick = { showtime ->
                confirmDelete(showtime)
            }
        )
        rvShowtimes.adapter = adapter
        rvShowtimes.layoutManager = LinearLayoutManager(this)
    }

    private fun listenToShowtimes() {
        db.collection("showtimes")
            .addSnapshotListener { value, error ->
                if (error != null) return@addSnapshotListener
                if (value != null) {
                    showtimeList.clear()
                    for (doc in value) {
                        val st = doc.toObject(Showtime::class.java)
                        st.id = doc.id
                        showtimeList.add(st)
                    }
                    showtimeList.sortByDescending { it.date }
                    adapter.notifyDataSetChanged()
                }
            }
    }

    private fun confirmDelete(showtime: Showtime) {
        // Bước 1: Hỏi xác nhận user trước
        AlertDialog.Builder(this)
            .setTitle("Xóa Lịch Chiếu")
            .setMessage("Bạn có chắc muốn xóa lịch chiếu phim '${showtime.movieTitle}' lúc ${showtime.time} không?")
            .setPositiveButton("Xóa") { _, _ ->
                checkAndPerformDelete(showtime) // Gọi hàm kiểm tra
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun checkAndPerformDelete(showtime: Showtime) {
        // Bước 2: Kiểm tra dữ liệu mới nhất trên Server (để tránh trường hợp vừa có người mua xong)
        db.collection("showtimes").document(showtime.id).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val bookedSeats = document.get("bookedSeats") as? List<*>

                    // RÀNG BUỘC 2: Kiểm tra vé đã bán
                    if (bookedSeats != null && bookedSeats.isNotEmpty()) {
                        // Đã có vé bán -> CHẶN XÓA
                        AlertDialog.Builder(this)
                            .setTitle("Không thể xóa!")
                            .setMessage("Suất chiếu này đã bán được ${bookedSeats.size} vé.\nViệc xóa suất chiếu sẽ làm mất dữ liệu vé của khách hàng.")
                            .setPositiveButton("Đóng", null)
                            .show()
                    } else {
                        // Chưa có vé -> XÓA
                        deleteShowtime(showtime.id)
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Lỗi kết nối Server", Toast.LENGTH_SHORT).show()
            }
    }

    private fun deleteShowtime(showtimeId: String) {
        db.collection("showtimes").document(showtimeId).delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Đã xóa thành công!", Toast.LENGTH_SHORT).show()
            }
    }

    // --- INNER ADAPTER CLASS ---
    inner class ShowtimeAdapter(
        private val list: List<Showtime>,
        private val onEditClick: (Showtime) -> Unit, // Callback Sửa
        private val onDeleteClick: (Showtime) -> Unit // Callback Xóa
    ) : RecyclerView.Adapter<ShowtimeAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val imgPoster: ImageView = view.findViewById(R.id.imgPoster)
            val tvMovie: TextView = view.findViewById(R.id.tvMovieTitle)
            val tvRoom: TextView = view.findViewById(R.id.tvRoomName)
            val tvDate: TextView = view.findViewById(R.id.tvDateTime)
            val btnEdit: ImageView = view.findViewById(R.id.btnEdit) // Nút Sửa mới
            val btnDelete: ImageView = view.findViewById(R.id.btnDelete)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_admin_showtime, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = list[position]
            holder.tvMovie.text = item.movieTitle
            holder.tvRoom.text = item.roomName
            holder.tvDate.text = "${item.date} - ${item.time}"

            Glide.with(holder.itemView.context)
                .load(item.posterUrl)
                .centerCrop()
                .placeholder(R.drawable.ic_launcher_background)
                .into(holder.imgPoster)

            // Bắt sự kiện Sửa
            holder.btnEdit.setOnClickListener {
                onEditClick(item)
            }

            // Bắt sự kiện Xóa
            holder.btnDelete.setOnClickListener {
                onDeleteClick(item)
            }
        }

        override fun getItemCount() = list.size
    }
}