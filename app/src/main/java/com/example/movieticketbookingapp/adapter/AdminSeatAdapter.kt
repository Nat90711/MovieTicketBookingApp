package com.example.movieticketbookingapp.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.movieticketbookingapp.R

// Định nghĩa các hằng số trạng thái (để dùng chung)
const val SEAT_TYPE_STANDARD = 0
const val SEAT_TYPE_VIP = 1
const val SEAT_TYPE_DOUBLE = 2
const val SEAT_TYPE_AISLE = -1 // Lối đi (khoảng trống)

class AdminSeatAdapter(
    private val seatTypes: ArrayList<Int>, // List chứa trạng thái ghế (0, 1, -1)
    private val totalCols: Int,
    private val onSeatClick: (Int) -> Unit
) : RecyclerView.Adapter<AdminSeatAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val viewSeat: View = itemView.findViewById(R.id.viewSeat)

        fun bind(type: Int, position: Int) {
            viewSeat.visibility = View.VISIBLE
            viewSeat.alpha = 1.0f

            // Xóa background cũ để tránh chồng chéo
            viewSeat.background = null

            // Lấy LayoutParams để chỉnh margin (như bài trước)
            val params = viewSeat.layoutParams as ViewGroup.MarginLayoutParams
            val marginSize = 4 // Hoặc số bạn muốn

            // Reset margin mặc định
            params.setMargins(marginSize, marginSize, marginSize, marginSize)

            when (type) {
                SEAT_TYPE_STANDARD -> {
                    viewSeat.setBackgroundResource(R.drawable.bg_seat_available)
                    // QUAN TRỌNG: Xóa sạch mọi màu tint cũ (để không bị dính màu vàng/hồng)
                    viewSeat.background.setTintList(null)
                }
                SEAT_TYPE_VIP -> {
                    viewSeat.setBackgroundResource(R.drawable.bg_seat_available)
                    // QUAN TRỌNG: Dùng .mutate() trước khi tô màu để không ảnh hưởng ghế khác
                    viewSeat.background.mutate().setTint(Color.parseColor("#F44336"))
                }
                SEAT_TYPE_AISLE -> {
                    viewSeat.visibility = View.INVISIBLE
                }
                SEAT_TYPE_DOUBLE -> {
                    // Logic ghế đôi dính nhau (như bài trước)
                    determineCoupleSeatBackground(position, params, marginSize)
                    // Vì bg_seat_couple_left/right đã có màu hồng sẵn trong XML
                    // nên thường không cần setTint.
                    // Nhưng nếu bạn muốn chắc chắn, hãy setTintList(null) ở đây luôn.
                    viewSeat.background?.setTintList(null)
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

            // Kiểm tra Chẵn/Lẻ để biết là Trái hay Phải
            if (consecutiveDoublesCount % 2 == 0) {
                // === ĐÂY LÀ NỬA TRÁI ===
                viewSeat.setBackgroundResource(R.drawable.bg_seat_couple_left)

                // Mẹo dính liền: Set margin bên Phải bằng 0 (hoặc số âm nhẹ nếu vẫn hở)
                params.setMargins(stdMargin, stdMargin, 0, stdMargin)

            } else {
                // === ĐÂY LÀ NỬA PHẢI ===
                viewSeat.setBackgroundResource(R.drawable.bg_seat_couple_right)

                // Mẹo dính liền: Set margin bên Trái bằng 0
                params.setMargins(0, stdMargin, stdMargin, stdMargin)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // Dùng lại item_seat.xml cũ của bạn
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_seat, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(seatTypes[position], position)
    }

    override fun getItemCount(): Int = seatTypes.size
}