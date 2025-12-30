package com.example.movieticketbookingapp.model

data class CinemaRoom(
    var roomId: String = "",
    var roomName: String = "",
    var type: String = "",
    var totalRows: Int = 0,
    var totalCols: Int = 0,
    var seatConfiguration: List<Int> = emptyList()
) {
    // Hàm tính tổng ghế
    fun getCapacity(): Int {
        return seatConfiguration.count { it != -1 }
    }
}