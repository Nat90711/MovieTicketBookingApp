package com.example.movieticketbookingapp.ui.booking

import android.content.Intent
import android.os.Bundle
import android.os.StrictMode
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RadioButton
import androidx.cardview.widget.CardView
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.google.gson.JsonObject
import okhttp3.OkHttpClient
import okhttp3.Request
import androidx.appcompat.app.AppCompatActivity
import com.example.movieticketbookingapp.R
import com.example.movieticketbookingapp.zalopay.CreateOrder
import com.google.android.material.button.MaterialButton
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import vn.zalopay.sdk.Environment
import vn.zalopay.sdk.ZaloPayError
import vn.zalopay.sdk.ZaloPaySDK
import vn.zalopay.sdk.listeners.PayOrderListener
import java.text.DecimalFormat
import com.example.movieticketbookingapp.BuildConfig
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.example.movieticketbookingapp.email.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import okhttp3.ResponseBody

class   PaymentActivity : AppCompatActivity() {

    private val SEPAY_API_TOKEN = "Bearer OQMQ4HYRIELZNFCVTF7ECGGKZE9YJXY6QOOQSPOCJF2SUWNGU29T86PSZUZTMB5L"
    private val MY_BANK_ACCOUNT = "0334174028"
    private val MY_BANK_CODE = "MB"

    private val EMAILJS_SERVICE_ID = "service_9mpu0og"
    private val EMAILJS_TEMPLATE_ID = "template_i97hn0y"
    private val EMAILJS_PUBLIC_KEY = "zZFIyL41HgRsf5pA9"

    private lateinit var cardZalo: CardView
    private lateinit var cardSePay: CardView
    private lateinit var rbZalo: RadioButton
    private lateinit var rbSePay: RadioButton

    private lateinit var qrLayout: LinearLayout
    private lateinit var imgQrCode: ImageView
    private lateinit var tvPaymentContent: TextView
    private lateinit var prgChecking: ProgressBar

    private var isSePaySelected = false // Mặc định là Zalo (false)
    private var paymentCodeString = "" // Mã nội dung chuyển khoản (VD: VE1234)
    private var isCheckingPayment = false // Cờ kiểm soát vòng lặp check tiền
    private val client = OkHttpClient() // Client gọi API
    private lateinit var tvTotalAmount: TextView
    private lateinit var tvOrderInfo: TextView
    private lateinit var btnPayZalo: MaterialButton

    // Firebase
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private var posterUrl: String = ""

    // Dữ liệu vé
    private var totalPrice: Double = 0.0
    private var showtimeId: String = ""
    private var seatIds = ArrayList<String>()
    private var seatNames = ArrayList<String>()
    private var movieTitle: String = ""
    private var cinemaName: String = ""
    private var dateTime: String = ""
    private lateinit var tvCountdown: TextView
    private var expireTimeSeconds: Long = 0
    private var countDownTimer: android.os.CountDownTimer? = null
    private var qrBottomSheetDialog: BottomSheetDialog? = null
    private var roomName: String = ""
    private var foodList = ArrayList<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_booking_payment)

        // --- KHỞI TẠO ZALOPAY (SANDBOX) ---
        // AppID và Key mặc định cho môi trường Sandbox
        val appId = 2553 // AppID mặc định của Sandbox
        ZaloPaySDK.init(appId, Environment.SANDBOX)

        // Cho phép chạy mạng ở Main Thread
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        initViews()
        getIntentData()
        startCountdown()
        displayData()

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        btnPayZalo.setOnClickListener {
            checkSeatsBeforePay()
        }
    }

    // Nhận dữ liệu từ ReviewTicketActivity
    private fun getIntentData() {
//        totalPrice = intent.getDoubleExtra("total_price", 0.0)
        totalPrice = 1000.0
        showtimeId = intent.getStringExtra("showtime_id") ?: ""
        seatIds = intent.getStringArrayListExtra("seat_ids") ?: arrayListOf()
        seatNames = intent.getStringArrayListExtra("seat_names") ?: arrayListOf()
        movieTitle = intent.getStringExtra("movie_title") ?: ""
        cinemaName = intent.getStringExtra("cinema_name") ?: ""
        dateTime = intent.getStringExtra("date_time") ?: ""
        posterUrl = intent.getStringExtra("poster_url") ?: ""
        expireTimeSeconds = intent.getLongExtra("expire_time_seconds", 0)
        roomName = intent.getStringExtra("room_name") ?: ""
        foodList = intent.getStringArrayListExtra("selected_foods") ?: arrayListOf()
    }

    private fun startCountdown() {
        val currentTime = System.currentTimeMillis() / 1000 // Giây
        val timeDiff = expireTimeSeconds - currentTime

        if (timeDiff <= 0) {
            handleTimeout()
            return
        }

        countDownTimer = object : android.os.CountDownTimer(timeDiff * 1000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsRemaining = millisUntilFinished / 1000
                val minutes = secondsRemaining / 60
                val seconds = secondsRemaining % 60
                // Bạn cần thêm TextView tvCountdown vào layout activity_booking_payment.xml
                tvCountdown.text = String.format("Giữ ghế trong: %02d:%02d", minutes, seconds)
            }

            override fun onFinish() {
                tvCountdown.text = "Hết giờ!"
                handleTimeout()
            }
        }.start()
    }

    private fun handleTimeout() {
        Toast.makeText(this, "Hết thời gian giữ ghế!", Toast.LENGTH_LONG).show()
        finish() // Thoát màn hình thanh toán
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
        stopCheckingPayment()
    }

    private fun initViews() {
        tvTotalAmount = findViewById(R.id.tvTotalAmount)
        tvOrderInfo = findViewById(R.id.tvOrderInfo)
        btnPayZalo = findViewById(R.id.btnPayZalo)
        tvCountdown = findViewById(R.id.tvCountdown)

        cardZalo = findViewById(R.id.cardZalo)
        cardSePay = findViewById(R.id.cardSePay)
        rbZalo = findViewById(R.id.rbZalo)
        rbSePay = findViewById(R.id.rbSePay)
        qrLayout = findViewById(R.id.qrLayout)
        imgQrCode = findViewById(R.id.imgQrCode)
        tvPaymentContent = findViewById(R.id.tvPaymentContent)
        prgChecking = findViewById(R.id.prgChecking)

        // Mặc định chọn ZaloPay
        selectPaymentMethod(isSePay = false)

        // Xử lý sự kiện Click chọn phương thức
        cardZalo.setOnClickListener { selectPaymentMethod(false) }
        cardSePay.setOnClickListener { selectPaymentMethod(true) }
    }

    private fun selectPaymentMethod(isSePay: Boolean) {
        isSePaySelected = isSePay

        // Đảo trạng thái RadioButton
        rbZalo.isChecked = !isSePay
        rbSePay.isChecked = isSePay

        // Nếu chọn lại Zalo -> Ẩn QR đi, reset nút Pay
        if (!isSePay) {
            qrLayout.visibility = View.GONE
            stopCheckingPayment() // Dừng check tiền nếu đang chạy
            btnPayZalo.text = "PAY"
            btnPayZalo.visibility = View.VISIBLE
        } else {
            // Nếu chọn SePay -> Nút Pay sẽ dùng để "Lấy mã QR"
            btnPayZalo.text = "GET QR CODE"
            btnPayZalo.visibility = View.VISIBLE
        }
    }

    private fun displayData() {
        val formatter = DecimalFormat("#,### VND")
        tvTotalAmount.text = formatter.format(totalPrice)
        tvOrderInfo.text = "Phim: $movieTitle\nGhế: ${seatNames.joinToString(", ")}"
    }

    // === GỌI ZALOPAY ===
    private fun requestZaloPay() {
        val orderApi = CreateOrder()

        try {
            val amountString = totalPrice.toLong().toString()
            val data = orderApi.createOrder(amountString)

            // === SỬA ĐOẠN NÀY ===
            // Kiểm tra xem data có null không trước khi dùng
            if (data != null) {
                val code = data.getString("return_code")

                if (code == "1") {
                    val token = data.getString("zp_trans_token")

                    ZaloPaySDK.getInstance().payOrder(this, token, "demozpdk://app", object :
                        PayOrderListener {
                        override fun onPaymentSucceeded(transactionId: String, transToken: String, appTransID: String) {
                            saveTicketToFirebase()
                        }

                        override fun onPaymentCanceled(zpTransToken: String, appTransID: String) {
                            Toast.makeText(this@PaymentActivity, "Bạn đã hủy thanh toán", Toast.LENGTH_SHORT).show()
                        }

                        override fun onPaymentError(zaloPayError: ZaloPayError, zpTransToken: String, appTransID: String) {
                            Toast.makeText(this@PaymentActivity, "Lỗi thanh toán: $zaloPayError", Toast.LENGTH_SHORT).show()
                        }
                    })
                } else {
                    Toast.makeText(this, "Lỗi tạo đơn hàng ZaloPay (Code: $code)", Toast.LENGTH_SHORT).show()
                }
            } else {
                // Trường hợp data bị null (Lỗi mạng hoặc server)
                Toast.makeText(this, "Không thể kết nối đến ZaloPay!", Toast.LENGTH_SHORT).show()
            }
            // ====================

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkSeatsBeforePay() {
        val showtimeRef = db.collection("showtimes").document(showtimeId)
        val myUserId = auth.currentUser?.uid

        db.runTransaction { transaction ->
            // 1. Check xem ghế đã bị bán chưa (trong bookedSeats)
            val snapshot = transaction.get(showtimeRef)
            val bookedList = snapshot.get("bookedSeats") as? List<Long> ?: emptyList()
            val mySeatIdsLong = seatIds.map { it.toLong() }

            for (id in mySeatIdsLong) {
                if (bookedList.contains(id)) {
                    throw com.google.firebase.firestore.FirebaseFirestoreException(
                        "Ghế $id đã bị bán!",
                        com.google.firebase.firestore.FirebaseFirestoreException.Code.ABORTED
                    )
                }
            }

            // 2. Check xem ghế có bị người khác giữ khóa (Lock) không
            // (Phòng trường hợp mình hết giờ, ghế nhả ra, người khác nhảy vào lock ngay lập tức)
            val currentTime = com.google.firebase.Timestamp.now().seconds

            for (id in seatIds) {
                val lockDoc = transaction.get(showtimeRef.collection("locks").document(id))
                if (lockDoc.exists()) {
                    val expireAt = lockDoc.getTimestamp("expireAt")?.seconds ?: 0
                    val holderId = lockDoc.getString("userId")

                    // Nếu ghế đang bị khóa bởi NGƯỜI KHÁC và còn hạn
                    if (expireAt > currentTime && holderId != myUserId) {
                        throw com.google.firebase.firestore.FirebaseFirestoreException(
                            "Ghế $id đang được giữ bởi người khác!",
                            com.google.firebase.firestore.FirebaseFirestoreException.Code.ABORTED
                        )
                    }
                }
            }
            null // OK
        }.addOnSuccessListener {
            if (isSePaySelected) {
                showSePayUI() // Hiện QR Code
            } else {
                requestZaloPay() // Gọi Zalo SDK
            }
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Lỗi: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun showSePayUI() {
        // 1. Tạo BottomSheetDialog
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.layout_bottom_sheet_qr, null)
        dialog.setContentView(view)

        // 2. Ánh xạ View trong BottomSheet
        val imgQr = view.findViewById<ImageView>(R.id.imgQrCodeSheet)
        val tvContent = view.findViewById<TextView>(R.id.tvPaymentContentSheet)
        val btnClose = view.findViewById<Button>(R.id.btnCloseSheet)

        // 3. Logic tạo QR
        val uniqueCode = "VE${System.currentTimeMillis() % 1000000}"
        paymentCodeString = uniqueCode
        tvContent.text = "Nội dung CK: $paymentCodeString"

        val qrUrl = "https://img.vietqr.io/image/$MY_BANK_CODE-$MY_BANK_ACCOUNT-compact.png?amount=${totalPrice.toInt()}&addInfo=$paymentCodeString"

        Glide.with(this)
            .load(qrUrl)
            .placeholder(R.drawable.ic_launcher_background)
            .into(imgQr)

        // 4. Xử lý sự kiện đóng
        btnClose.setOnClickListener {
            dialog.dismiss()
        }

        // Khi Dialog đóng -> Dừng check tiền
        dialog.setOnDismissListener {
            stopCheckingPayment()
        }

        qrBottomSheetDialog = dialog
        dialog.show()

        // 5. Bắt đầu check tiền
        startCheckingPayment()
    }

    private fun startCheckingPayment() {
        if (isCheckingPayment) return // Đang check rồi thì thôi
        isCheckingPayment = true

        // Chạy luồng phụ để không đơ màn hình
        Thread {
            while (isCheckingPayment) {
                checkTransactionFromSePay()
                try {
                    Thread.sleep(2000) // Hỏi API mỗi 2 giây
                } catch (e: InterruptedException) {
                    break
                }
            }
        }.start()
    }

    private fun stopCheckingPayment() {
        isCheckingPayment = false
    }

    // Gọi API SePay để xem tiền vào chưa
    private fun checkTransactionFromSePay() {
        try {
            // API lấy danh sách giao dịch
            val request = Request.Builder()
                .url("https://my.sepay.vn/userapi/transactions/list")
                .addHeader("Authorization", SEPAY_API_TOKEN) // Token từ BuildConfig
                .addHeader("Content-Type", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    // Parse JSON
                    val jsonObject = Gson().fromJson(responseBody, JsonObject::class.java)
                    val status = jsonObject.get("status").asInt

                    if (status == 200) {
                        val transactions = jsonObject.getAsJsonArray("transactions")

                        // Duyệt danh sách giao dịch mới nhất
                        for (item in transactions) {
                            val content = item.asJsonObject.get("transaction_content").asString
                            val amountIn = item.asJsonObject.get("amount_in").asString

                            // KIỂM TRA ĐIỀU KIỆN:
                            // 1. Nội dung CK chứa mã code của mình (paymentCodeString)
                            // 2. Số tiền khớp với giá vé (totalPrice)

                            if (content.contains(paymentCodeString)) {
                                runOnUiThread {
                                    stopCheckingPayment()
                                    // Đóng BottomSheet nếu đang mở
                                    qrBottomSheetDialog?.dismiss()

                                    Toast.makeText(this, "Thanh toán thành công!", Toast.LENGTH_SHORT).show()
                                    saveTicketToFirebase()
                                }
                                return
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // === LƯU VÉ ===
    private fun saveTicketToFirebase() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Lỗi xác thực: Chưa đăng nhập!", Toast.LENGTH_SHORT).show()
            return
        }

        // 1. Khởi tạo Batch (Để gom nhóm lệnh ghi và xóa)
        val batch = db.batch()
        val showtimeRef = db.collection("showtimes").document(showtimeId)

        // 2. Cập nhật mảng 'bookedSeats' (Thêm ghế vừa mua vào danh sách đã bán vĩnh viễn)
        val seatIdsLong = seatIds.map { it.toLong() }
        val updateData = hashMapOf(
            "bookedSeats" to FieldValue.arrayUnion(*seatIdsLong.toTypedArray())
        )
        batch.set(showtimeRef, updateData, SetOptions.merge())

        // 3. Xóa các bản ghi trong collection 'locks' (Dọn dẹp rác)
        for (seatId in seatIds) {
            val lockRef = showtimeRef.collection("locks").document(seatId)
            batch.delete(lockRef)
        }

        // 4. Cam kết thực thi (Commit)
        batch.commit()
            .addOnSuccessListener {
                createNewBookingRecord(currentUser.uid)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Lỗi lưu vé vào hệ thống: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun createNewBookingRecord(userId: String) {
        db.collection("users").document(userId).get()
            .addOnCompleteListener { task ->
                var nameToSave = "Khách hàng"
                var emailToSave = ""
                val currentUser = auth.currentUser

                if (task.isSuccessful && task.result != null && task.result!!.exists()) {
                    // Lấy thành công từ DB
                    val userDoc = task.result!!
                    // Ưu tiên field "fullName", nếu không có thì tìm "name"
                    val dbName = userDoc.getString("fullName") ?: userDoc.getString("name")
                    val dbEmail = userDoc.getString("email")

                    // Logic fallback: DB -> Auth -> Mặc định
                    nameToSave = dbName ?: currentUser?.displayName ?: "Khách hàng"
                    emailToSave = dbEmail ?: currentUser?.email ?: ""
                } else {
                    // Nếu lỗi mạng hoặc chưa lưu profile, dùng tạm thông tin từ Auth
                    nameToSave = currentUser?.displayName ?: "Khách hàng"
                    emailToSave = currentUser?.email ?: ""
                }

                // 2. Chuẩn bị dữ liệu Booking
                val orderIdLong = System.currentTimeMillis()
                val orderIdString = orderIdLong.toString()
                val paymentMethodName = if (isSePaySelected) "SePay" else "ZaloPay"

                val durationStr = intent.getStringExtra("duration") ?: ""
                val durationInt = durationStr.toIntOrNull() ?: 0
                val seatTypeStr = intent.getStringExtra("seat_type") ?: ""
                // Lấy danh sách món ăn từ biến toàn cục foodList (đã nhận ở onCreate)
                val foodsStr = foodList.joinToString(", ")

                val bookingData = hashMapOf(
                    "bookingId" to orderIdLong,
                    "userId" to userId,
                    "userName" to nameToSave,       // <--- Tên chuẩn
                    "userEmail" to emailToSave,     // <--- Email chuẩn
                    "movieTitle" to movieTitle,
                    "posterUrl" to posterUrl,
                    "cinema" to cinemaName,
                    "dateTime" to dateTime,
                    "seats" to seatNames,
                    "totalPrice" to totalPrice,
                    "bookingTime" to Timestamp.now(),
                    "paymentMethod" to paymentMethodName,
                    "duration" to durationInt,
                    "roomName" to roomName,
                    "seatType" to seatTypeStr,
                    "foods" to foodsStr,
                    "status" to "paid"              // <--- Trạng thái thanh toán
                )

                // 3. Lưu vé vào Firestore
                db.collection("bookings").document(orderIdString).set(bookingData)
                    .addOnSuccessListener {
                        // Gửi email xác nhận (nếu có email)
                        if (emailToSave.isNotEmpty()) {
                            sendConfirmationEmail(
                                toEmail = emailToSave,
                                toName = nameToSave,
                                bookingId = orderIdString,
                                foodsStr = foodsStr
                            )
                        }

                        // Chuyển sang màn hình Thành Công
                        val intent = Intent(this, PaymentSuccessActivity::class.java)
                        intent.putExtra("movie_title", movieTitle)
                        intent.putExtra("cinema_name", cinemaName)
                        intent.putExtra("date_time", dateTime)
                        intent.putStringArrayListExtra("seat_names", seatNames)
                        intent.putExtra("total_price", totalPrice)
                        intent.putExtra("poster_url", posterUrl)
                        intent.putExtra("booking_id", orderIdString)
                        intent.putExtra("payment_method", paymentMethodName)
                        intent.putStringArrayListExtra("selected_foods", foodList)
                        intent.putExtra("duration", durationStr)
                        intent.putExtra("seat_type", seatTypeStr)
                        intent.putExtra("room_name", roomName)

                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Lỗi lưu vé: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
    }

    private fun sendConfirmationEmail(toEmail: String, toName: String, bookingId: String, foodsStr: String) {
        val formatter = DecimalFormat("#,### VND")

        // Tạo dữ liệu param khớp với Template trên web
        val params = EmailParams(
            to_email = toEmail,
            to_name = toName,
            movie_title = movieTitle,
            cinema_name = cinemaName,
            room_name = roomName,
            date_time = dateTime,
            seat_names = seatNames.joinToString(", "),
            foods = if (foodsStr.isEmpty()) "Không có" else foodsStr,
            total_price = formatter.format(totalPrice),
            booking_id = bookingId
        )

        val request = EmailRequest(
            serviceId = EMAILJS_SERVICE_ID,
            templateId = EMAILJS_TEMPLATE_ID,
            userId = EMAILJS_PUBLIC_KEY,
            templateParams = params
        )

        // Gọi API (Chạy ngầm không chặn UI)
        RetrofitClient.instance.sendEmail(request).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    // Mail đã gửi ok
                    println("Email sent successfully!")
                } else {
                    println("Failed to send email: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                println("Error sending email: ${t.message}")
            }
        })
    }

    // Hàm nhận kết quả khi quay lại từ app ZaloPay
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        ZaloPaySDK.getInstance().onResult(intent)
    }
}