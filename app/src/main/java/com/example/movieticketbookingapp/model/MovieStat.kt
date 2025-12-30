package com.example.movieticketbookingapp.model

data class MovieStat(
    val movieTitle: String,
    val totalRevenue: Double,
    val ticketCount: Int
) : Comparable<MovieStat> {
    override fun compareTo(other: MovieStat): Int {
        // Sắp xếp giảm dần theo doanh thu
        return other.totalRevenue.compareTo(this.totalRevenue)
    }
}