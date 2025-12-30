package com.example.movieticketbookingapp.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class User(
    val username: String,
    val name: String,
    val email: String,
    val password: String,
    val role: String = "user"
) : Parcelable