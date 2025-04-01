package com.example.accuratedamoov.worker

import android.content.Context
import android.util.Log
import androidx.work.*
import com.example.accuratedamoov.data.network.RetrofitClient
import com.example.accuratedamoov.data.network.SyncRequest
import com.example.accuratedamoov.database.DatabaseHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class TrackTableCheckWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val dbHelper = DatabaseHelper.getInstance(applicationContext)
        return try {
            val tableNames = listOf(
                "LastKnownPointTable", "EventsStartPointTable", "EventsTable",
                "EventsStopPointTable", "RangeDirectTable", "RangeLateralTable",
                "RangeVerticalTable", "RangeAccuracyTable", "RangeSpeedTable", "TrackTable"
            )

            // Ensure required columns exist
            for (table in tableNames) {
                dbHelper.addSyncedColumnIfNotExists(table)
                dbHelper.addDeviceIdColumnIfNotExists(table, applicationContext)
            }

            val jsonData = JSONObject()
            var hasDataToSync = false

            for (table in tableNames) {
                val tableData = dbHelper.getUnsyncedTableData(table)
                if (tableData.length() > 0) {
                    jsonData.put(table, tableData)
                    hasDataToSync = true
                }
            }

            val sharedPreferences = applicationContext.getSharedPreferences("appSettings", Context.MODE_PRIVATE)
            val syncInterval = sharedPreferences.getInt("sync_interval", 10).toLong()

            if (!hasDataToSync) {
                Log.d("WorkManager", "✅ No new data, rescheduling worker.")
                scheduleWorker(syncInterval)
                return Result.success()
            }

            val success = withContext(Dispatchers.IO) { syncData(dbHelper, jsonData) }

            dbHelper.closeDatabase() // ✅ Ensures DB is closed after syncing

            if (success) {
                Log.d("WorkManager", "✅ Data sync successful.")
                scheduleWorker(syncInterval)
                Result.success()
            } else {
                Log.e("WorkManager", "❌ Data sync failed, retrying.")
                scheduleWorker(syncInterval)
                Result.retry()
            }

        } catch (e: Exception) {
            Log.e("WorkManager", "❌ Error in Worker", e)
            dbHelper.closeDatabase() // ✅ Ensures DB is closed on error
            Result.failure()
        }
    }

    private suspend fun syncData(dbHelper: DatabaseHelper, jsonData: JSONObject): Boolean {
        val apiService = RetrofitClient.getApiService(applicationContext)
        var allSuccessful = true

        for (tableName in jsonData.keys()) {
            val tableData = jsonData.getJSONArray(tableName)
            val dataList = mutableListOf<Map<String, Any>>()
            val syncedIds = mutableListOf<Int>()

            for (i in 0 until tableData.length()) {
                val item = tableData.getJSONObject(i)
                val map = mutableMapOf<String, Any>()

                var isValidRecord = true
                item.keys().forEach { key ->
                    val value = item.get(key)
                    if (value == JSONObject.NULL || (value is String && value.isBlank())) {
                        isValidRecord = false
                    } else if (!key.equals("id", ignoreCase = true)) {
                        map[key] = value
                    }
                }

                if (isValidRecord) {
                    dataList.add(map)
                    if (item.has("id")) {
                        syncedIds.add(item.getInt("id"))
                    } else if (item.has("ID")) {
                        syncedIds.add(item.getInt("ID"))
                    }
                }
            }

            if (dataList.isEmpty()) {
                Log.d("WorkManager", "⚠️ No valid records for $tableName, skipping sync.")
                continue
            }

            val request = SyncRequest(dataList)

            try {
                val response = withContext(Dispatchers.IO) {
                    apiService.syncData(tableName.replace("_", "-"), request).execute()
                }

                if (response.isSuccessful) {
                    withContext(Dispatchers.IO) {
                        deleteSyncedRecords(dbHelper, tableName, syncedIds)
                    }
                    Log.d("WorkManager", "✅ Synced $tableName, records deleted.")
                } else {
                    Log.e("WorkManager", "❌ Sync failed for $tableName: ${response.errorBody()?.string()}")
                    allSuccessful = false
                }
            } catch (e: Exception) {
                Log.e("WorkManager", "❌ Sync error for $tableName: ${e.message}")
                allSuccessful = false
            }
        }

        return allSuccessful
    }

    private fun deleteSyncedRecords(dbHelper: DatabaseHelper, tableName: String, ids: List<Int>) {
        if (ids.isEmpty()) return

        val db = dbHelper.openDatabase() ?: return

        db.beginTransaction()
        try {
            val idList = ids.joinToString(",")
            db.execSQL("DELETE FROM $tableName WHERE id IN ($idList)")
            db.setTransactionSuccessful()
            Log.d("WorkManager", "🗑️ Deleted synced records from $tableName")
        } catch (e: Exception) {
            Log.e("WorkManager", "❌ Failed to delete records from $tableName: ${e.message}")
        } finally {
            db.endTransaction()
        }
    }

    private fun scheduleWorker(syncInterval: Long) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<TrackTableCheckWorker>()
            .setConstraints(constraints)
            .setInitialDelay(syncInterval, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(applicationContext).enqueueUniqueWork(
            "TrackTableCheckWorker",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }
}
