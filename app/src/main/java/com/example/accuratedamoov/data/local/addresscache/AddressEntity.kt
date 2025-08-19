package com.example.accuratedamoov.data.local.addresscache

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "address_cache")
data class AddressEntity(
    @PrimaryKey(autoGenerate = false)
    val key: String, // format: "lat,lon"
    val lat: Double,
    val lon: Double,
    val address: String
)
