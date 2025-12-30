package com.example.movieticketbookingapp.email

import com.google.gson.annotations.SerializedName

data class EmailRequest(
    @SerializedName("service_id") val serviceId: String,
    @SerializedName("template_id") val templateId: String,
    @SerializedName("user_id") val userId: String, // Đây là Public Key
    @SerializedName("template_params") val templateParams: EmailParams
)

data class EmailParams(
    val to_email: String,
    val to_name: String,
    val movie_title: String,
    val cinema_name: String,
    val room_name: String,
    val date_time: String,
    val seat_names: String,
    val foods: String,
    val total_price: String,
    val booking_id: String
)