package com.example.movieticketbookingapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.util.Locale

class AdminMovieAdapter(
    private var originalList: ArrayList<Movie>, // Danh sách gốc
    private val onEditClick: (Movie) -> Unit,
    private val onDeleteClick: (Movie) -> Unit
) : RecyclerView.Adapter<AdminMovieAdapter.AdminViewHolder>() {

    // Danh sách hiển thị (có thể bị lọc khi tìm kiếm)
    private var displayList: ArrayList<Movie> = ArrayList(originalList)

    inner class AdminViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgPoster: ImageView = itemView.findViewById(R.id.imgPoster)
        val tvName: TextView = itemView.findViewById(R.id.tvName)
        val tvGenre: TextView = itemView.findViewById(R.id.tvGenre)
        val tvDuration: TextView = itemView.findViewById(R.id.tvDuration)
        val tvID: TextView = itemView.findViewById(R.id.tvID)
        val btnEdit: ImageView = itemView.findViewById(R.id.btnEdit)
        val btnDelete: ImageView = itemView.findViewById(R.id.btnDelete)

        fun bind(movie: Movie) {
            tvName.text = "Tên: ${movie.title}"
            tvGenre.text = "Thể loại: ${movie.genre}"
            tvDuration.text = "Thời lượng: ${movie.duration}"
            tvID.text = "ID: ${movie.id}"

            Glide.with(itemView.context)
                .load(movie.posterUrl)
                .centerCrop()
                .placeholder(R.drawable.ic_launcher_background)
                .into(imgPoster)

            // Sự kiện click
            btnEdit.setOnClickListener { onEditClick(movie) }
            btnDelete.setOnClickListener { onDeleteClick(movie) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdminViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_admin_movie, parent, false)
        return AdminViewHolder(view)
    }

    override fun onBindViewHolder(holder: AdminViewHolder, position: Int) {
        holder.bind(displayList[position])
    }

    override fun getItemCount(): Int = displayList.size

    // --- HÀM TÌM KIẾM ---
    fun filter(query: String) {
        val searchText = query.lowercase(Locale.getDefault())
        displayList.clear()
        if (searchText.isEmpty()) {
            displayList.addAll(originalList)
        } else {
            for (movie in originalList) {
                if (movie.title.lowercase(Locale.getDefault()).contains(searchText) ||
                    movie.id.toString().contains(searchText)) {
                    displayList.add(movie)
                }
            }
        }
        notifyDataSetChanged()
    }

    // Hàm cập nhật data khi thêm/xóa
    fun updateData(newList: ArrayList<Movie>) {
        originalList = ArrayList(newList)
        filter("") // Reset lại list hiển thị
    }
}