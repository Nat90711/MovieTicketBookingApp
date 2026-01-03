package com.example.movieticketbookingapp.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.movieticketbookingapp.R

class UserSeatAdapter(
    private var seatList: List<Seat>,
    private val totalCols: Int,
    private val isCoupleMode: Boolean,
    private val onSeatClick: (Seat) -> Unit
) : RecyclerView.Adapter<UserSeatAdapter.SeatViewHolder>() {

    private fun isLeftSeatOfCouple(pos: Int, type: Int): Boolean {
        if (type != 2) return false
        val rowStart = (pos / totalCols) * totalCols
        var consecutiveDoubles = 0
        var i = pos - 1
        while (i >= rowStart && seatList[i].type == 2) {
            consecutiveDoubles++
            i--
        }
        return (consecutiveDoubles % 2 == 0)
    }

    inner class SeatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val viewSeat: View = itemView.findViewById(R.id.viewSeat)
        val tvSeatName: TextView = itemView.findViewById(R.id.tvSeatName) // Ánh xạ TextView

        fun bind(seat: Seat, position: Int) {
            // 1. Reset visual
            viewSeat.visibility = View.VISIBLE
            viewSeat.alpha = 1.0f
            viewSeat.background = null
            viewSeat.backgroundTintList = null

            val rowChar = (position / totalCols + 65).toChar()

            var realSeatNum = 0
            val rowStart = (position / totalCols) * totalCols

            for (i in rowStart..position) {
                if (seatList[i].type != -1) {
                    realSeatNum++
                }
            }

            tvSeatName.text = "$rowChar$realSeatNum"

            // Layout Params
            val params = viewSeat.layoutParams as ViewGroup.MarginLayoutParams
            val marginSize = 2
            params.setMargins(marginSize, marginSize, marginSize, marginSize)

            if (seat.status == SeatStatus.AISLE || seat.type == -1) {
                viewSeat.visibility = View.INVISIBLE
                tvSeatName.visibility = View.INVISIBLE // Ẩn tên ghế lối đi
                return
            } else {
                tvSeatName.visibility = View.VISIBLE
            }

            var bgResId = R.drawable.bg_seat_available
            if (seat.type == 2) { // Ghế đôi
                if (isLeftSeatOfCouple(position, seat.type)) {
                    bgResId = R.drawable.bg_seat_couple_left
                    params.setMargins(marginSize, marginSize, 0, marginSize)
                } else {
                    bgResId = R.drawable.bg_seat_couple_right
                    params.setMargins(0, marginSize, marginSize, marginSize)
                }
            }
            viewSeat.setBackgroundResource(bgResId)
            viewSeat.layoutParams = params

            var textColor = Color.BLACK // Mặc định chữ đen

            val color = when (seat.status) {
                SeatStatus.BOOKED -> {
                    textColor = Color.WHITE
                    Color.parseColor("#888888")
                }
                SeatStatus.HELD -> {
                    textColor = Color.BLACK
                    Color.parseColor("#FFC107")
                }
                SeatStatus.SELECTED -> {
                    textColor = Color.WHITE
                    Color.parseColor("#4A90E2")
                }
                SeatStatus.AVAILABLE -> {
                    when (seat.type) {
                        1 -> { // VIP
                            textColor = Color.WHITE
                            Color.parseColor("#F44336")
                        }
                        2 -> { // Couple
                            textColor = Color.WHITE
                            Color.parseColor("#E91E63")
                        }
                        else -> { // Thường
                            textColor = Color.BLACK
                            Color.parseColor("#E0E0E0")
                        }
                    }
                }
                else -> Color.WHITE
            }

            viewSeat.background.setTint(color)
            tvSeatName.setTextColor(textColor)

            // Logic Click
            var isEnable = true
            if (seat.status == SeatStatus.BOOKED || seat.status == SeatStatus.HELD) {
                isEnable = false
            } else {
                if (isCoupleMode) {
                    if (seat.type != 2) isEnable = false
                } else {
                    if (seat.type == 2) isEnable = false
                }
            }

            if (seat.status == SeatStatus.BOOKED || seat.status == SeatStatus.HELD) {
                viewSeat.alpha = 1.0f
                itemView.setOnClickListener {}
            } else {
                var isCompatible = true
                if (isCoupleMode) {
                    if (seat.type != 2) isCompatible = false
                } else {
                    if (seat.type == 2) isCompatible = false
                }

                if (isCompatible) {
                    viewSeat.alpha = 1.0f
                    itemView.setOnClickListener { onSeatClick(seat) }
                } else {
                    viewSeat.alpha = 0.3f
                    itemView.setOnClickListener {
                        val msg = if (isCoupleMode) "Bạn đang mua Vé Đôi, chỉ được chọn ghế đôi!"
                        else "Bạn đang mua Vé Đơn, không thể chọn ghế đôi!"
                        Toast.makeText(itemView.context, msg, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SeatViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_seat, parent, false)
        return SeatViewHolder(view)
    }

    override fun onBindViewHolder(holder: SeatViewHolder, position: Int) {
        holder.bind(seatList[position], position)
    }

    override fun getItemCount(): Int = seatList.size
}