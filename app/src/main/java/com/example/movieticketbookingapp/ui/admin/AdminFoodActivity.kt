package com.example.movieticketbookingapp.ui.admin

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.movieticketbookingapp.R
import com.example.movieticketbookingapp.adapter.AdminFoodAdapter
import com.example.movieticketbookingapp.model.Food
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore

class AdminFoodActivity : AppCompatActivity() {

    private lateinit var rvFood: RecyclerView
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var db: FirebaseFirestore
    private val foodList = ArrayList<Food>()
    private lateinit var adapter: AdminFoodAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_food_list)

        db = FirebaseFirestore.getInstance()
        rvFood = findViewById(R.id.rvFood)
        fabAdd = findViewById(R.id.fabAddFood)

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        setupRecyclerView()
        loadFoods()

        // Thêm món mới
        fabAdd.setOnClickListener {
            showFoodDialog(null) // null nghĩa là chế độ thêm mới
        }
    }

    private fun setupRecyclerView() {
        adapter = AdminFoodAdapter(foodList,
            onItemClick = { food ->
                showFoodDialog(food) // Truyền food vào nghĩa là chế độ Sửa
            },
            onDeleteClick = { food ->
                showDeleteConfirm(food)
            }
        )
        rvFood.layoutManager = LinearLayoutManager(this)
        rvFood.adapter = adapter
    }

    private fun loadFoods() {
        db.collection("foods").get()
            .addOnSuccessListener { documents ->
                foodList.clear()
                for (doc in documents) {
                    val food = doc.toObject(Food::class.java)
                    food.id = doc.id
                    foodList.add(food)
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Lỗi tải dữ liệu: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Hiển thị dialog Thêm hoặc Sửa
    private fun showFoodDialog(food: Food?) {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_add_food, null)
        val tvTitle = view.findViewById<TextView>(R.id.tvDialogTitle)
        val etName = view.findViewById<EditText>(R.id.etFoodName)
        val etPrice = view.findViewById<EditText>(R.id.etFoodPrice)
        val etImage = view.findViewById<EditText>(R.id.etFoodImage)
        val btnSave = view.findViewById<Button>(R.id.btnSave)

        // Nếu là sửa -> Điền dữ liệu cũ vào
        if (food != null) {
            tvTitle.text = "Cập nhật món"
            etName.setText(food.name)
            etPrice.setText(food.price.toInt().toString())
            etImage.setText(food.imageUrl)
        } else {
            tvTitle.text = "Thêm món mới"
        }

        val dialog = AlertDialog.Builder(this)
            .setView(view)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent) // Bo góc đẹp
        dialog.show()

        btnSave.setOnClickListener {
            val name = etName.text.toString().trim()
            val priceStr = etPrice.text.toString().trim()
            val imageUrl = etImage.text.toString().trim()

            if (name.isEmpty() || priceStr.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập tên và giá!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val price = priceStr.toDoubleOrNull() ?: 0.0

            val foodData = hashMapOf(
                "name" to name,
                "price" to price,
                "imageUrl" to imageUrl
            )

            if (food == null) {
                // --- THÊM MỚI ---
                db.collection("foods").add(foodData)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Thêm thành công!", Toast.LENGTH_SHORT).show()
                        loadFoods()
                        dialog.dismiss()
                    }
            } else {
                // --- CẬP NHẬT ---
                db.collection("foods").document(food.id).update(foodData as Map<String, Any>)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show()
                        loadFoods()
                        dialog.dismiss()
                    }
            }
        }
    }

    private fun showDeleteConfirm(food: Food) {
        AlertDialog.Builder(this)
            .setTitle("Xóa món ăn")
            .setMessage("Bạn có chắc muốn xóa '${food.name}' không?")
            .setPositiveButton("Xóa") { _, _ ->
                db.collection("foods").document(food.id).delete()
                    .addOnSuccessListener {
                        Toast.makeText(this, "Đã xóa!", Toast.LENGTH_SHORT).show()
                        loadFoods()
                    }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }
}