package com.example.accuratedamoov.data.model

data class SystemEventRequest(
    val device_id: String,
    val user_id: Int,
    val event_message: String,
    val event_type: String?, // optional
    val timestamp: Long
)
