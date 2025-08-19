package com.example.accuratedamoov.data.local.addresscache

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface AddressDao {
    @Query("SELECT address FROM address_cache WHERE key = :key LIMIT 1")
    suspend fun getAddress(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAddress(addressEntity: AddressEntity)
}
