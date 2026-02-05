package com.example.accuratedamoov.data.model

// LoginRequest.kt
data class LoginRequest(
    val email: String,
    val password: String,
    val device_id: String? = null,    // optional
)


