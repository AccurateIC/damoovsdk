package com.example.accuratedamoov.model

data class TripNotification(
    val id: Int,
    val uniqueId: String,
    val message: String,
    val timestamp: Long,
    val lat: Double?,
    val lng: Double?,
    val isRead: Int
)
