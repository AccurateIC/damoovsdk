package com.example.accuratedamoov.worker

import android.content.Context
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.accuratedamoov.database.DatabaseHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class TrackTableCheckWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : Worker(appContext, workerParams) {

    override fun doWork(): Result {
        val dbHelper = DatabaseHelper(applicationContext)

        val sharedPreferences =
            applicationContext.getSharedPreferences("tracker_prefs", Context.MODE_PRIVATE)
        val lastRowCount = sharedPreferences.getInt("last_row_count", -1)

        /*val rowCount = dbHelper.getRowCount()
        Log.d("Omkar: RowCount",rowCount.toString())

        // just for testing
        val workRequest = OneTimeWorkRequestBuilder<TrackTableCheckWorker>()
            .setInitialDelay(15, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(applicationContext).enqueue(workRequest)

        return if (rowCount != lastRowCount) {
            // if tables records changes then only process data
            sharedPreferences.edit().putInt("last_row_count", rowCount).apply()
            Result.success()
        } else {
            Log.d("Omkar: RowCount","unchanged")
            Result.retry()
        }*/

        return try {
            val tableNames = listOf(
                "LastKnownPointTable", "EventsStartPointTable", "EventsTable",
                "EventsStopPointTable", "RangeDirectTable", "RangeLateralTable",
                "RangeVerticalTable", "RangeAccuracyTable", "RangeSpeedTable", "TrackTable"
            )
            val workRequest = OneTimeWorkRequestBuilder<TrackTableCheckWorker>()
                .setInitialDelay(60, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(applicationContext).enqueue(workRequest)


            // 1 Check if any table has changed
            val changedTables = mutableListOf<String>()
            for (table in tableNames) {
                val currentCount = dbHelper.getRowCount(table)
                val lastCount = sharedPreferences.getInt(table, -1)
                if (currentCount != lastCount) {

                    changedTables.add(table)
                }
            }


            // 2 If no tables have changed, do nothing
            if (changedTables.isEmpty()) {
                Log.d("WorkManager", "No changes detected, skipping sync.")
            }

            // 3️ Fetch data only for changed tables
            val jsonData = JSONObject()
            for (table in changedTables) {
                jsonData.put(table, dbHelper.getTableData(table))
            }

            // 4 Push to cloud
            val success = true // API push

            if (success) {
                // 5️ Update SharedPreferences with new row counts
                val editor = sharedPreferences.edit()
                for (table in changedTables) {
                    editor.putInt(table, dbHelper.getRowCount(table))
                }
                editor.apply()

                Log.d("OmkarWorkManager", "Data sync successful, updated cache")
                Result.success()
            } else {
                Log.e("OmkarWorkManager", "Data sync failed, will retry")
                Result.retry()
            }

        } catch (e: Exception) {
            Log.e("OmkarWorkManager", "Error in Worker", e)
            Result.failure()
        }

    }

}

