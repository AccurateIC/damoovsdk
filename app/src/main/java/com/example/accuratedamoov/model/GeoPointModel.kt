package com.example.accuratedamoov.model

import org.osmdroid.util.GeoPoint

data class GeoPointModel(
    val latitude: Double,
    val longitude: Double
)
fun GeoPointModel.toGeoPoint(): GeoPoint {
    return GeoPoint(latitude, longitude)
}