package com.example.movieticketbookingapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.movieticketbookingapp.R
import com.example.movieticketbookingapp.model.Movie

class MovieAdapter(
    private var movies: List<Movie>,
    // 👇 THAM SỐ MỚI: layoutId (Mặc định là item_movie cũ để không lỗi bên Home)
    private val layoutId: Int = R.layout.item_movie,
    private val onItemClick: (Movie) -> Unit
) : RecyclerView.Adapter<MovieAdapter.MovieViewHolder>() {

    // Hàm cập nhật dữ liệu (Dùng cho Search hoặc Filter sau này rất tiện)
    fun updateList(newList: List<Movie>) {
        movies = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovieViewHolder {
        // 👇 SỬ DỤNG layoutId ĐƯỢC TRUYỀN VÀO THAY VÌ HARDCODE
        val view = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
        return MovieViewHolder(view)
    }

    override fun onBindViewHolder(holder: MovieViewHolder, position: Int) {
        holder.bind(movies[position])
    }

    override fun getItemCount(): Int = movies.size

    inner class MovieViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // 👇 Lưu ý: ID trong item_movie.xml và item_movie_grid.xml PHẢI GIỐNG NHAU
        private val imgPoster: ImageView = itemView.findViewById(R.id.imgPoster)
        private val tvTitle: TextView = itemView.findViewById(R.id.tvMovieTitle)

        // Các view phụ (Có thể null nếu layout cũ không có)
        // Ví dụ: item_movie_grid có hiện thời lượng, nhưng item_movie thì không
        private val tvDuration: TextView? = itemView.findViewById(R.id.tvDuration)

        fun bind(movie: Movie) {
            tvTitle.text = movie.title

            // Set data cho các view phụ nếu tìm thấy view đó trong layout
            tvDuration?.text = "${movie.duration} phút"


            Glide.with(itemView.context)
                .load(movie.posterUrl)
                .centerCrop()
                .into(imgPoster)

            itemView.setOnClickListener {
                onItemClick(movie)
            }
        }
    }
}