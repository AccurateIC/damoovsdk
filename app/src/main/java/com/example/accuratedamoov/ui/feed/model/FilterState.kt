package com.example.accuratedamoov.ui.feed.model


import java.util.*

data class FilterState(
    val startDate: Date? = null,
    val endDate: Date? = null,
    val minDistance: Float? = null,
    val maxDistance: Float? = null,
    val minDurationMinutes: Int? = null,
    val maxDurationMinutes: Int? = null
)
