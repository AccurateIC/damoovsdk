package com.example.accuratedamoov.data.model

data class SafetySummaryResponse(
    val safety_score: Double,
    val trips: Int,
    //val driver_trips: Int,
    val mileage_km: Double,
    val time_driven_minutes: Double,
    val average_speed_kmh: Double,
    val max_speed_kmh: Double,
    val phone_usage_percentage: Double,
    ///val speeding_percentage: Double,
//    val phone_usage_speeding_percentage: Double,
//    val unique_tags_count: Int
)
