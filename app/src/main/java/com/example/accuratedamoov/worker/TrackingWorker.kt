package com.example.accuratedamoov.worker

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import android.Manifest
import android.util.Log

import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.accuratedamoov.database.DatabaseHelper
import com.telematicssdk.tracking.TrackingApi
import java.util.concurrent.TimeUnit

class TrackingWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {

    private val dbHelper = DatabaseHelper.getInstance(applicationContext)

    override fun doWork(): Result {
        Log.d("TrackingWorker", "✅ TrackingWorker started")
        return try {
            val trackingApi = TrackingApi.getInstance()

            if (trackingApi.isInitialized() && trackingApi.isSdkEnabled()) {
                val isTracking = trackingApi.isTracking()
                Log.d("TrackingWorker", "isTracking: $isTracking")

                // Ensure column exists before processing
                dbHelper.addEndDateColumnIfNotExists()

                // If not currently tracking, update the latest active track's end_date
                if (!isTracking) {
                    updateLatestActiveTrackEndDate()
                }
            }
            return Result.success()
        } catch (e: Exception) {
            Log.e("TrackingWorker", "❌ doWork failed", e)
            Result.failure()
        }
    }

    private fun updateLatestActiveTrackEndDate() {
        try {
            val db = dbHelper.openDatabase() ?: return

            val cursor = db.rawQuery(
                "SELECT track_id, start_date FROM TrackTable WHERE end_date IS NULL ORDER BY start_date DESC LIMIT 1",
                null
            )

            if (cursor.moveToFirst()) {
                val trackId = cursor.getInt(0)
                val startDate = cursor.getLong(1)
                val currentMillis = System.currentTimeMillis()

                // Ensure end_date is after start_date
                if (currentMillis > startDate) {
                    db.execSQL(
                        "UPDATE TrackTable SET end_date = ? WHERE track_id = ?",
                        arrayOf(currentMillis, trackId)
                    )
                    Log.d("TrackingWorker", "✅ Updated end_date for track_id=$trackId at $currentMillis")
                } else {
                    Log.w("TrackingWorker", "⚠️ Skipped update: currentMillis <= startDate")
                }
            } else {
                Log.i("TrackingWorker", "ℹ️ No active track found to update.")
            }

            cursor.close()
        } catch (e: Exception) {
            Log.e("TrackingWorker", "❌ Failed to update end_date", e)
        }
    }
}

