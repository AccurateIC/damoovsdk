package com.example.accuratedamoov.data.model

data class LoginResponse(
    val success: Boolean,
    val user_id: Int,
    val name: String,
    val token: String,
    val last_login: String?,
    val created_on: String
)