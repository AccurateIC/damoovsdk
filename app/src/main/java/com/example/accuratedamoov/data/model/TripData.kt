package com.example.accuratedamoov.data.model

data class TripData(
    val UNIQUE_ID: Long,
    val start_date: Long,
    val start_latitude: Double,
    val start_longitude: Double,
    val end_latitude: Double,
    val end_longitude: Double,
    val device_id: String,
    val distance_km: Double
)