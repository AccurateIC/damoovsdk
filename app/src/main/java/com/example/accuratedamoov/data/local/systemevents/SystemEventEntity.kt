package com.example.accuratedamoov.data.local.systemevents

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "SystemEventsTable")
data class SystemEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val device_id: String,
    val user_id: Int,
    val event_message: String,
    val event_type: String?,
    val timestamp: Long,
    val synced: Boolean = false
)