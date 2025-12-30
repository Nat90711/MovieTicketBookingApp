package com.example.movieticketbookingapp.ui.main

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.movieticketbookingapp.R
import com.example.movieticketbookingapp.ui.admin.AdminDashboardActivity
import com.example.movieticketbookingapp.ui.admin.AdminMovieActivity
import com.example.movieticketbookingapp.ui.auth.LoginActivity
import com.google.firebase.auth.FirebaseAuth

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        supportActionBar?.hide()

        Handler(Looper.getMainLooper()).postDelayed({
            checkLoginAndNavigate()
        }, 2500)
    }

    private fun checkLoginAndNavigate() {
        val currentUser = FirebaseAuth.getInstance().currentUser

        if (currentUser != null) {
            // 1. ĐÃ ĐĂNG NHẬP -> Cần kiểm tra xem là Admin hay User
            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()

            db.collection("users").document(currentUser.uid).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        // Lấy trường role
                        val role = document.getString("role")

                        if (role == "admin") {
                            // -> LÀ ADMIN: Chuyển sang màn hình Admin
                            val intent = Intent(this, AdminDashboardActivity::class.java)
                            startActivity(intent)
                        } else {
                            // -> LÀ USER THƯỜNG: Chuyển sang HomeActivity
                            val intent = Intent(this, HomeActivity::class.java)
                            startActivity(intent)
                        }
                    } else {
                        // Trường hợp lỗi (user đã bị xóa trên DB nhưng Auth vẫn còn), quay về Login
                        val intent = Intent(this, LoginActivity::class.java)
                        startActivity(intent)
                    }
                    finish() // Đóng Splash sau khi đã chuyển trang
                }
                .addOnFailureListener {
                    // Nếu mất mạng hoặc lỗi server, có thể cho về Login
                    val intent = Intent(this, LoginActivity::class.java)
                    startActivity(intent)
                    finish()
                }

        } else {
            // 2. CHƯA ĐĂNG NHẬP -> Vào màn hình Login
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}