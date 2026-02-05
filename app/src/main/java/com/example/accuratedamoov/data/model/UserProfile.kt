package com.example.accuratedamoov.data.model

data class UserProfile(
    val id: Int,
    val email: String?,
    val name: String?,
    val phone: String?,
    val created_on: String?,
    val last_login: String?,
    val is_email_verified: Boolean,
    val is_phone_verified: Boolean
)
