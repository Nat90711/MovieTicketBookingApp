package com.example.movieticketbookingapp.ui.auth

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.movieticketbookingapp.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FirebaseFirestore

class SignUpActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth_signup)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val etName = findViewById<TextInputEditText>(R.id.etName)
        val etUsername = findViewById<TextInputEditText>(R.id.etUsername)
        val etEmail = findViewById<TextInputEditText>(R.id.etEmail)
        val etPassword = findViewById<TextInputEditText>(R.id.etPassword)
        val btnRegister = findViewById<MaterialButton>(R.id.btnRegister)
        val tvGoToLogin = findViewById<TextView>(R.id.tvGoToLogin)
        tvGoToLogin.paintFlags = tvGoToLogin.paintFlags or android.graphics.Paint.UNDERLINE_TEXT_FLAG

        btnRegister.setOnClickListener {
            val name = etName.text.toString().trim()
            val username = etUsername.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (name.isEmpty() || username.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Vui lòng điền đầy đủ thông tin!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // BƯỚC 1: Kiểm tra Username đã tồn tại trong Firestore chưa
            db.collection("users")
                .whereEqualTo("username", username)
                .get()
                .addOnSuccessListener { documents ->
                    if (!documents.isEmpty) {
                        // Nếu tìm thấy document nào có username này -> Đã trùng
                        Toast.makeText(this, "Username này đã được sử dụng!", Toast.LENGTH_SHORT).show()
                    } else {
                        // Username chưa trùng -> Tiến hành tạo tài khoản Auth
                        createFirebaseUser(name, username, email, password)
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Lỗi kiểm tra username: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }

        tvGoToLogin.setOnClickListener { finish() }
    }

    private fun createFirebaseUser(name: String, username: String, email: String, pass: String) {
        // BƯỚC 2: Tạo User trên Firebase Auth (Check trùng Email tự động ở đây)
        auth.createUserWithEmailAndPassword(email, pass)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid

                    // BƯỚC 3: Lưu thông tin chi tiết vào Firestore
                    val userMap = hashMapOf(
                        "uid" to userId,
                        "fullName" to name,      // Tên thật để hiển thị
                        "username" to username,  // Username để đăng nhập
                        "email" to email,        // Email để liên lạc/reset pass
                        "role" to "user"         // Mặc định là user
                    )

                    if (userId != null) {
                        db.collection("users").document(userId)
                            .set(userMap)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Đăng ký thành công!", Toast.LENGTH_SHORT).show()
                                finish()
                            }
                            .addOnFailureListener {
                                Toast.makeText(this, "Lỗi lưu data: ${it.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                } else {
                    if (task.exception is FirebaseAuthUserCollisionException) {
                        Toast.makeText(this, "Email này đã được đăng ký!", Toast.LENGTH_SHORT).show()
                    } else {
                        // Các lỗi khác (Mật khẩu yếu, lỗi mạng...)
                        Toast.makeText(this, "Lỗi: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }

                }
            }
    }
}