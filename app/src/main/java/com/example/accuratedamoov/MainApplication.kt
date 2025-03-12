package com.example.accuratedamoov


import android.app.Application
import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.accuratedamoov.worker.TrackTableCheckWorker
import com.example.accuratedamoov.worker.TrackingWorker
import java.util.concurrent.TimeUnit


class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Retrieve saved interval from SharedPreferences, default to 60 minutes
        val sharedPreferences = getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        val syncInterval = sharedPreferences.getInt("SYNC_INTERVAL", 60).toLong() // Default is 60 minutes

        scheduleWorker(syncInterval)
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
