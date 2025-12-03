package com.example.accuratedamoov.data.model

data class RegisterResponse(
    val success: Boolean,
    val message: String?,             // backend returns message on success
    val user_id: Int?,
    val device_id: String?,
    val is_email_verified: Int?,      // 1 if verified, 0 if not
    val is_phone_verified: Int?,      // 1 if verified, 0 if not
    val error: String?                // error message if any
)