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
    private val layoutId: Int = R.layout.item_movie,
    private val onItemClick: (Movie) -> Unit
) : RecyclerView.Adapter<MovieAdapter.MovieViewHolder>() {

    // Hàm cập nhật dữ liệu
    fun updateList(newList: List<Movie>) {
        movies = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovieViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
        return MovieViewHolder(view)
    }

    override fun onBindViewHolder(holder: MovieViewHolder, position: Int) {
        holder.bind(movies[position])
    }

    override fun getItemCount(): Int = movies.size

    inner class MovieViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imgPoster: ImageView = itemView.findViewById(R.id.imgPoster)
        private val tvTitle: TextView = itemView.findViewById(R.id.tvMovieTitle)

        private val tvDuration: TextView? = itemView.findViewById(R.id.tvDuration)

        fun bind(movie: Movie) {
            tvTitle.text = movie.title
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