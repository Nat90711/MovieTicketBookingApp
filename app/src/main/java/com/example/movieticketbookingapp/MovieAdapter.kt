package com.example.movieticketbookingapp

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class MovieAdapter(private val movies: List<Movie>) : RecyclerView.Adapter<MovieAdapter.MovieViewHolder>() {

    inner class MovieViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgPoster: ImageView = itemView.findViewById(R.id.imgPoster)
        val tvTitle: TextView = itemView.findViewById(R.id.tvMovieTitle)

        fun bind(movie: Movie) {
            tvTitle.text = movie.title
            Glide.with(itemView.context)
                .load(movie.posterUrl)
                .centerCrop()
                .into(imgPoster)

            // Xử lý sự kiện click vào phim (để sang trang chi tiết sau này)
            itemView.setOnClickListener {
                val intent =
                    Intent(itemView.context, DetailActivity::class.java)
                intent.putExtra("movie_data", movie) // Truyền object movie qua
                itemView.context.startActivity(intent)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovieViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_movie, parent, false)
        return MovieViewHolder(view)
    }

    override fun onBindViewHolder(holder: MovieViewHolder, position: Int) {
        holder.bind(movies[position])
    }

    override fun getItemCount(): Int = movies.size
}