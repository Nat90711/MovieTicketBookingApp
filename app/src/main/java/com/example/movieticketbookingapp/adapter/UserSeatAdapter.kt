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

    // Helper kiểm tra ghế đôi đầu (Trái) hay đuôi (Phải)
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

        fun bind(seat: Seat, position: Int) {
            // 1. Reset trạng thái visual
            viewSeat.visibility = View.VISIBLE
            viewSeat.alpha = 1.0f
            viewSeat.background = null
            viewSeat.backgroundTintList = null // Xóa tint cũ

            // 2. Setup LayoutParams (Margin) cho ghế đôi dính nhau
            val params = viewSeat.layoutParams as ViewGroup.MarginLayoutParams
            val marginSize = 4 // Khoảng cách tiêu chuẩn
            params.setMargins(marginSize, marginSize, marginSize, marginSize) // Reset

            // 3. Xử lý hiển thị theo Loại ghế & Trạng thái
            if (seat.status == SeatStatus.AISLE || seat.type == -1) {
                viewSeat.visibility = View.INVISIBLE
                return
            }

            // --- A. XÁC ĐỊNH HÌNH DÁNG (SHAPE) ---
            var bgResId = R.drawable.bg_seat_available // Mặc định bo tròn 4 góc

            if (seat.type == 2) { // Ghế đôi
                if (isLeftSeatOfCouple(position, seat.type)) {
                    bgResId = R.drawable.bg_seat_couple_left
                    params.setMargins(marginSize, marginSize, 0, marginSize) // Dính phải
                } else {
                    bgResId = R.drawable.bg_seat_couple_right
                    params.setMargins(0, marginSize, marginSize, marginSize) // Dính trái
                }
            }

            viewSeat.setBackgroundResource(bgResId)
            viewSeat.layoutParams = params

            // --- B. XÁC ĐỊNH MÀU SẮC (COLOR) THEO TRẠNG THÁI ---
            val color = when (seat.status) {
                SeatStatus.BOOKED -> Color.parseColor("#888888") // Xám
                SeatStatus.HELD -> Color.parseColor("#FFC107")   // Vàng
                SeatStatus.SELECTED -> Color.parseColor("#4A90E2") // Xanh lam
                SeatStatus.AVAILABLE -> {
                    // Nếu còn trống thì tô màu theo loại ghế
                    when (seat.type) {
                        1 -> Color.parseColor("#F44336") // VIP
                        2 -> Color.parseColor("#E91E63") // Couple
                        else -> Color.parseColor("#E0E0E0")             // Thường
                    }
                }
                else -> Color.WHITE
            }

            viewSeat.background.setTint(color)

            // Sự kiện click
            var isEnable = true

            // Nếu ghế đã có người đặt/giữ -> Disable
            if (seat.status == SeatStatus.BOOKED || seat.status == SeatStatus.HELD) {
                isEnable = false
            } else {
                // Logic lọc theo chế độ vé
                if (isCoupleMode) {
                    // Nếu mua Vé Đôi -> Chỉ cho chọn ghế Type 2 (Couple)
                    if (seat.type != 2) isEnable = false
                } else {
                    // Nếu mua Vé Đơn -> Chỉ cho chọn ghế Type 0, 1 (Thường, VIP)
                    // Không cho chọn ghế Type 2
                    if (seat.type == 2) isEnable = false
                }
            }

            if (seat.status == SeatStatus.BOOKED || seat.status == SeatStatus.HELD) {
                viewSeat.alpha = 1.0f
                itemView.setOnClickListener {
                }
            }
            // 2. TRƯỜNG HỢP: GHẾ CÒN TRỐNG (AVAILABLE)
            else {
                // Kiểm tra xem ghế có phù hợp với loại vé đang chọn không
                var isCompatible = true
                if (isCoupleMode) {
                    if (seat.type != 2) isCompatible = false // Mua vé đôi mà chọn ghế đơn -> Sai
                } else {
                    if (seat.type == 2) isCompatible = false // Mua vé đơn mà chọn ghế đôi -> Sai
                }

                if (isCompatible) {
                    // --- HỢP LỆ ---
                    viewSeat.alpha = 1.0f // Sáng rõ
                    itemView.setOnClickListener { onSeatClick(seat) }
                } else {
                    // --- KHÔNG HỢP LỆ (Sai loại vé) ---
                    viewSeat.alpha = 0.3f // Làm mờ đi để user biết không được chọn
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