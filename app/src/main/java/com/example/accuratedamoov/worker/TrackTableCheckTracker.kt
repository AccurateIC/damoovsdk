package com.example.accuratedamoov.worker

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

import androidx.work.WorkerParameters
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
        return try {
            val dbHelper = DatabaseHelper(applicationContext)
            val tableNames = listOf(
                "LastKnownPointTable", "EventsStartPointTable", "EventsTable",
                "EventsStopPointTable", "RangeDirectTable", "RangeLateralTable",
                "RangeVerticalTable", "RangeAccuracyTable", "RangeSpeedTable", "TrackTable"
            )

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

            if (!hasDataToSync) {
                Log.d("WorkManager", "No data found in any table, skipping sync.")
                return Result.success()
            }

            val success = withContext(Dispatchers.IO) { syncData(jsonData) }

            if (success) {
                Log.d("WorkManager", "✅ Data sync successful, marked records as synced and deleted.")
                scheduleWorker(1)
                Result.success()
            } else {
                Log.e("WorkManager", "❌ Data sync failed, will retry.")
                Result.retry()
            }

        } catch (e: Exception) {
            Log.e("WorkManager", "❌ Error in Worker", e)
            Result.failure()
        }
    }

    private suspend fun syncData(jsonData: JSONObject): Boolean {
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
                Log.d("WorkManager", "⚠️ No valid records found for $tableName, skipping sync.")
                continue
            }

            val request = SyncRequest(dataList)

            try {
                val response = withContext(Dispatchers.IO) {
                    apiService.syncData(tableName.replace("_", "-"), request).execute()
                }

                if (response.isSuccessful) {
                    withContext(Dispatchers.IO) {
                        deleteSyncedRecords(applicationContext, tableName, syncedIds)
                    }
                    Log.d("WorkManager", "✅ Sync successful for $tableName, records deleted.")
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

    private fun deleteSyncedRecords(context: Context, tableName: String, ids: List<Int>) {
        if (ids.isEmpty()) return

        val dbHelper = DatabaseHelper(context)
        val db = SQLiteDatabase.openDatabase(dbHelper.dbPath, null, SQLiteDatabase.OPEN_READWRITE)

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
            .setInitialDelay(syncInterval, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(applicationContext).enqueueUniqueWork(
            "TrackTableCheckWorker_OneTime",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )


    }
}



