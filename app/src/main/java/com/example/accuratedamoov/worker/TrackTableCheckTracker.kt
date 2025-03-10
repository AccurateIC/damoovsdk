package com.example.accuratedamoov.worker

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.accuratedamoov.data.network.RetrofitClient
import com.example.accuratedamoov.data.network.SyncRequest
import com.example.accuratedamoov.database.DatabaseHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import org.json.JSONObject

class TrackTableCheckWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val dbHelper = DatabaseHelper(applicationContext)
            val tableNames = listOf(
                "LastKnownPointTable", "EventsStartPointTable", "EventsTable",
                "EventsStopPointTable", "RangeDirectTable", "RangeLateralTable",
                "RangeVerticalTable", "RangeAccuracyTable", "RangeSpeedTable", "TrackTable"
            )

            // Ensure each table has the 'synced' column
            for (table in tableNames) {
                dbHelper.addSyncedColumnIfNotExists(table)
            }

            val jsonData = JSONObject()
            var hasDataToSync = false

            for (table in tableNames) {
                val rowCount = dbHelper.getRowCount(table)
                if (rowCount > 0) {
                    jsonData.put(table, dbHelper.getTableData(table))
                    hasDataToSync = true
                }
            }

            if (!hasDataToSync) {
                Log.d("WorkManager", "No data found in any table, skipping sync.")
                return Result.success()
            }

            val success = withContext(Dispatchers.IO) { syncData(jsonData) }

            if (success) {
                Log.d("OmkarWorkManager", "✅ Data sync successful, marked records as synced.")
                Result.success()
            } else {
                Log.e("OmkarWorkManager", "❌ Data sync failed, will retry.")
                Result.retry()
            }

        } catch (e: Exception) {
            Log.e("OmkarWorkManager", "❌ Error in Worker", e)
            Result.failure()
        }
    }




    private suspend fun syncData(jsonData: JSONObject): Boolean {
        val apiService = RetrofitClient.apiService
        var allSuccessful = true

        for (tableName in jsonData.keys()) {
            val tableData = jsonData.getJSONArray(tableName)
            val dataList = mutableListOf<Map<String, Any>>()
            val syncedIds = mutableListOf<Int>() // Store successfully synced record IDs

            for (i in 0 until tableData.length()) {
                val item = tableData.getJSONObject(i)
                val map = mutableMapOf<String, Any>()
                item.keys().forEach { key -> map[key] = item.get(key) }
                dataList.add(map)

                // Capture primary key ID for marking records as synced
                if (item.has("id")) {
                    syncedIds.add(item.getInt("id"))
                } else if (item.has("ID")) {
                    syncedIds.add(item.getInt("ID"))
                }

            }

            val request = SyncRequest(dataList)

            try {
                val response = withContext(Dispatchers.IO) {
                    apiService.syncData(tableName.replace("_", "-"), request).execute()
                }

                if (response.isSuccessful) {
                    withContext(Dispatchers.IO) {
                        markRecordsAsSynced(applicationContext, tableName, syncedIds)
                    }
                    Log.d("OmkarWorkManager", "✅ Sync successful for $tableName")
                } else {
                    Log.e("OmkarWorkManager", "❌ Sync failed for $tableName: ${response.errorBody()?.string()}")
                    allSuccessful = false
                }
            } catch (e: Exception) {
                Log.e("OmkarWorkManager", "❌ Sync error for $tableName: ${e.message}")
                allSuccessful = false
            }
        }

        return allSuccessful
    }


    /*private fun emptyTable(context: Context, tableName: String) {
        val dbHelper = DatabaseHelper(context)
        val db = SQLiteDatabase.openDatabase(dbHelper.dbPath, null, SQLiteDatabase.OPEN_READWRITE)

        db.beginTransaction()
        try {
            if(tableName != "TrackTable") {
                db.execSQL("DELETE FROM $tableName") // ✅ Clears all records
                db.setTransactionSuccessful()
            }
            Log.d("OmkarWorkManager", "Emptied table: $tableName")
        } catch (e: Exception) {
            Log.e("OmkarWorkManager", "❌ Failed to empty table $tableName: ${e.message}")
        } finally {
            db.endTransaction()
        }
    }*/


    private fun markRecordsAsSynced(context: Context, tableName: String, ids: List<Int>) {
        if (ids.isEmpty()) return

        val dbHelper = DatabaseHelper(context)
        val db = SQLiteDatabase.openDatabase(dbHelper.dbPath, null, SQLiteDatabase.OPEN_READWRITE)

        db.beginTransaction()
        try {
            val idList = ids.joinToString(",") // Convert list to SQL IN clause format
            db.execSQL("UPDATE $tableName SET synced = 1 WHERE id IN ($idList)")
            db.setTransactionSuccessful()
            Log.d("OmkarWorkManager", "✅ Marked records as synced in $tableName")
        } catch (e: Exception) {
            Log.e("OmkarWorkManager", "❌ Failed to mark records as synced in $tableName: ${e.message}")
        } finally {
            db.endTransaction()
        }
    }

}

