package com.example.accuratedamoov.data.local.systemevents


import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [SystemEventEntity::class /*, other entities */],
    version = 2,
    exportSchema = false
)
abstract class SystemEventDatabase : RoomDatabase() {

    abstract fun systemEventDao(): SystemEventDao

    companion object {
        @Volatile
        private var INSTANCE: SystemEventDatabase? = null

        fun getInstance(context: Context): SystemEventDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SystemEventDatabase::class.java,
                    "accurate_damoov_events_db"
                )
                    .fallbackToDestructiveMigration()// no parameter
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
