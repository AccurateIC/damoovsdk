package com.example.accuratedamoov


import android.app.Application
import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.accuratedamoov.worker.TrackTableCheckWorker
import com.example.accuratedamoov.worker.TrackingWorker
import com.raxeltelematics.v2.sdk.Settings
import com.raxeltelematics.v2.sdk.Settings.Companion.stopTrackingTimeHigh
import com.raxeltelematics.v2.sdk.TrackingApi
import java.util.concurrent.TimeUnit


class MainApplication : Application() {
 val trackingApi = TrackingApi.getInstance()
    override fun onCreate() {
        super.onCreate()


        val androidId = android.provider.Settings.Secure.getString(
            contentResolver,
            android.provider.Settings.Secure.ANDROID_ID
        )

        if (!trackingApi.isInitialized()) {
            Log.d("MainApplication", "SDK not initialized")

            val settings = Settings(
                stopTrackingTimeHigh,
                150,
                true,
                true,
                false
            )

            trackingApi.initialize(applicationContext, settings)
            Log.d("MainApplication", "SDK initialized")
        }

        // Common setup after initialization
        if (trackingApi.areAllRequiredPermissionsAndSensorsGranted() && !trackingApi.isSdkEnabled()) {
            trackingApi.setDeviceID(androidId)
            trackingApi.setEnableSdk(true)

        }

        // Retrieve saved interval from SharedPreferences, default to 15 minutes
        val sharedPreferences = getSharedPreferences("appSettings", Context.MODE_PRIVATE)
        //val syncInterval = sharedPreferences.getInt("sync_interval", 1).toLong() // Default is 15 minutes

        scheduleWorker(1)
        observeAndCancelWork( )

    }

    private fun observeAndCancelWork() {
        val workManager = WorkManager.getInstance(applicationContext)

        workManager.getWorkInfosByTagLiveData("com.raxeltelematics.v2.sdk.sync.common.tag")
            .observeForever { workInfos ->
                workInfos?.forEach { workInfo ->
                    if (!workInfo.tags.contains("com.example.accuratedamoov.worker.TrackTableCheckWorker")) {
                        Log.d("WorkManager", "Cancelling Work: ${workInfo.id}")
                        workManager.cancelWorkById(workInfo.id)
                    }
                }
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

            WorkManager.getInstance(this).enqueueUniqueWork(
                "TrackTableCheckWorker_OneTime",
                ExistingWorkPolicy.REPLACE,
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
