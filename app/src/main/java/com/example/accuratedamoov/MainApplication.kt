package com.example.accuratedamoov


import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log

import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.accuratedamoov.service.PermissionMonitorService
import com.example.accuratedamoov.worker.TrackTableCheckWorker
import com.example.accuratedamoov.worker.TrackingWorker
import com.telematicssdk.tracking.TrackingApi
import java.util.UUID

import java.util.concurrent.TimeUnit


class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        if (!TrackingApi.getInstance().isInitialized()) {
            Log.d("MainApplication","SDK not initialized")
            val settings = com.telematicssdk.tracking.Settings(
                com.telematicssdk.tracking.Settings.stopTrackingTimeHigh, 150, autoStartOn = true, hfOn = true, elmOn = false
            )
            val api = TrackingApi.getInstance()
            settings.stopTrackingTimeout(10)
            api.initialize(applicationContext, settings)
            api.setAutoStartEnabled(true,true)

            val androidId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
            val deviceId = UUID.nameUUIDFromBytes(androidId.toByteArray()).toString()
            if (api.areAllRequiredPermissionsAndSensorsGranted()) {
                api.setDeviceID(deviceId)
                api.setEnableSdk(true)
            }
            Log.d("MainApplication","SDK initialized")
        }
        // Retrieve saved interval from SharedPreferences, default to 15 minutes
        val sharedPreferences = getSharedPreferences("appSettings", Context.MODE_PRIVATE)
        val syncInterval = sharedPreferences.getInt("sync_interval", 15).toLong() // Default is 15 minutes

        scheduleWorker(syncInterval)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(Intent(this, PermissionMonitorService::class.java))
        } else {
            startService(Intent(this, PermissionMonitorService::class.java))
        }
        //scheduleTrackingWorker()
    }

    private fun scheduleWorker(syncInterval: Long) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED) // Requires network
            .setRequiresBatteryNotLow(true) // Avoids running on low battery
            .build()

        val workRequest = PeriodicWorkRequestBuilder<TrackTableCheckWorker>(
            syncInterval, TimeUnit.MINUTES // Apply user-defined interval
        )
            .setInitialDelay(60, TimeUnit.SECONDS) // Delay before first execution
            .setConstraints(constraints) // Apply constraints
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "TrackTableCheckWorker",
            ExistingPeriodicWorkPolicy.UPDATE, // Ensure the new interval takes effect
            workRequest
        )
    }


    private fun scheduleTrackingWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED) // Requires network
            .setRequiresBatteryNotLow(true) // Avoids running on low battery
            .build()

        val workRequest = OneTimeWorkRequestBuilder<TrackingWorker>()
            .setInitialDelay(60, TimeUnit.SECONDS) // Delay before execution
            .setConstraints(constraints) // Apply constraints if needed
            .build()

        WorkManager.getInstance(this).enqueueUniqueWork(
            "TrackingWorker",
            ExistingWorkPolicy.REPLACE, // Ensures the new work request replaces any existing one
            workRequest
        )
    }
}


       /* val workRequest = OneTimeWorkRequestBuilder<TrackTableCheckWorker>()
            .setInitialDelay(60, TimeUnit.SECONDS) // 🔹 Runs every 5 seconds
            .build()

        WorkManager.getInstance(this).enqueue(workRequest)*/

       /* val periodicWorkRequest = PeriodicWorkRequestBuilder<TrackTableCheckWorker>(
            15, TimeUnit.MINUTES
        ).setConstraints(
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "TrackTableCheckWorker",
            ExistingPeriodicWorkPolicy.KEEP,
            periodicWorkRequest
        )*/
