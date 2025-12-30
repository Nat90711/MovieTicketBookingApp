package com.example.movieticketbookingapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.movieticketbookingapp.R
import com.example.movieticketbookingapp.model.MovieStat
import java.text.DecimalFormat

class AdminStatAdapter(
    private val stats: List<MovieStat>
) : RecyclerView.Adapter<AdminStatAdapter.StatViewHolder>() {

    // Tìm doanh thu cao nhất để làm mốc 100% cho ProgressBar
    private val maxRevenue = stats.maxOfOrNull { it.totalRevenue } ?: 1.0

    inner class StatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tvMovieName)
        val tvRevenue: TextView = itemView.findViewById(R.id.tvRevenue)
        val tvCount: TextView = itemView.findViewById(R.id.tvTicketCount)
        val progressBar: ProgressBar = itemView.findViewById(R.id.progressBarRevenue)

        fun bind(item: MovieStat) {
            tvName.text = item.movieTitle

            val formatter = DecimalFormat("#,### đ")
            tvRevenue.text = formatter.format(item.totalRevenue)

            tvCount.text = "Đã bán: ${item.ticketCount} vé"

            // Tính phần trăm: (Doanh thu phim / Doanh thu cao nhất) * 100
            val percentage = (item.totalRevenue / maxRevenue * 100).toInt()
            progressBar.progress = percentage
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StatViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_admin_stat, parent, false)
        return StatViewHolder(view)
    }

    override fun onBindViewHolder(holder: StatViewHolder, position: Int) {
        holder.bind(stats[position])
    }

    override fun getItemCount() = stats.size
}