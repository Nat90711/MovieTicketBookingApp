package com.example.movieticketbookingapp.ui.booking

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.movieticketbookingapp.ui.main.HomeActivity
import com.example.movieticketbookingapp.R
import com.example.movieticketbookingapp.ui.ticket.TicketDetailActivity
import com.google.android.material.button.MaterialButton

class PaymentSuccessActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_booking_success)

        val btnViewTicket = findViewById<MaterialButton>(R.id.btnViewTicket)
        val btnGoHome = findViewById<MaterialButton>(R.id.btnGoHome)

        // Nút VỀ HOME: Xóa hết các màn hình cũ, về thẳng Home
        btnGoHome.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        // Nút XEM VÉ: Chuyển sang TicketDetailActivity
        // Cần truyền tiếp dữ liệu vé nhận được từ PaymentActivity sang đây
        btnViewTicket.setOnClickListener {
            val intent = Intent(this, TicketDetailActivity::class.java)
            if (getIntent().extras != null) {
                intent.putExtras(getIntent().extras!!)
            }
            startActivity(intent)
        }
    }
}