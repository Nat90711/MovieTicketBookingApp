package com.example.movieticketbookingapp.ui.booking

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.movieticketbookingapp.R
import com.example.movieticketbookingapp.adapter.FoodAdapter
import com.example.movieticketbookingapp.model.Food
import com.example.movieticketbookingapp.model.Movie
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FirebaseFirestore
import java.text.DecimalFormat

class FoodSelectionActivity : AppCompatActivity() {

    private lateinit var rvFood: RecyclerView
    private lateinit var btnConfirm: MaterialButton
    private lateinit var tvTotalPrice: TextView
    private lateinit var btnBack: ImageView

    private val foodList = ArrayList<Food>()
    private lateinit var adapter: FoodAdapter

    // Dữ liệu nhận từ SeatSelection
    private var ticketPrice: Double = 0.0
    private var currentFoodPrice: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_booking_food_select)

        // 1. Nhận dữ liệu từ màn hình ghế
        ticketPrice = intent.getDoubleExtra("total_price", 0.0)

        initViews()
        loadFoods()
        updateTotalPrice()

        btnConfirm.setOnClickListener {
            goToReviewPage()
        }
        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun initViews() {
        rvFood = findViewById(R.id.rvFood)
        tvTotalPrice = findViewById(R.id.tvTotalPrice)
        btnConfirm = findViewById(R.id.btnConfirmFood)
        rvFood.layoutManager = LinearLayoutManager(this)
        btnBack = findViewById(R.id.btnBack)
    }

    private fun loadFoods() {
        FirebaseFirestore.getInstance().collection("foods")
            .get()
            .addOnSuccessListener { documents ->
                foodList.clear()
                for (doc in documents) {
                    val food = doc.toObject(Food::class.java)
                    food.id = doc.id
                    foodList.add(food)
                }

                // Setup Adapter
                adapter = FoodAdapter(foodList) { food, qty ->
                    // Mỗi khi số lượng thay đổi -> Tính lại tiền
                    calculateFoodPrice()
                }
                rvFood.adapter = adapter
            }
    }

    private fun calculateFoodPrice() {
        val selectedMap = adapter.getSelectedFoods()
        currentFoodPrice = 0.0

        for ((id, qty) in selectedMap) {
            val food = foodList.find { it.id == id }
            if (food != null) {
                currentFoodPrice += (food.price * qty)
            }
        }
        updateTotalPrice()
    }

    private fun updateTotalPrice() {
        val finalTotal = ticketPrice + currentFoodPrice
        val formatter = DecimalFormat("#,###")
        tvTotalPrice.text = "${formatter.format(finalTotal)} VND"
        btnConfirm.text = "TIẾP TỤC (${formatter.format(finalTotal)})"
    }

    private fun goToReviewPage() {
        val intent = Intent(this, ReviewTicketActivity::class.java)

        // 1. Copy toàn bộ dữ liệu cũ từ SeatSelection sang Review
        if (getIntent().extras != null) {
            intent.putExtras(getIntent().extras!!)
        }

        // 2. Cập nhật giá tổng mới (Vé + Nước)
        val finalTotal = ticketPrice + currentFoodPrice
        intent.putExtra("total_price", finalTotal)

        // 3. Đóng gói dữ liệu đồ ăn đã chọn để gửi đi
        val selectedMap = adapter.getSelectedFoods()
        val foodNames = ArrayList<String>()

        for ((id, qty) in selectedMap) {
            val food = foodList.find { it.id == id }
            if (food != null) {
                foodNames.add("${qty}x ${food.name}")
            }
        }

        intent.putStringArrayListExtra("selected_foods", foodNames)
        intent.putExtra("food_price", currentFoodPrice)

        startActivity(intent)
    }
}