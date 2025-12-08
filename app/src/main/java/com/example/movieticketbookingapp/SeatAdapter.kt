package com.example.movieticketbookingapp

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
// Định nghĩa 4 trạng thái ghế
enum class SeatStatus { AVAILABLE, SELECTED, BOOKED, AISLE }

data class Seat(val id: Int, var status: SeatStatus)

class SeatAdapter(
    private val seats: List<Seat>,
    private val onSeatSelected: (Seat) -> Unit
) : RecyclerView.Adapter<SeatAdapter.SeatViewHolder>() {

    inner class SeatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val viewSeat: View = itemView.findViewById(R.id.viewSeat)

        fun bind(seat: Seat) {
            // --- SỬA LỖI TẠI ĐÂY: KHÔNG chỉnh params.width/height nữa ---
            // Chỉ cần set trạng thái hiển thị thôi

            if (seat.status == SeatStatus.AISLE) {
                // Lối đi: Trong suốt
                viewSeat.setBackgroundColor(Color.TRANSPARENT)

                // Vô hiệu hóa
                viewSeat.isEnabled = false
                viewSeat.setOnClickListener(null) // Xóa sự kiện click
            } else {
                // Ghế thường: Reset lại trạng thái
                viewSeat.isEnabled = true

                when (seat.status) {
                    SeatStatus.AVAILABLE -> viewSeat.setBackgroundResource(R.drawable.bg_seat_available)
                    SeatStatus.SELECTED -> viewSeat.setBackgroundResource(R.drawable.bg_seat_selected)
                    SeatStatus.BOOKED -> viewSeat.setBackgroundResource(R.drawable.bg_seat_booked)
                    else -> {}
                }

                // Gán sự kiện click
                viewSeat.setOnClickListener {
                    if (seat.status == SeatStatus.BOOKED) return@setOnClickListener

                    if (seat.status == SeatStatus.SELECTED) {
                        seat.status = SeatStatus.AVAILABLE
                    } else {
                        seat.status = SeatStatus.SELECTED
                    }
                    notifyItemChanged(adapterPosition)
                    onSeatSelected(seat)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SeatViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_seat, parent, false)
        return SeatViewHolder(view)
    }

    override fun onBindViewHolder(holder: SeatViewHolder, position: Int) {
        holder.bind(seats[position])
    }

    override fun getItemCount(): Int = seats.size
}