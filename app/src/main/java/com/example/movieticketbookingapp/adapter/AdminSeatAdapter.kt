package com.example.movieticketbookingapp.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.movieticketbookingapp.R

const val SEAT_TYPE_STANDARD = 0
const val SEAT_TYPE_VIP = 1
const val SEAT_TYPE_DOUBLE = 2
const val SEAT_TYPE_AISLE = -1

class AdminSeatAdapter(
    private val seatTypes: ArrayList<Int>,
    private val totalCols: Int,
    private val onSeatClick: (Int) -> Unit
) : RecyclerView.Adapter<AdminSeatAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val viewSeat: View = itemView.findViewById(R.id.viewSeat)
        val tvSeatName: TextView = itemView.findViewById(R.id.tvSeatName) // Ánh xạ TextView

        fun bind(type: Int, position: Int) {
            viewSeat.visibility = View.VISIBLE
            viewSeat.alpha = 1.0f
            viewSeat.background = null

            val rowChar = (position / totalCols + 65).toChar()

            var realSeatNum = 0
            val rowStart = (position / totalCols) * totalCols

            for (i in rowStart..position) {

                if (seatTypes[i] != SEAT_TYPE_AISLE) {
                    realSeatNum++
                }
            }

            tvSeatName.text = "$rowChar$realSeatNum"

            val params = viewSeat.layoutParams as ViewGroup.MarginLayoutParams
            val marginSize = 2
            params.setMargins(marginSize, marginSize, marginSize, marginSize)

            when (type) {
                SEAT_TYPE_STANDARD -> {
                    viewSeat.setBackgroundResource(R.drawable.bg_seat_available)
                    viewSeat.background.setTintList(null) // Màu gốc (xám nhạt)
                    tvSeatName.setTextColor(Color.BLACK)
                    tvSeatName.visibility = View.VISIBLE
                }
                SEAT_TYPE_VIP -> {
                    viewSeat.setBackgroundResource(R.drawable.bg_seat_available)
                    viewSeat.background.mutate().setTint(Color.parseColor("#F44336"))
                    tvSeatName.setTextColor(Color.WHITE)
                    tvSeatName.visibility = View.VISIBLE
                }
                SEAT_TYPE_AISLE -> {
                    viewSeat.visibility = View.INVISIBLE
                    tvSeatName.visibility = View.INVISIBLE
                }
                SEAT_TYPE_DOUBLE -> {
                    determineCoupleSeatBackground(position, params, marginSize)
                    viewSeat.background?.setTintList(null)
                    tvSeatName.setTextColor(Color.WHITE)
                    tvSeatName.visibility = View.VISIBLE
                }
            }

            viewSeat.layoutParams = params
            itemView.setOnClickListener { onSeatClick(adapterPosition) }
        }

        private fun determineCoupleSeatBackground(
            position: Int,
            params: ViewGroup.MarginLayoutParams,
            stdMargin: Int
        ) {
            val rowStart = (position / totalCols) * totalCols
            var consecutiveDoublesCount = 0
            var i = position - 1

            while (i >= rowStart && seatTypes[i] == SEAT_TYPE_DOUBLE) {
                consecutiveDoublesCount++
                i--
            }

            if (consecutiveDoublesCount % 2 == 0) {
                viewSeat.setBackgroundResource(R.drawable.bg_seat_couple_left)
                params.setMargins(stdMargin, stdMargin, 0, stdMargin)
            } else {
                viewSeat.setBackgroundResource(R.drawable.bg_seat_couple_right)
                params.setMargins(0, stdMargin, stdMargin, stdMargin)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_seat, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(seatTypes[position], position)
    }

    override fun getItemCount(): Int = seatTypes.size
}