package com.example.movieticketbookingapp.ui.ticket

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.bumptech.glide.Glide
import com.example.movieticketbookingapp.R
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import java.io.File
import java.io.FileOutputStream
import java.text.DecimalFormat

class TicketDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ticket_detail)

        // 1. Nhận dữ liệu từ Intent
        val movieTitle = intent.getStringExtra("movie_title") ?: "Unknown"
        val posterUrl = intent.getStringExtra("poster_url")
        val cinema = intent.getStringExtra("cinema_name") ?: "Cinestar"
        val dateTime = intent.getStringExtra("date_time") ?: "" // Ví dụ: "15:15, 12th Nov..."
        val seats = intent.getStringArrayListExtra("seat_names") ?: arrayListOf()
        val totalPrice = intent.getDoubleExtra("total_price", 0.0)
        val orderId = intent.getStringExtra("booking_id") ?: System.currentTimeMillis().toString()
        val paymentMethod = intent.getStringExtra("payment_method") ?: "ZaloPay"
        val duration = intent.getStringExtra("duration") ?: "N/A"
        val seatType = intent.getStringExtra("seat_type") ?: "Standard"
        val roomName = intent.getStringExtra("room_name") ?: "N/A"

        // --- NHẬN DỮ LIỆU ĐỒ ĂN ---
        val foodsList = intent.getStringArrayListExtra("selected_foods")
        val foodsString = intent.getStringExtra("foods")

        // Tách chuỗi ngày giờ
        val timeParts = dateTime.split(", ")
        val timeShow = if (timeParts.isNotEmpty()) timeParts[0] else ""
        val dateShow = if (timeParts.size > 1) timeParts[1] else ""

        // 2. Ánh xạ View
        val imgPoster: ImageView = findViewById(R.id.imgPoster)
        val imgQRCode: ImageView = findViewById(R.id.imgQRCode)
        val tvFoodInfo: TextView = findViewById(R.id.tvFoodInfo)

        // --- Ánh xạ cho nút Download ---
        val btnDownload: ImageView = findViewById(R.id.btnDownload)
        val cardTicket: CardView = findViewById(R.id.cardTicket)

        findViewById<TextView>(R.id.tvMovieTitle).text = movieTitle
        findViewById<TextView>(R.id.tvCinema).text = cinema
        findViewById<TextView>(R.id.tvRoomName).text = roomName
        findViewById<TextView>(R.id.tvDate).text = dateShow
        findViewById<TextView>(R.id.tvTime).text = timeShow
        findViewById<TextView>(R.id.tvDuration).text = "$duration mins"
        findViewById<TextView>(R.id.tvSeatType).text = seatType
        findViewById<TextView>(R.id.tvTicketCount).text = seats.size.toString()
        findViewById<TextView>(R.id.tvSeatNo).text = seats.joinToString(", ")

        // --- HIỂN THỊ COMBO ---
        if (!foodsList.isNullOrEmpty()) {
            tvFoodInfo.text = foodsList.joinToString(", ")
        } else if (!foodsString.isNullOrEmpty()) {
            tvFoodInfo.text = foodsString
        } else {
            tvFoodInfo.text = "Không chọn"
        }

        findViewById<TextView>(R.id.tvOrderId).text = orderId
        findViewById<TextView>(R.id.tvPaymentMethod).text = paymentMethod

        val formatter = DecimalFormat("#,### VND")
        findViewById<TextView>(R.id.tvTotalPrice).text = formatter.format(totalPrice)

        // Load Poster
        if (posterUrl != null) {
            Glide.with(this).load(posterUrl).centerCrop().into(imgPoster)
        }

        // 3. TẠO MÃ QR CODE
        try {
            val barcodeEncoder = BarcodeEncoder()
            // Nội dung QR chứa ID vé và Tên phim để soát vé
            val qrContent = "TICKET|$orderId|$movieTitle|$seats"
            val bitmap = barcodeEncoder.encodeBitmap(qrContent, BarcodeFormat.QR_CODE, 400, 400)
            imgQRCode.setImageBitmap(bitmap)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Back button
        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        // --- XỬ LÝ DOWNLOAD PDF ---
        btnDownload.setOnClickListener {
            // Tắt nút download tạm thời để tránh spam
            btnDownload.isEnabled = false
            Toast.makeText(this, "Đang tạo file PDF...", Toast.LENGTH_SHORT).show()

            val fileName = "CinemaTicket_${orderId}.pdf"
            generatePdfFromView(cardTicket, fileName)

            // Bật lại sau 2s
            btnDownload.postDelayed({ btnDownload.isEnabled = true }, 2000)
        }
    }

    // --- CÁC HÀM HỖ TRỢ TẠO PDF ---

    private fun getBitmapFromView(view: View): Bitmap {
        // Tạo bitmap với kích thước bằng view
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Vẽ background trắng (nếu view trong suốt sẽ bị đen khi ra PDF)
        val bgDrawable = view.background
        if (bgDrawable != null) {
            bgDrawable.draw(canvas)
        } else {
            canvas.drawColor(Color.WHITE)
        }

        // Vẽ nội dung view lên canvas
        view.draw(canvas)
        return bitmap
    }

    private fun generatePdfFromView(view: View, fileName: String) {
        try {
            // 1. Chụp View thành ảnh
            val bitmap = getBitmapFromView(view)

            // 2. Khởi tạo PDF Document
            val pdfDocument = PdfDocument()

            // Tạo trang PDF với kích thước bằng kích thước View
            val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, 1).create()
            val page = pdfDocument.startPage(pageInfo)

            // 3. Vẽ ảnh vừa chụp vào trang PDF
            val canvas = page.canvas
            canvas.drawBitmap(bitmap, 0f, 0f, null)

            pdfDocument.finishPage(page)

            // 4. Lưu file vào thư mục Downloads
            savePdfToDownloads(pdfDocument, fileName)

            pdfDocument.close()

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Lỗi khi tạo PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun savePdfToDownloads(pdfDocument: PdfDocument, fileName: String) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10 trở lên: Dùng MediaStore
                val contentValues = android.content.ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }

                val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    val outputStream = contentResolver.openOutputStream(uri)
                    if (outputStream != null) {
                        pdfDocument.writeTo(outputStream)
                        outputStream.close()
                        Toast.makeText(this, "Đã lưu vé vào thư mục Download!", Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                // Android 9 trở xuống: Dùng File API truyền thống
                val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val file = File(downloadDir, fileName)
                val outputStream = FileOutputStream(file)
                pdfDocument.writeTo(outputStream)
                outputStream.close()
                Toast.makeText(this, "Đã lưu vé vào thư mục Download!", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Lỗi khi lưu file: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}