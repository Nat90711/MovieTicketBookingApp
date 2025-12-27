package com.example.movieticketbookingapp.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Movie(
    val id: Int = 0,             // Thêm = 0
    val title: String = "",      // Thêm = ""
    val posterUrl: String = "",
    val duration: Int = 120,
    val genre: String = "",
    val description: String = "",
    val director: String = "",
    val cast: List<String> = emptyList(),
    val language: String = "",
    val status: String = "now_showing",
    val hot: Boolean = false
) : Parcelable