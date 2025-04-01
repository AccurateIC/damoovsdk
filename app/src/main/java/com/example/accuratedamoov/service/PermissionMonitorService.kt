package com.example.accuratedamoov.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkQuery
import com.example.accuratedamoov.R
import com.telematicssdk.tracking.Settings.Companion.stopTrackingTimeHigh
import com.telematicssdk.tracking.TrackingApi

import java.util.UUID

class PermissionMonitorService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private val checkInterval: Long = 5000

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        startForegroundService()
        checkPermissions()
        observeAndCancelWork()
        return START_STICKY
    }

    private fun startForegroundService() {
        val notification = createNotification()
        startForeground(1, notification) // ✅ This prevents ANR
    }


    private fun createNotification(): Notification {
        val channelId = "PermissionMonitorServiceChannel"
        val notificationManager = getSystemService(NotificationManager::class.java)

        // Create Notification Channel (if not created)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Permission Monitor",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }

        // Build notification
        return NotificationCompat.Builder(this, channelId)
            .setContentText("Running in the background")
            .setSmallIcon(R.drawable.ic_splash)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
    }

    private fun checkPermissions() {
        val trackingApi = TrackingApi.getInstance()

        handler.postDelayed(object : Runnable {
            override fun run() {
                val isPermissionGranted = ContextCompat.checkSelfPermission(
                    this@PermissionMonitorService, Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED

                if (isPermissionGranted && trackingApi.areAllRequiredPermissionsAndSensorsGranted()) {
                    val androidId = android.provider.Settings.Secure.getString(
                        contentResolver,
                        android.provider.Settings.Secure.ANDROID_ID
                    )
                    if (!trackingApi.isInitialized()) {
                        Log.d("MainApplication", "SDK not initialized")

                        val settings = com.telematicssdk.tracking.Settings(
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
                    if(!trackingApi.isSdkEnabled()) {
                        trackingApi.setDeviceID(UUID.nameUUIDFromBytes(androidId.toByteArray(Charsets.UTF_8))
                            .toString())
                        trackingApi.setEnableSdk(true)
                    }
                    trackingApi.setAutoStartEnabled(true,true)

                } else {
                    Log.e(
                        "PermissionMonitorService",
                        "Location permission revoked. Stopping tracking."
                    )
                    showNotification()
                    if (trackingApi.isTracking()) {
                        trackingApi.stopTracking()
                        trackingApi.setEnableSdk(false)
                    }
                }

                handler.postDelayed(this, checkInterval)
            }
        }, checkInterval)
    }

    private fun showNotification() {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "tracking_alerts",
                "Tracking Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts for tracking issues"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, "tracking_alerts")
            .setSmallIcon(R.drawable.ic_stop_circle)
            .setContentTitle("Tracking Disabled")
            .setContentText("Location permissions are disabled. Tap to enable them.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        notificationManager.notify(1001, builder.build())
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

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
}
