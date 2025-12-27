package com.example.movieticketbookingapp.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.movieticketbookingapp.ui.admin.AdminMovieActivity
import com.example.movieticketbookingapp.ui.main.HomeActivity
import com.example.movieticketbookingapp.R
import com.example.movieticketbookingapp.ui.admin.AdminDashboardActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // 1. Đưa biến view lên đây để các hàm bên dưới dùng được
    private lateinit var progressBar: ProgressBar
    private lateinit var btnLogin: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth_login)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Ánh xạ View
        val etUsername = findViewById<TextInputEditText>(R.id.etUsername)
        val etPassword = findViewById<TextInputEditText>(R.id.etPassword)
        val tvGoToSignUp = findViewById<TextView>(R.id.tvGoToSignUp)

        // Khởi tạo 2 biến toàn cục
        btnLogin = findViewById(R.id.btnLogin)
        progressBar = findViewById(R.id.progressBar)

        // Kiểm tra session cũ
        if (auth.currentUser != null) {
            checkRoleAndRedirect(auth.currentUser!!.uid)
            return
        }

        btnLogin.setOnClickListener {
            val usernameInput = etUsername.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (usernameInput.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập Username và Password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // --- BẮT ĐẦU LOGIN ---
            showLoading() // Hiện loading
            loginWithUsername(usernameInput, password)
        }

        tvGoToSignUp.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }
    }

    // Hàm phụ: Hiện loading
    private fun showLoading() {
        progressBar.visibility = View.VISIBLE
        btnLogin.isEnabled = false
        btnLogin.text = "" // Xóa chữ để chỉ hiện vòng xoay (tùy chọn)
    }

    // Hàm phụ: Ẩn loading (Quan trọng: Gọi khi lỗi)
    private fun hideLoading() {
        progressBar.visibility = View.GONE
        btnLogin.isEnabled = true
        btnLogin.text = "LOGIN"
    }

    private fun loginWithUsername(username: String, password: String) {
        db.collection("users")
            .whereEqualTo("username", username)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    // LỖI: Không tìm thấy username
                    hideLoading() // <--- Phải ẩn loading
                    Toast.makeText(this, "Username không tồn tại!", Toast.LENGTH_SHORT).show()
                } else {
                    val email = documents.documents[0].getString("email")
                    if (email != null) {
                        performFirebaseLogin(email, password)
                    } else {
                        // LỖI: Data sai
                        hideLoading() // <--- Phải ẩn loading
                        Toast.makeText(this, "Lỗi dữ liệu tài khoản!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .addOnFailureListener {
                // LỖI: Mạng hoặc Server
                hideLoading() // <--- Phải ẩn loading
                Toast.makeText(this, "Lỗi kết nối: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun performFirebaseLogin(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid
                    if (userId != null) {
                        checkRoleAndRedirect(userId)
                    }
                } else {
                    // LỖI: Sai mật khẩu
                    hideLoading() // <--- Phải ẩn loading
                    Toast.makeText(this, "Sai mật khẩu!", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun checkRoleAndRedirect(userId: String) {
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                // Khi thành công chuyển màn hình thì không cần hideLoading cũng được,
                // vì Activity này sẽ bị đóng hoặc che đi.

                if (document != null && document.exists()) {
                    val role = document.getString("role")
                    val fullName = document.getString("fullName")

                    if (role == "admin") {
                        Toast.makeText(this, "Xin chào Admin: $fullName", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, AdminDashboardActivity::class.java))
                    } else {
                        Toast.makeText(this, "Xin chào $fullName", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, HomeActivity::class.java))
                    }
                    finish()
                } else {
                    hideLoading() // Trường hợp hiếm: có Auth nhưng không có dữ liệu trong Firestore
                }
            }
            .addOnFailureListener {
                hideLoading()
                Toast.makeText(this, "Không lấy được thông tin người dùng", Toast.LENGTH_SHORT).show()
            }
    }
}