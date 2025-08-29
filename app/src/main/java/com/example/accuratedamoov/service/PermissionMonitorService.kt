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
import androidx.lifecycle.Observer
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkQuery
import com.example.accuratedamoov.R
import com.example.accuratedamoov.worker.TrackTableCheckWorker
import com.example.accuratedamoov.worker.TrackingWorker
import com.telematicssdk.tracking.TrackingApi


import java.util.*
import java.util.concurrent.TimeUnit

class PermissionMonitorService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private val checkInterval: Long = 60_000 // check every 1 min instead of 15s
    private val CHANNEL_ID = "permission_monitor_service"

    private val workObserver = Observer<List<WorkInfo>> { workInfos ->
        workInfos?.forEach { workInfo ->
            val tags = workInfo.tags
            val workId = workInfo.id

            Log.d("WorkManager", "ID: $workId")
            Log.d("WorkManager", "State: ${workInfo.state}")
            Log.d("WorkManager", "Tags: $tags")

            if (!tags.contains("com.example.accuratedamoov.worker.TrackTableCheckWorker")) {
                Log.d("WorkManager", "Cancelling Work: $workId")
                WorkManager.getInstance(applicationContext).cancelWorkById(workId)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val prefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("is_logged_in", false)) {
            Log.d("PermissionMonitorService", "User not logged in, stopping service.")
            stopSelf()
            return START_NOT_STICKY
        }

        startForegroundService()
        handler.postDelayed(::checkPermissions, checkInterval)
        observeAndCancelWork()
        return START_STICKY
    }

    private fun startForegroundService() {
        createNotificationChannel()
        val notification = createSilentNotification()
        startForeground(1, notification) // ✅ Keep notification for reliability
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Permission Monitor",
                NotificationManager.IMPORTANCE_MIN
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun createSilentNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentText("Monitoring permissions in the background")
            .setSmallIcon(R.drawable.ic_splash)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .build()
    }

    private fun checkPermissions() {
        val trackingApi = TrackingApi.getInstance()

        val isPermissionGranted = ContextCompat.checkSelfPermission(
            this, android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (isPermissionGranted && trackingApi.isInitialized() &&
            trackingApi.areAllRequiredPermissionsAndSensorsGranted()
        ) {
            if (!trackingApi.isSdkEnabled()) {
                val androidId = android.provider.Settings.Secure.getString(
                    contentResolver,
                    android.provider.Settings.Secure.ANDROID_ID
                )

// Convert to UUID format
                val deviceId = UUID.nameUUIDFromBytes(androidId.toByteArray()).toString()

                trackingApi.setDeviceID(deviceId)
                trackingApi.setEnableSdk(true)
                trackingApi.setAutoStartEnabled(true,true)


                Log.d("PermissionMonitorService", "tracking SDK enabled")
            } else {
                Log.d("PermissionMonitorService", "SDK already enabled, tracking = ${trackingApi.isTracking()}")
            }
        } else {
            Log.e("PermissionMonitorService", "Permission revoked or SDK not ready. Stopping tracking.")
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
            )
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

        notificationManager.notify(1001, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        WorkManager.getInstance(applicationContext).getWorkInfosLiveData(
            WorkQuery.Builder.fromStates(listOf(WorkInfo.State.ENQUEUED)).build()
        ).removeObserver(workObserver)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun observeAndCancelWork() {
        val workManager = WorkManager.getInstance(applicationContext)
        val workQuery = WorkQuery.Builder
            .fromStates(listOf(WorkInfo.State.ENQUEUED))
            .build()
        workManager.getWorkInfosLiveData(workQuery).observeForever(workObserver)
    }
}
