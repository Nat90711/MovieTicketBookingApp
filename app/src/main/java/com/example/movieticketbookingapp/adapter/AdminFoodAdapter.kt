package com.example.movieticketbookingapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.movieticketbookingapp.R
import com.example.movieticketbookingapp.model.Food
import java.text.DecimalFormat

class AdminFoodAdapter(
    private val list: List<Food>,
    private val onItemClick: (Food) -> Unit,   // Click vào item để Sửa
    private val onDeleteClick: (Food) -> Unit  // Click vào nút X để Xóa
) : RecyclerView.Adapter<AdminFoodAdapter.FoodViewHolder>() {

    inner class FoodViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgFood: ImageView = itemView.findViewById(R.id.imgFood)
        val tvName: TextView = itemView.findViewById(R.id.tvName)
        val tvPrice: TextView = itemView.findViewById(R.id.tvPrice)
        val btnDelete: ImageView = itemView.findViewById(R.id.btnDelete)

        fun bind(food: Food) {
            tvName.text = food.name
            val formatter = DecimalFormat("#,### đ")
            tvPrice.text = formatter.format(food.price)

            if (food.imageUrl.isNotEmpty()) {
                Glide.with(itemView.context).load(food.imageUrl).centerCrop().into(imgFood)
            }

            // Click vào item -> Sửa
            itemView.setOnClickListener { onItemClick(food) }

            // Click vào nút xóa
            btnDelete.setOnClickListener { onDeleteClick(food) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FoodViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_admin_food, parent, false)
        return FoodViewHolder(view)
    }

    override fun onBindViewHolder(holder: FoodViewHolder, position: Int) {
        holder.bind(list[position])
    }

    override fun getItemCount() = list.size
}