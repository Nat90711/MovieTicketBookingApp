package com.example.movieticketbookingapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.movieticketbookingapp.R

class AdminBookingAdapter(
    private var list: List<Map<String, Any>>,
    private val onClick: (Map<String, Any>) -> Unit
) : RecyclerView.Adapter<AdminBookingAdapter.ViewHolder>() {

    fun updateData(newList: List<Map<String, Any>>) {
        list = newList
        notifyDataSetChanged()
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvId: TextView = view.findViewById(R.id.tvBookingId)
        val tvMovie: TextView = view.findViewById(R.id.tvMovieTitle)
        val tvSeat: TextView = view.findViewById(R.id.tvSeatInfo)
        val tvPrice: TextView = view.findViewById(R.id.tvPrice)
        val tvCustomer: TextView = view.findViewById(R.id.tvCustomerInfo)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_admin_booking, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]

        holder.tvId.text = "#${item["bookingId"]}"
        holder.tvMovie.text = item["movieTitle"].toString()

        // Xử lý hiển thị ghế và ngày giờ
        val seats = item["seats"] as? List<*>
        val seatStr = seats?.joinToString(", ") ?: ""
        val dateTime = item["dateTime"] ?: ""
        holder.tvSeat.text = "Ghế: $seatStr ($dateTime)"

        // Xử lý giá tiền
        val price = item["totalPrice"].toString()
        val formatter = java.text.DecimalFormat("#,###")
        holder.tvPrice.text = "${formatter.format(price.toDoubleOrNull() ?: 0)} VND"

        // --- XỬ LÝ HIỂN THỊ INFO USER ---
        // Lấy tên và email từ Firestore (nếu có lưu)
        val userName = item["userName"]?.toString() ?: "Khách vãng lai"
        val userContact = item["userEmail"]?.toString() ?: "Không có TT" // Hoặc dùng userPhone

        holder.tvCustomer.text = "KH: $userName | $userContact"
        // ---------------------------------------

        holder.itemView.setOnClickListener { onClick(item) }
    }

    override fun getItemCount() = list.size
}