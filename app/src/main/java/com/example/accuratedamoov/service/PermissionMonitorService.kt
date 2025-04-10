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
import android.os.*
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkQuery
import com.example.accuratedamoov.R
import com.raxeltelematics.v2.sdk.Settings.Companion.stopTrackingTimeHigh
import com.raxeltelematics.v2.sdk.TrackingApi

import java.util.*

class PermissionMonitorService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private val checkInterval: Long = 15000
    private val CHANNEL_ID = "permission_monitor_service"

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundService() // ✅ Required for Android 8+
        handler.postDelayed(::checkPermissions, checkInterval)
        observeAndCancelWork()
        return START_STICKY
    }

    private fun startForegroundService() {
        createNotificationChannel()
        val notification = createSilentNotification()
        startForeground(1, notification) // Start in foreground mode to prevent ANR issues

        // ✅ Hide notification immediately after startup
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Permission Monitor",
                NotificationManager.IMPORTANCE_MIN // Lowest importance, no user attention
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun createSilentNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentText("Monitoring permissions in the background")
            .setSmallIcon(R.drawable.ic_splash)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
    }

    private fun checkPermissions() {
        val trackingApi = TrackingApi.getInstance()

        val isPermissionGranted = ContextCompat.checkSelfPermission(
            this, android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (isPermissionGranted && trackingApi.areAllRequiredPermissionsAndSensorsGranted()) {
            val androidId = Settings.Secure.getString(
                contentResolver,
                Settings.Secure.ANDROID_ID
            )

            if (!trackingApi.isInitialized()) {
                Log.d("PermissionMonitorService", "SDK not initialized")
                val settings = com.raxeltelematics.v2.sdk.Settings(
                    stopTrackingTimeHigh,
                    150,
                    true,
                    true,
                    false
                )
                settings.stopTrackingTimeout(15)
                trackingApi.initialize(applicationContext, settings)
                Log.d("PermissionMonitorService", "SDK initialized")
            }
            if(!trackingApi.isSdkEnabled()) {
                // for tracking 2.2.63
                trackingApi.setDeviceID(androidId)
                // for tracking 3.0.0
                /* trackingApi.setDeviceID(
                     UUID.nameUUIDFromBytes(androidId.toByteArray(Charsets.UTF_8)).toString()
                 )*/
                trackingApi.setEnableSdk(true)
                Log.d("PermissionMonitorService", "tracking SDK enabled")
            }else{
                Log.d("PermissionMonitorService", "Already tracking SDK enabled, no need to enable again and tracking is ${trackingApi.isTracking()}")
            }
                // trackingApi.setAutoStartEnabled(true, true)  //for tracking 3.0.0
        } else {
            Log.e("PermissionMonitorService", "Location permission revoked. Stopping tracking.")
            showPermissionRevokedNotification()
            if (trackingApi.isTracking()) {
                trackingApi.stopTracking()
                trackingApi.setEnableSdk(false)
            }
        }

        handler.postDelayed(::checkPermissions, checkInterval)
    }

    private fun showPermissionRevokedNotification() {
        val notificationManager = getSystemService(NotificationManager::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "tracking_alerts",
                "Tracking Alerts",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Alerts for tracking issues"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, "tracking_alerts")
            .setSmallIcon(R.drawable.ic_stop_circle)
            .setContentTitle("Tracking Disabled")
            .setContentText("Location permissions are disabled. Tap to enable them.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(1001, notification) // Show only when permission is revoked
    }

    override fun onBind(intent: Intent?): IBinder? = null

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
}
