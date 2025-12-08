package com.example.movieticketbookingapp

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val etUsername = findViewById<TextInputEditText>(R.id.etUsername)
        val etPassword = findViewById<TextInputEditText>(R.id.etPassword)
        val btnLogin = findViewById<MaterialButton>(R.id.btnLogin)
        val tvGoToSignUp = findViewById<TextView>(R.id.tvGoToSignUp)

        val textSignUp = "Don't have an account? Sign up"
        val spannableString = android.text.SpannableString(textSignUp)

        // 2. Xác định vị trí từ khóa "Sign up" để format
        val startIndex = textSignUp.indexOf("Sign up")
        val endIndex = startIndex + "Sign up".length

        // 3. Tạo kiểu IN ĐẬM (Bold)
        spannableString.setSpan(
            android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
            startIndex,
            endIndex,
            android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        // 4. Tạo kiểu GẠCH CHÂN (Underline)
        spannableString.setSpan(
            android.text.style.UnderlineSpan(),
            startIndex,
            endIndex,
            android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        // 5. Gán vào TextView
        tvGoToSignUp.text = spannableString

        // Xử lý đăng nhập
        btnLogin.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Gọi hàm check từ MockDatabase
            val user = MockUserDatabase.login(username, password)

            if (user != null) {
                // Đăng nhập thành công
                if (user.role == "admin") {
                    Toast.makeText(this, "Xin chào Admin ${user.name}!", Toast.LENGTH_SHORT).show()
                    // TODO: Chuyển sang màn hình Admin (làm sau)
                    // Tạm thời vẫn cho vào Home nhưng hiện thông báo Admin
                    val intent = Intent(this, AdminActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, HomeActivity::class.java)
                    intent.putExtra("user_info", user) // Truyền user qua Home
                    startActivity(intent)
                    finish()
                }
            } else {
                Toast.makeText(this, "Sai tài khoản hoặc mật khẩu!", Toast.LENGTH_SHORT).show()
            }
        }

        // Chuyển sang màn hình đăng ký
        tvGoToSignUp.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }
    }
}