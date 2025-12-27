package com.example.movieticketbookingapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.movieticketbookingapp.R
import com.example.movieticketbookingapp.model.Booking
import com.google.android.material.button.MaterialButton

class BookingAdapter(
    private val bookingList: List<Booking>,
    private val onViewClick: (Booking) -> Unit
) : RecyclerView.Adapter<BookingAdapter.BookingViewHolder>() {

    class BookingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgPoster: ImageView = itemView.findViewById(R.id.imgPoster)
        val tvTitle: TextView = itemView.findViewById(R.id.tvMovieTitle)
        val tvCinema: TextView = itemView.findViewById(R.id.tvCinema)
        val tvDateTime: TextView = itemView.findViewById(R.id.tvDateTime)
        val tvSeats: TextView = itemView.findViewById(R.id.tvSeats)
        val btnView: MaterialButton = itemView.findViewById(R.id.btnViewTicket)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_booked_ticket, parent, false)
        return BookingViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookingViewHolder, position: Int) {
        val booking = bookingList[position]

        holder.tvTitle.text = booking.movieTitle
        holder.tvCinema.text = booking.cinema
        holder.tvDateTime.text = booking.dateTime
        holder.tvSeats.text = "Seats: ${booking.seats.joinToString(", ")}" // Hiển thị ghế: Seats: A1, A2

        // Load Poster
        Glide.with(holder.itemView.context)
            .load(booking.posterUrl)
            .centerCrop()
            .placeholder(R.drawable.ic_launcher_background)
            .into(holder.imgPoster)

        // Sự kiện Click
        holder.btnView.setOnClickListener { onViewClick(booking) }
    }

    override fun getItemCount() = bookingList.size
}