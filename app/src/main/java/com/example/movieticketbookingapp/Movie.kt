package com.example.movieticketbookingapp

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Movie(
    val id: Int,
    val title: String,
    val posterUrl: String,
    val rating: Double = 9.0,         // Mặc định cho giống thiết kế
    val duration: String = "1h50m",   // Thời lượng
    val genre: String = "Anime",      // Thể loại
    val description: String = "Mô tả phim..."
) : Parcelable