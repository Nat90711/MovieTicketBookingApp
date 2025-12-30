package com.example.movieticketbookingapp.ui.admin

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.example.movieticketbookingapp.R
import com.example.movieticketbookingapp.ui.auth.LoginActivity
import com.google.firebase.auth.FirebaseAuth

class AdminDashboardActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)

        // 1. Quản lý Phim
        findViewById<CardView>(R.id.cardManageMovies).setOnClickListener {
            startActivity(Intent(this, AdminMovieActivity::class.java))
        }

        // 2. Quản lý Phòng
        findViewById<CardView>(R.id.cardManageRooms).setOnClickListener {
            startActivity(Intent(this, AdminRoomActivity::class.java))
        }

        // 3. Quản lý Lịch chiếu
        findViewById<CardView>(R.id.cardManageShowtimes).setOnClickListener {
            startActivity(Intent(this, AdminShowtimeActivity::class.java))
        }

        // 4. Doanh thu
        findViewById<CardView>(R.id.cardStats).setOnClickListener {
            startActivity(Intent(this, AdminStatsActivity::class.java))
        }

        // 5. Quản lý Món ăn
        findViewById<CardView>(R.id.cardManageFood).setOnClickListener {
            startActivity(Intent(this, AdminFoodActivity::class.java))
        }

        // 6. Quản lý vé
        findViewById<CardView>(R.id.cardManageBookings).setOnClickListener {
            startActivity(Intent(this, AdminBookingActivity::class.java))
        }

        // 5. Logout - Sửa lại để gọi Dialog xác nhận
        findViewById<ImageView>(R.id.btnLogout).setOnClickListener {
            performLogout()
        }
    }

    // Hàm hiển thị Dialog xác nhận đăng xuất
    private fun performLogout() {
        // Khởi tạo Dialog
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.layout_dialog_logout)

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT
        )

        dialog.setCancelable(false)

        // Ánh xạ nút bấm
        val btnCancel = dialog.findViewById<Button>(R.id.btnCancelLogout)
        val btnConfirm = dialog.findViewById<Button>(R.id.btnConfirmLogout)

        // Xử lý sự kiện
        btnCancel.setOnClickListener {
            dialog.dismiss() // Đóng dialog
        }

        btnConfirm.setOnClickListener {
            dialog.dismiss()

            // Thực hiện đăng xuất Firebase
            FirebaseAuth.getInstance().signOut()

            // Chuyển về màn hình Login
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        dialog.show()
    }
}