package com.example.accuratedamoov.data.model

data class RegisterModel(
    val email: String? = null,        // optional, can be null if phone used
    val phone: String? = null,        // optional, can be null if email used
    val password: String? = null,     // optional, can be null if OTP used
    val otp: String? = null,          // optional, only required if using OTP
    val name: String? = null,
    val device_id: String? = null,    // optional
    val device_name: String? = null
)



