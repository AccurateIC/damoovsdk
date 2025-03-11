package com.example.accuratedamoov.broadcastreceiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.accuratedamoov.worker.TrackTableCheckWorker
import java.util.concurrent.TimeUnit

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val workRequest = PeriodicWorkRequestBuilder<TrackTableCheckWorker>(
                15, TimeUnit.MINUTES // 🔹 Minimum allowed by WorkManager
            )
                .setInitialDelay(1, TimeUnit.MINUTES) // 🔹 Delay after boot
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "TrackTableCheckWork",
                ExistingPeriodicWorkPolicy.KEEP, // 🔹 Ensures only one instance runs
                workRequest
            )
        }
    }
}
