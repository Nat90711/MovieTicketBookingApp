package com.example.movieticketbookingapp.model

data class CinemaRoom(
    var id: String = "",
    var name: String = "",
    var type: String = "",
    var totalRows: Int = 0,
    var totalCols: Int = 0
) {
    // Hàm tính tổng ghế
    fun getCapacity(): Int {
        return totalRows * totalCols
    }
}