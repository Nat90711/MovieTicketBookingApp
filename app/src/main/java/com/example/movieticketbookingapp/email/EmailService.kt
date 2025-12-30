package com.example.movieticketbookingapp.email

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface EmailApiService {
    @POST("api/v1.0/email/send")
    fun sendEmail(@Body body: EmailRequest): Call<ResponseBody>
}