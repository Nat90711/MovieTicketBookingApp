package com.example.movieticketbookingapp.ui.main

import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.app.Dialog
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.movieticketbookingapp.R
import com.example.movieticketbookingapp.ui.auth.LoginActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatDelegate
import com.example.movieticketbookingapp.ui.movie.MovieActivity
import com.example.movieticketbookingapp.ui.ticket.MyTicketsActivity

class ProfileActivity : AppCompatActivity() {

    // Views
    private lateinit var imgAvatar: ShapeableImageView
    private lateinit var btnEditProfile: MaterialButton
    private lateinit var etName: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etDob: TextInputEditText
    private lateinit var etPhone: TextInputEditText
    private lateinit var btnLogout: MaterialButton
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var btnSave: MaterialButton
    private lateinit var btnSettings: ImageView

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // Trạng thái Edit
    private var isEditing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_profile)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Không làm gì cả -> Vô hiệu hóa nút Back vật lý
            }
        })

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        initViews()

        // Mặc định khóa các ô nhập liệu khi mới vào
        toggleEditState(false)

        setupBottomNav()
        setupListeners()
        loadUserProfile()
    }

    override fun onResume() {
        super.onResume()
        if (bottomNav.selectedItemId != R.id.nav_profile) {
            bottomNav.selectedItemId = R.id.nav_profile
        }
    }

    private fun initViews() {
        imgAvatar = findViewById(R.id.imgAvatar)
        btnEditProfile = findViewById(R.id.btnEditProfile)
        etName = findViewById(R.id.etName)
        etEmail = findViewById(R.id.etEmail)
        etDob = findViewById(R.id.etDob)
        etPhone = findViewById(R.id.etPhone)
        btnSave = findViewById(R.id.btnSave) // Ánh xạ nút Save
        btnSettings = findViewById(R.id.btnSettings)
        bottomNav = findViewById(R.id.bottomNavigation)
    }

    private fun setupListeners() {
        // 1. Xử lý nút EDIT / SAVE
        btnEditProfile.setOnClickListener {
            if (!isEditing) {
                toggleEditState(true)
            }
        }

        btnSave.setOnClickListener {
            saveUserProfile()
        }

        // 2. Xử lý chọn ngày sinh (Hiện Lịch)
        etDob.setOnClickListener {
            if (isEditing) { // Chỉ cho chọn khi đang ở chế độ sửa
                showDatePicker()
            }
        }

        btnSettings.setOnClickListener { view ->
            showSettingsMenu(view)
        }
    }

    private fun showSettingsMenu(anchorView: View) {
        val view = LayoutInflater.from(this).inflate(R.layout.layout_custom_popup_menu, null)

        val widthInDp = 200f
        val density = resources.displayMetrics.density
        val widthInPx = (widthInDp * density).toInt()

        val popupWindow = PopupWindow(
            view,
            widthInPx,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )

        popupWindow.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        popupWindow.elevation = 10f

        // 1. Ánh xạ các View
        val btnDarkModeItem = view.findViewById<View>(R.id.menuItemDarkMode)
        val btnLogoutItem = view.findViewById<View>(R.id.menuItemLogout)

        val imgDarkMode = view.findViewById<ImageView>(R.id.imgDarkMode)
        val tvDarkMode = view.findViewById<TextView>(R.id.tvDarkMode)

        // 2. KIỂM TRA CHẾ ĐỘ HIỆN TẠI (Quan trọng)
        val currentNightMode = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
        val isDarkMode = currentNightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES

        // 3. Cập nhật Giao diện Menu theo chế độ
        if (isDarkMode) {
            // Đang tối -> Gợi ý chuyển sang sáng
            tvDarkMode.text = "Light Mode"
            imgDarkMode.setImageResource(R.drawable.ic_sun) // Nhớ tạo icon ic_sun
        } else {
            // Đang sáng -> Gợi ý chuyển sang tối
            tvDarkMode.text = "Dark Mode"
            imgDarkMode.setImageResource(R.drawable.ic_moon)
        }

        // 4. Bắt sự kiện Click
        btnDarkModeItem.setOnClickListener {
            toggleDarkMode()
            popupWindow.dismiss()
        }

        btnLogoutItem.setOnClickListener {
            popupWindow.dismiss()
            performLogout()
        }

        popupWindow.showAsDropDown(anchorView, -100, 0)
    }

    private fun toggleDarkMode() {
        // 1. Kiểm tra chế độ hiện tại
        val currentNightMode = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
        val isDarkMode = currentNightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES

        // 2. Xác định chế độ MỚI (Ngược lại với hiện tại)
        val newMode = if (isDarkMode) {
            AppCompatDelegate.MODE_NIGHT_NO // Chuyển sang Sáng
        } else {
            AppCompatDelegate.MODE_NIGHT_YES // Chuyển sang Tối
        }

        // 3. Áp dụng chế độ mới
        AppCompatDelegate.setDefaultNightMode(newMode)

        // 4. LƯU VÀO SHAREDPREFERENCES (Quan trọng)
        val sharedPreferences = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        // Nếu newMode là YES -> Lưu true, ngược lại lưu false
        editor.putBoolean("is_dark_mode", newMode == AppCompatDelegate.MODE_NIGHT_YES)
        editor.apply() // Lưu bất đồng bộ

        // Toast thông báo
        val message = if (newMode == AppCompatDelegate.MODE_NIGHT_YES) "Dark Mode Enabled" else "Light Mode Enabled"
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun performLogout() {
        // 1. Khởi tạo Dialog
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.layout_dialog_logout)

        // 2. Setup Background trong suốt (QUAN TRỌNG để thấy bo góc)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        // Setup kích thước (90% chiều rộng màn hình cho đẹp)
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT
        )

        // Không cho bấm ra ngoài để tắt (tùy chọn)
        dialog.setCancelable(false)

        // 3. Ánh xạ nút bấm
        val btnCancel = dialog.findViewById<Button>(R.id.btnCancelLogout)
        val btnConfirm = dialog.findViewById<Button>(R.id.btnConfirmLogout)

        // 4. Xử lý sự kiện
        btnCancel.setOnClickListener {
            dialog.dismiss() // Đóng dialog
        }

        btnConfirm.setOnClickListener {
            dialog.dismiss()
            // Thực hiện đăng xuất
            auth.signOut()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        dialog.show()
    }

    // Hàm bật/tắt chế độ sửa
    private fun toggleEditState(enable: Boolean) {
        isEditing = enable

        // Mở/Khóa các ô nhập liệu
        etName.isEnabled = enable
        etPhone.isEnabled = enable
        etDob.isEnabled = enable
        etDob.isClickable = enable

        // Email luôn khóa
        etEmail.isEnabled = false

        // Ẩn/Hiện nút Save
        if (enable) {
            btnSave.visibility = View.VISIBLE
            etName.requestFocus()
            // Nút Edit có thể mờ đi hoặc giữ nguyên, tùy bạn
        } else {
            btnSave.visibility = View.GONE
        }
    }

    // Hàm hiển thị Lịch chọn ngày
    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog =
            DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
                // Format ngày thành DD/MM/YYYY
                val formattedDate = "$selectedDay/${selectedMonth + 1}/$selectedYear"
                etDob.setText(formattedDate)
            }, year, month, day)

        datePickerDialog.show()
    }

    // Hàm lưu dữ liệu lên Firebase Firestore
    private fun saveUserProfile() {
        val userId = auth.currentUser?.uid ?: return

        val newName = etName.text.toString().trim()
        val newPhone = etPhone.text.toString().trim()
        val newDob = etDob.text.toString().trim()

        if (newName.isEmpty()) {
            etName.error = "Name cannot be empty"
            return
        }

        // Tạo map dữ liệu để update
        val userUpdates = hashMapOf<String, Any>(
            "fullName" to newName,
            "phoneNumber" to newPhone,
            "dob" to newDob
        )

        // Update lên Firestore
        db.collection("users").document(userId)
            .update(userUpdates)
            .addOnSuccessListener {
                Toast.makeText(this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show()
                toggleEditState(false) // Quay về chế độ xem
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Lỗi cập nhật: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadUserProfile() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            etEmail.setText(currentUser.email)

            db.collection("users").document(currentUser.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val name = document.getString("fullName") ?: ""
                        val phone = document.getString("phoneNumber") ?: ""
                        val dob = document.getString("dob") ?: ""
                        val avatarUrl = document.getString("avatarUrl") ?: ""

                        etName.setText(name)
                        etPhone.setText(phone)
                        etDob.setText(dob)

                        if (avatarUrl.isNotEmpty()) {
                            Glide.with(this)
                                .load(avatarUrl)
                                .placeholder(R.drawable.ic_launcher_background)
                                .error(R.drawable.ic_launcher_background)
                                .into(imgAvatar)
                        }
                    }
                }
        }
    }

    private fun setupBottomNav() {
        bottomNav.selectedItemId = R.id.nav_profile
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, HomeActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.nav_movie -> {
                    startActivity(Intent(this, MovieActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.nav_cart -> {
                    startActivity(Intent(this, MyTicketsActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.nav_profile -> true
                else -> false
            }
        }
    }
}