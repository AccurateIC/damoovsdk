package com.example.accuratedamoov


import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkQuery
import com.example.accuratedamoov.broadcastreceiver.PermissionChangeReceiver
import com.example.accuratedamoov.service.NetworkMonitorService
import com.example.accuratedamoov.service.PermissionMonitorService
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


        val permissionMonitorServicesIntent = Intent(this, PermissionMonitorService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(permissionMonitorServicesIntent)
        } else {
            startService(permissionMonitorServicesIntent)
        }
        /*val permissionReceiver = PermissionChangeReceiver()
        val filter = IntentFilter(Intent.ACTION_PACKAGE_CHANGED).apply {
            addDataScheme("package")
        }
        registerReceiver(permissionReceiver, filter)*/

        Log.d("MainApplication", "PermissionChangeReceiver registered")

        Log.d("MainApplication", "PermissionReceiver registered")
        val networkServiceIntent = Intent(this, NetworkMonitorService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(networkServiceIntent)
        } else {
            startService(networkServiceIntent)
        }

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
        if (trackingApi.isInitialized() && trackingApi.areAllRequiredPermissionsAndSensorsGranted()) {

                // for tracking 2.2.63
                trackingApi.setDeviceID(androidId)


                // for tracking 3.0.0
               /* trackingApi.setDeviceID(
                    UUID.nameUUIDFromBytes(androidId.toByteArray(Charsets.UTF_8)).toString()
                )*/
                trackingApi.setEnableSdk(true)
                Log.d("MainApplication","tracking SDK enabled")
            // for tracking 3.0.0,not present in 2.2.263
            //trackingApi.setAutoStartEnabled(true,true)
        }

        val sharedPreferences = getSharedPreferences("appSettings", Context.MODE_PRIVATE)
        val syncInterval = sharedPreferences.getInt("sync_interval", 10).toLong()

        scheduleWorker(syncInterval)
        getAllWorkerRequests(applicationContext)
        observeAndCancelWork()

    }

    fun getAllWorkerRequests(context: Context) {
        val workQuery = WorkQuery.Builder
            .fromStates(WorkInfo.State.entries)
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

        Handler(Looper.getMainLooper()).post {
            workManager.getWorkInfosLiveData(workQuery).observeForever { workInfos ->
                workInfos?.forEach { workInfo ->
                    val tags = workInfo.tags
                    val workId = workInfo.id

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
