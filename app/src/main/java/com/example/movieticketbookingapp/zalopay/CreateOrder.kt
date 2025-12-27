package com.example.movieticketbookingapp.zalopay

import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONObject
import java.util.Date

class CreateOrder {
    private class CreateOrderData(amount: String) {
        var AppId: String = "2553" // AppID Sandbox mặc định
        var Key1: String = "PcY4iZIKFCIdgZvA6ueMcMHHUbRLYjPL" // Key1 Sandbox mặc định
        var AppUser: String = "Android_Demo"
        var AppTime: String = Date().time.toString()
        var Amount: String = amount
        var AppTransId: String = Helpers.getAppTransId()
        var EmbedData: String = "{}"
        var Items: String = "[]"
        var BankCode: String = "zalopayapp"
        var Description: String = "Thanh toan ve phim $Amount"

        // Tạo chữ ký: AppId + | + AppTransId + | + AppUser + | + Amount + | + AppTime + | + EmbedData + | + Items
        val input = "$AppId|$AppTransId|$AppUser|$Amount|$AppTime|$EmbedData|$Items"
        var Mac: String = Helpers.getMac(Key1, input)
    }

    fun createOrder(amount: String): JSONObject? {
        val input = CreateOrderData(amount)
        val formBody: RequestBody = FormBody.Builder()
            .add("app_id", input.AppId)
            .add("app_user", input.AppUser)
            .add("app_time", input.AppTime)
            .add("amount", input.Amount)
            .add("app_trans_id", input.AppTransId)
            .add("embed_data", input.EmbedData)
            .add("item", input.Items)
            .add("bank_code", input.BankCode)
            .add("description", input.Description)
            .add("mac", input.Mac)
            .build()

        val client = OkHttpClient()
        val request = Request.Builder()
            .url("https://sb-openapi.zalopay.vn/v2/create") // URL môi trường Sandbox
            .post(formBody)
            .build()

        return try {
            val response = client.newCall(request).execute()
            JSONObject(response.body?.string())
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}