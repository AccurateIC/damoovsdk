package com.example.accuratedamoov.data.model

data class RegisterResponse(
    val success: Boolean,
    val user_id: Int?,
    val device_id: String?,
    val error: String?
)
