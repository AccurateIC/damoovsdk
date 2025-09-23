package com.example.accuratedamoov.data.local.systemevents


import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface SystemEventDao {
    @Insert
    suspend fun insertEvent(event: SystemEventEntity)

    @Query("SELECT * FROM systemeventstable ORDER BY timestamp DESC")
    suspend fun getAllEvents(): List<SystemEventEntity>

    @Query("SELECT * FROM systemeventstable WHERE synced = 0 ORDER BY timestamp ASC")
    suspend fun getPendingEvents(): List<SystemEventEntity>

    @Update
    suspend fun updateEvents(events: List<SystemEventEntity>)
}

