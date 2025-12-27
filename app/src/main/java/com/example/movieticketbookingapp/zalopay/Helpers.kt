package com.example.movieticketbookingapp.zalopay

import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.random.Random // Import thêm Random

object Helpers {
    // Xóa biến transIdDefault cũ đi, không cần nữa

    fun getAppTransId(): String {
        // Tạo format ngày: yyMMdd (Ví dụ: 251215)
        val format = SimpleDateFormat("yyMMdd", Locale.getDefault())
        val timeString = format.format(Date())

        // Tạo số ngẫu nhiên 6 chữ số (từ 100000 đến 999999) để không bao giờ trùng
        val randomId = Random.nextInt(100000, 999999)

        // Format chuẩn: yyMMdd_AppId_Random
        // Ví dụ: 251215_2553_837291
        return String.format("%s_2553_%s", timeString, randomId)
    }

    // ... (Hàm getMac và bytesToHex giữ nguyên)
    fun getMac(key: String, data: String): String {
        // ... giữ nguyên ...
        return try {
            val sha256Hmac = Mac.getInstance("HmacSHA256")
            val secretKey = SecretKeySpec(key.toByteArray(), "HmacSHA256")
            sha256Hmac.init(secretKey)
            val bytes = sha256Hmac.doFinal(data.toByteArray())
            bytesToHex(bytes)
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val sb = StringBuilder()
        for (b in bytes) {
            sb.append(String.format("%02x", b))
        }
        return sb.toString()
    }
}