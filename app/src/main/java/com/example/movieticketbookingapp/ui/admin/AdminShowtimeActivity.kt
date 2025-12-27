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

        // Nút Back
        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        btnAdd.setOnClickListener {
            startActivity(Intent(this, AddShowtimeActivity::class.java))
        }

        // Lắng nghe dữ liệu realtime
        listenToShowtimes()
    }

    private fun setupRecyclerView() {
        adapter = ShowtimeAdapter(showtimeList) { showtime ->
            // Sự kiện khi bấm nút Xóa
            confirmDelete(showtime)
        }
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
                        // Gán lại ID từ document nếu model chưa có
                        st.id = doc.id
                        showtimeList.add(st)
                    }
                    // Sắp xếp theo ngày giảm dần hoặc tên phim tùy bạn
                    showtimeList.sortByDescending { it.date }
                    adapter.notifyDataSetChanged()
                }
            }
    }

    private fun confirmDelete(showtime: Showtime) {
        AlertDialog.Builder(this)
            .setTitle("Xóa Lịch Chiếu")
            .setMessage("Bạn có chắc muốn xóa lịch chiếu phim '${showtime.movieTitle}' lúc ${showtime.time} không?\n(Dữ liệu đặt vé liên quan cũng nên được xử lý)")
            .setPositiveButton("Xóa") { _, _ ->
                deleteShowtime(showtime.id)
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun deleteShowtime(id: String) {
        db.collection("showtimes").document(id)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Đã xóa thành công!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Lỗi khi xóa: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // --- INNER ADAPTER CLASS ---
    inner class ShowtimeAdapter(
        private val list: List<Showtime>,
        private val onDeleteClick: (Showtime) -> Unit
    ) : RecyclerView.Adapter<ShowtimeAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val imgPoster: ImageView = view.findViewById(R.id.imgPoster)
            val tvMovie: TextView = view.findViewById(R.id.tvMovieTitle)
            val tvRoom: TextView = view.findViewById(R.id.tvRoomName)
            val tvDate: TextView = view.findViewById(R.id.tvDateTime)
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

            holder.btnDelete.setOnClickListener {
                onDeleteClick(item)
            }
        }

        override fun getItemCount() = list.size
    }
}