package com.example.movieticketbookingapp

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class SignUpActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        val etName = findViewById<TextInputEditText>(R.id.etName)
        val etUsername = findViewById<TextInputEditText>(R.id.etUsername)
        val etEmail = findViewById<TextInputEditText>(R.id.etEmail)
        val etPassword = findViewById<TextInputEditText>(R.id.etPassword)
        val btnRegister = findViewById<MaterialButton>(R.id.btnRegister)
        val tvGoToLogin = findViewById<TextView>(R.id.tvGoToLogin)

        btnRegister.setOnClickListener {
            val name = etName.text.toString().trim()
            val username = etUsername.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            // 1. Validate cơ bản
            if (name.isEmpty() || username.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Vui lòng điền đầy đủ thông tin!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 2. Kiểm tra trùng Username
            if (MockUserDatabase.isUsernameTaken(username)) {
                Toast.makeText(this, "Username này đã tồn tại!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 3. Tạo User mới (Mặc định role = "user")
            val newUser = User(username, name, email, password, role = "user")

            // 4. Lưu vào Database ảo
            MockUserDatabase.register(newUser)

            Toast.makeText(this, "Đăng ký thành công! Hãy đăng nhập.", Toast.LENGTH_SHORT).show()

            // 5. Quay về màn hình đăng nhập
            finish()
        }

        val textLogin = "Already have an account? Login"
        val spannableString = android.text.SpannableString(textLogin)

        // 2. Xác định vị trí từ khóa "Login"
        val startIndex = textLogin.indexOf("Login")
        val endIndex = startIndex + "Login".length

        // 3. Tạo kiểu IN ĐẬM
        spannableString.setSpan(
            android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
            startIndex,
            endIndex,
            android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        // 4. Tạo kiểu GẠCH CHÂN
        spannableString.setSpan(
            android.text.style.UnderlineSpan(),
            startIndex,
            endIndex,
            android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        // 5. Gán vào TextView
        tvGoToLogin.text = spannableString

        // Nút quay lại Login
        tvGoToLogin.setOnClickListener {
            finish()
        }
    }
}