package com.example.accuratedamoov.data.model

data class TripData(
    val unique_id: String,

    // Timestamps as formatted strings (e.g., "2025-08-05 10:49:33")
    val start_date_ist: String,
    val end_date_ist: String,

    // Duration in HH:mm format
    val duration_hh_mm: String,

    // Distance in kilometers
    val distance_km: Double,

    // Coordinates as comma-separated strings ("lat,lng")
    val start_coordinates: String,
    val end_coordinates: String
)

