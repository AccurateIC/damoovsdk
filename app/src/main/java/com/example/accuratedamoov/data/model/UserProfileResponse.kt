package com.example.accuratedamoov.data.model

data class UserProfileResponse(
    val success: Boolean,
    val data: UserProfile?,
    val error: String?
)
