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

class FoodAdapter(
    private val foodList: List<Food>,
    private val onQuantityChanged: (Food, Int) -> Unit // Callback trả về món và số lượng hiện tại
) : RecyclerView.Adapter<FoodAdapter.FoodViewHolder>() {

    // Map lưu số lượng từng món: Key=FoodID, Value=Quantity
    private val quantityMap = HashMap<String, Int>()

    inner class FoodViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgFood: ImageView = itemView.findViewById(R.id.imgFood)
        val tvName: TextView = itemView.findViewById(R.id.tvFoodName)
        val tvPrice: TextView = itemView.findViewById(R.id.tvFoodPrice)
        val btnMinus: ImageView = itemView.findViewById(R.id.btnMinus)
        val btnPlus: ImageView = itemView.findViewById(R.id.btnPlus)
        val tvQuantity: TextView = itemView.findViewById(R.id.tvQuantity)

        fun bind(food: Food) {
            tvName.text = food.name
            val formatter = DecimalFormat("#,###")
            tvPrice.text = "${formatter.format(food.price)} đ"

            // Load ảnh (nếu có thư viện Glide)
            if (food.imageUrl.isNotEmpty()) {
                Glide.with(itemView.context).load(food.imageUrl).into(imgFood)
            }

            // Hiển thị số lượng hiện tại
            val currentQty = quantityMap[food.id] ?: 0
            tvQuantity.text = currentQty.toString()

            btnPlus.setOnClickListener {
                val newQty = (quantityMap[food.id] ?: 0) + 1
                quantityMap[food.id] = newQty
                tvQuantity.text = newQty.toString()
                onQuantityChanged(food, newQty)
            }

            btnMinus.setOnClickListener {
                val current = quantityMap[food.id] ?: 0
                if (current > 0) {
                    val newQty = current - 1
                    quantityMap[food.id] = newQty
                    tvQuantity.text = newQty.toString()
                    onQuantityChanged(food, newQty)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FoodViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_food, parent, false)
        return FoodViewHolder(view)
    }

    override fun onBindViewHolder(holder: FoodViewHolder, position: Int) {
        holder.bind(foodList[position])
    }

    override fun getItemCount() = foodList.size

    // Hàm lấy danh sách các món đã chọn để gửi đi
    fun getSelectedFoods(): Map<String, Int> {
        return quantityMap.filter { it.value > 0 }
    }
}