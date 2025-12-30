package com.example.movieticketbookingapp.ui.auth

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.movieticketbookingapp.ui.admin.AdminMovieActivity
import com.example.movieticketbookingapp.ui.main.HomeActivity
import com.example.movieticketbookingapp.R
import com.example.movieticketbookingapp.ui.admin.AdminDashboardActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var progressBar: ProgressBar
    private lateinit var btnLogin: MaterialButton
    private lateinit var btnGoogleLogin: MaterialButton
    private lateinit var googleSignInClient: GoogleSignInClient

    // --- XỬ LÝ KẾT QUẢ TRẢ VỀ TỪ GOOGLE ---
    private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                if (account != null) {
                    firebaseAuthWithGoogle(account.idToken!!)
                }
            } catch (e: ApiException) {
                hideLoading()
                Toast.makeText(this, "Google Sign-In thất bại: ${e.statusCode}", Toast.LENGTH_SHORT).show()
            }
        } else {
            hideLoading()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth_login)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Setup Google Sign In Options
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id)) // Tự động lấy từ google-services.json
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Ánh xạ View
        val etUsername = findViewById<TextInputEditText>(R.id.etUsername)
        val etPassword = findViewById<TextInputEditText>(R.id.etPassword)
        val tvGoToSignUp = findViewById<TextView>(R.id.tvGoToSignUp)

        btnLogin = findViewById(R.id.btnLogin)
        btnGoogleLogin = findViewById(R.id.btnGoogleLogin) // <--- Ánh xạ
        progressBar = findViewById(R.id.progressBar)

        if (auth.currentUser != null) {
            checkRoleAndRedirect(auth.currentUser!!.uid)
            return
        }

        // Sự kiện Login thường
        btnLogin.setOnClickListener {
            val usernameInput = etUsername.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (usernameInput.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập Username và Password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            showLoading()
            loginWithUsername(usernameInput, password)
        }

        // --- SỰ KIỆN LOGIN GOOGLE ---
        btnGoogleLogin.setOnClickListener {
            showLoading()
            val signInIntent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent)
        }

        tvGoToSignUp.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null) {
                        checkGoogleUserInFirestore(user)
                    }
                } else {
                    hideLoading()
                    Toast.makeText(this, "Lỗi xác thực Firebase: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    // Kiểm tra xem User Google này đã có trong Firestore chưa
    // Nếu chưa (lần đầu login) -> Tạo mới Document
    // Nếu rồi -> Chuyển hướng
    private fun checkGoogleUserInFirestore(user: com.google.firebase.auth.FirebaseUser) {
        val userId = user.uid
        val userRef = db.collection("users").document(userId)

        userRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                // Đã có tài khoản -> Chuyển hướng
                checkRoleAndRedirect(userId)
            } else {
                // Lần đầu đăng nhập -> Lưu thông tin vào Firestore
                val newUser = hashMapOf(
                    "id" to userId,
                    "fullName" to (user.displayName ?: "Google User"),
                    "email" to (user.email ?: ""),
                    "role" to "user", // Mặc định là user
                    "username" to (user.email ?: "") // Dùng email làm username tạm
                )

                userRef.set(newUser).addOnSuccessListener {
                    checkRoleAndRedirect(userId)
                }.addOnFailureListener {
                    hideLoading()
                    Toast.makeText(this, "Lỗi tạo dữ liệu người dùng", Toast.LENGTH_SHORT).show()
                }
            }
        }.addOnFailureListener {
            hideLoading()
            Toast.makeText(this, "Lỗi kết nối Server", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showLoading() {
        progressBar.visibility = View.VISIBLE
        btnLogin.isEnabled = false
        btnGoogleLogin.isEnabled = false // Disable nút Google
        btnLogin.text = ""
    }

    private fun hideLoading() {
        progressBar.visibility = View.GONE
        btnLogin.isEnabled = true
        btnGoogleLogin.isEnabled = true // Enable lại
        btnLogin.text = "LOGIN"
    }

    private fun loginWithUsername(username: String, password: String) {
        db.collection("users")
            .whereEqualTo("username", username)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    hideLoading()
                    Toast.makeText(this, "Username không tồn tại!", Toast.LENGTH_SHORT).show()
                } else {
                    val email = documents.documents[0].getString("email")
                    if (email != null) {
                        performFirebaseLogin(email, password)
                    } else {
                        hideLoading()
                        Toast.makeText(this, "Lỗi dữ liệu tài khoản!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .addOnFailureListener {
                hideLoading()
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
                    hideLoading()
                    Toast.makeText(this, "Sai mật khẩu!", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun checkRoleAndRedirect(userId: String) {
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
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
                    hideLoading()
                }
            }
            .addOnFailureListener {
                hideLoading()
                Toast.makeText(this, "Không lấy được thông tin người dùng", Toast.LENGTH_SHORT).show()
            }
    }
}