package com.example.accuratedamoov.worker

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.work.*
import com.example.accuratedamoov.MainApplication
import com.example.accuratedamoov.MainApplication.Companion.TRACK_TABLE_WORKER_TAG
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
        observeAndCancelWork()
        return try {
            val tableNames = listOf(
                "LastKnownPointTable", "EventsStartPointTable",
                "EventsStopPointTable",
                "TrackTable", "SampleTable"
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

            val sharedPreferences =
                applicationContext.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            val syncInterval = sharedPreferences.getInt("sync_interval", 10).toLong()
            val user_id = sharedPreferences.getInt("user_id", 0)
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
        val chunkSize = 300

        // Get user_id from SharedPreferences
        val sharedPreferences =
            applicationContext.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val userId = sharedPreferences.getInt("user_id", 0)

        for (tableName in jsonData.keys()) {
            val tableData = jsonData.getJSONArray(tableName)
            if (tableData.length() == 0) continue

            val totalRecords = tableData.length()
            var index = 0

            while (index < totalRecords) {
                val dataList = mutableListOf<Map<String, Any>>()
                val syncedIds = mutableListOf<Int>()

                val end = minOf(index + chunkSize, totalRecords)

                for (i in index until end) {
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
                        map["user_id"] = userId
                        dataList.add(map)

                        if (item.has("id")) syncedIds.add(item.getInt("id"))
                        else if (item.has("ID")) syncedIds.add(item.getInt("ID"))
                    }
                }

                if (dataList.isEmpty()) {
                    index += chunkSize
                    continue
                }

                val request = SyncRequest(dataList)
                try {
                    val response = withContext(Dispatchers.IO) {
                        apiService.syncData(tableName.replace("_", "-"), request).execute()
                    }

                    if (response.isSuccessful) {
                        Log.d("WorkManager", "✅ Synced $tableName chunk: $index–${end - 1}")
                        deleteSyncedRecords(dbHelper, tableName)

                    } else {
                        Log.e(
                            "WorkManager",
                            "❌ Sync failed for $tableName chunk: ${response.errorBody()?.string()}"
                        )
                        allSuccessful = false
                        break
                    }
                } catch (e: Exception) {
                    Log.e("WorkManager", "❌ Sync error for $tableName chunk: ${e.message}")
                    allSuccessful = false
                    break
                }

                index += chunkSize
            }
        }

        return allSuccessful
    }


    private fun deleteSyncedRecords(dbHelper: DatabaseHelper, tableName: String) {
        val db = dbHelper.openDatabase() ?: return

        db.beginTransaction()
        try {
            db.execSQL("DELETE FROM $tableName")
            Log.d("WorkManager", "🗑️ Deleted all records from $tableName after sync")

            db.setTransactionSuccessful()
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
            .setInitialDelay(60, TimeUnit.SECONDS)
            .addTag(TRACK_TABLE_WORKER_TAG)
            .build()

        WorkManager.getInstance(applicationContext).enqueueUniqueWork(
            "TrackTableCheckWorker",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    private fun observeAndCancelWork() {
        val workManager = WorkManager.getInstance(applicationContext)

        val workQuery = WorkQuery.Builder
            .fromStates(listOf(WorkInfo.State.ENQUEUED))
            .build()

        Handler(Looper.getMainLooper()).post {
            workManager.getWorkInfosLiveData(workQuery).observeForever { workInfos ->
                workInfos?.forEach { workInfo ->
                    val tags = workInfo.tags
                    val workId = workInfo.id

                    Log.d("WorkManager", "ID: $workId")
                    Log.d("WorkManager", "State: ${workInfo.state}")
                    Log.d("WorkManager", "Tags: $tags")

                    // Cancel work if it's NOT TrackTableCheckWorker
                    if (!workInfo.tags.contains(MainApplication.TRACK_TABLE_WORKER_TAG)) {
                        workManager.cancelWorkById(workInfo.id)
                    }
                }
            }
        }
    }
}
