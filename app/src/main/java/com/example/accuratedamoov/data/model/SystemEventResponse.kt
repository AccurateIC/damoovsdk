package com.example.accuratedamoov.data.model

data class SystemEventResponse(
    val success: Boolean,
    val message: String,
    val error: String? = null
)
