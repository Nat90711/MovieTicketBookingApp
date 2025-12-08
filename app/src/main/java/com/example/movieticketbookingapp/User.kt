package com.example.movieticketbookingapp

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class User(
    val username: String,
    val name: String,
    val email: String,
    val password: String, // Trong thực tế password phải được mã hóa, đây là demo
    val role: String = "user" // "user" hoặc "admin"
) : Parcelable