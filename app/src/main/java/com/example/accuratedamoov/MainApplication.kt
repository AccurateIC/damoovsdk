package com.example.accuratedamoov


import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkQuery
import com.example.accuratedamoov.worker.TrackTableCheckWorker
import com.example.accuratedamoov.worker.TrackingWorker
import com.telematicssdk.tracking.Settings
import com.telematicssdk.tracking.Settings.Companion.stopTrackingTimeHigh
import com.telematicssdk.tracking.TrackingApi
import java.util.Arrays
import java.util.UUID
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
            trackingApi.setDeviceID(
                UUID.nameUUIDFromBytes(androidId.toByteArray(Charsets.UTF_8))
                .toString())
            trackingApi.setEnableSdk(true)
            trackingApi.setAutoStartEnabled(true,true)

        }
        if(trackingApi.areAllRequiredPermissionsAndSensorsGranted() && trackingApi.isSdkEnabled()){
            trackingApi.setAutoStartEnabled(true,true)
        }

        val sharedPreferences = getSharedPreferences("appSettings", Context.MODE_PRIVATE)
        val syncInterval = sharedPreferences.getInt("sync_interval", 1).toLong()

        scheduleWorker(syncInterval)
        getAllWorkerRequests(applicationContext)
        observeAndCancelWork()

    }

    fun getAllWorkerRequests(context: Context) {
        val workQuery = WorkQuery.Builder
            .fromStates(listOf(WorkInfo.State.ENQUEUED, WorkInfo.State.RUNNING))
            .build()

        val workInfoList = WorkManager.getInstance(context).getWorkInfos(workQuery).get()

        for (workInfo in workInfoList) {
            Log.d("OmkarWorkmanager", "ID: ${workInfo.id}")
            Log.d("OmkarWorkmanager", "State: ${workInfo.state}")
            Log.d("OmkarWorkmanager", "Tags: ${workInfo.tags}")
        }
    }

   /* private fun observeAndCancelWork() {
        val workManager = WorkManager.getInstance(applicationContext)

        workManager.getWorkInfosByTagLiveData("com.telematicssdk.tracking.sync.common.tag")
            .observeForever { workInfos ->
                workInfos?.forEach { workInfo ->
                    if (!workInfo.tags.contains("com.example.accuratedamoov.worker.TrackTableCheckWorker")) {
                        Log.d("WorkManager", "Cancelling Work: ${workInfo.id}")
                        workManager.cancelWorkById(workInfo.id)
                    }
                }
            }
    }*/

    private fun observeAndCancelWork() {
        val workManager = WorkManager.getInstance(applicationContext)

        val workQuery = WorkQuery.Builder
            .fromStates(listOf(WorkInfo.State.ENQUEUED))
            .build()
        // Observe all enqueued work
        workManager.getWorkInfosLiveData(workQuery).observeForever  { workInfos ->
            workInfos?.forEach { workInfo ->
                val tags = workInfo.tags
                val workId = workInfo.id

                // Log the details
                Log.d("WorkManager", "ID: $workId")
                Log.d("WorkManager", "State: ${workInfo.state}")
                Log.d("WorkManager", "Tags: $tags")

                // Cancel work if it's NOT TrackTableCheckWorker
                if (!tags.contains("com.example.accuratedamoov.worker.TrackTableCheckWorker")) {
                    Log.d("WorkManager", "Cancelling Work: $workId")
                    workManager.cancelWorkById(workId)
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
                .setInitialDelay(syncInterval, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(this).enqueueUniqueWork(
                "TrackTableCheckWorker",
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
