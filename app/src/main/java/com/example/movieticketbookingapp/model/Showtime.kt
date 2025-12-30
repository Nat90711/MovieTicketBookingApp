package com.example.movieticketbookingapp.model

data class Showtime(
    var id: String = "",
    var movieId: Any? = null,
    var movieTitle: String = "",
    var posterUrl: String = "",
    var roomId: String = "",
    var roomName: String = "",
    var date: String = "",
    var time: String = "",
    var price: Double = 0.0,
    var bookedSeats: List<Long> = listOf()
)