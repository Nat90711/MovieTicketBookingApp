package com.example.movieticketbookingapp.model

data class Booking(
    var id: String = "", // ID của document Firebase
    val movieTitle: String = "",
    val posterUrl: String = "",
    val cinema: String = "",
    val dateTime: String = "",
    val seats: List<String> = emptyList(),
    val totalPrice: Double = 0.0,
    val paymentMethod: String = "",
    val duration: Int = 120,
    val seatType: String = "",
    val roomName: String = "",
    val foods: String = ""
)
