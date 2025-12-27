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

class SearchAdapter(
    private var movies: List<Movie>,
    private val onItemClick: (Movie) -> Unit
) : RecyclerView.Adapter<SearchAdapter.SearchViewHolder>() {

    inner class SearchViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imgPoster: ImageView = itemView.findViewById(R.id.imgPoster)
        private val tvTitle: TextView = itemView.findViewById(R.id.tvMovieTitle)
        private val tvGenre: TextView = itemView.findViewById(R.id.tvGenre)
        private val tvDuration: TextView = itemView.findViewById(R.id.tvDuration)

        fun bind(movie: Movie) {
            tvTitle.text = movie.title
            tvGenre.text = movie.genre
            tvDuration.text = "${movie.duration}"

            Glide.with(itemView.context).load(movie.posterUrl).into(imgPoster)

            itemView.setOnClickListener { onItemClick(movie) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_search_result, parent, false)
        return SearchViewHolder(view)
    }

    override fun onBindViewHolder(holder: SearchViewHolder, position: Int) {
        holder.bind(movies[position])
    }

    override fun getItemCount(): Int = movies.size

    // Hàm cập nhật dữ liệu mới
    fun updateList(newList: List<Movie>) {
        movies = newList
        notifyDataSetChanged()
    }
}