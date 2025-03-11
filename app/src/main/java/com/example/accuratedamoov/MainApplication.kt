package com.example.accuratedamoov


import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.accuratedamoov.worker.TrackTableCheckWorker
import java.util.concurrent.TimeUnit


class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        val workRequest = PeriodicWorkRequestBuilder<TrackTableCheckWorker>(
            15, TimeUnit.MINUTES // Minimum interval allowed by WorkManager
        )
            .setInitialDelay(60, TimeUnit.SECONDS) // Delay before first execution
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "TrackTableCheckWorker",
            ExistingPeriodicWorkPolicy.KEEP, // Prevents duplicate workers
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
