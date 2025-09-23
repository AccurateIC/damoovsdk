package com.example.accuratedamoov.broadcastreceiver

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.accuratedamoov.MainApplication.Companion.TRACK_TABLE_WORKER_TAG
import com.telematicssdk.tracking.TrackingApi
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import com.example.accuratedamoov.R
import com.example.accuratedamoov.data.local.systemevents.SystemEventDatabase
import com.example.accuratedamoov.data.local.systemevents.SystemEventEntity
import com.example.accuratedamoov.worker.SystemEventScheduler
import com.example.accuratedamoov.worker.TrackTableCheckWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class SystemChangeReceiver : BroadcastReceiver() {

    val trackingApi: TrackingApi = TrackingApi.getInstance()
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val eventMessage: String
        val eventType: String

        when (action) {
            PowerManager.ACTION_POWER_SAVE_MODE_CHANGED -> {
                val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                val isPowerSave = pm.isPowerSaveMode
                eventMessage = "Battery saver mode = $isPowerSave"
                eventType = "BATTERY_SAVER"
                if(!isPowerSave){
                    scheduleWorker(context)
                    SystemEventScheduler.scheduleSystemEvent(context)
                }
                if (!isPowerSave && isLocationPermissionGranted(context)) startTrackingSdkSafe()
                else stopTrackingSdkSafe()
            }

            Intent.ACTION_PACKAGE_REPLACED -> {
                eventMessage = "Package replaced: ${intent.data}"
                eventType = "PACKAGE_REPLACED"

                if (isLocationPermissionGranted(context)) startTrackingSdkSafe()
                else stopTrackingSdkSafe()
            }

            LocationManager.MODE_CHANGED_ACTION,
            LocationManager.PROVIDERS_CHANGED_ACTION -> {
                val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                val isEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                        lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
                eventMessage = "Location setting/provider changed → enabled = $isEnabled"
                eventType = "LOCATION_CHANGE"

                if (isEnabled && isLocationPermissionGranted(context)) startTrackingSdkSafe()
                else stopTrackingSdkSafe()
            }

            else -> {
                eventMessage = "Other system event: $action"
                eventType = "OTHER"
            }
        }

        // Log, notify, and save the event
        safeLog(context, eventMessage)
        showNotification(context, "System Event", eventMessage)
        saveEvent(context, eventMessage, eventType)
    }

    private fun getCurrentUserId(context: Context): Int {
        val sharedPrefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        return sharedPrefs.getInt("user_id", 0)
    }

    private fun saveEvent(context: Context, message: String, type: String) {
        val androidId = android.provider.Settings.Secure.getString(
            context.contentResolver,
            android.provider.Settings.Secure.ANDROID_ID
        )
        val deviceId = UUID.nameUUIDFromBytes(androidId.toByteArray()).toString()

        val db = SystemEventDatabase.getInstance(context)
        val dao = db.systemEventDao()
        val event = SystemEventEntity(
            device_id = deviceId,
            user_id = getCurrentUserId(context),
            event_message = message,
            event_type = type,
            timestamp = System.currentTimeMillis()
        )

        CoroutineScope(Dispatchers.IO).launch {
            try {
                dao.insertEvent(event)
                Log.d("SystemChangeReceiver", "✅ Event inserted: $message ($type)")

                // Optional: verify by counting rows
                val allEvents = dao.getAllEvents()
                Log.d("SystemChangeReceiver", "📊 Total events in DB: ${allEvents.size}")

                // Optional: log last inserted
                allEvents.firstOrNull()?.let { latest ->
                    Log.d(
                        "SystemChangeReceiver",
                        "🆕 Latest event in DB → ${latest.event_message} @${latest.timestamp}"
                    )
                }
            } catch (e: Exception) {
                Log.e("SystemChangeReceiver", "❌ Failed to insert event", e)
            }
        }
    }

    private fun isLocationPermissionGranted(context: Context) =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED

    // Tracking SDK helper methods
    private fun startTrackingSdkSafe() {
        if (trackingApi.isInitialized() && !trackingApi.isSdkEnabled()) {
            trackingApi.setEnableSdk(true)
            trackingApi.setAutoStartEnabled(true, true)
            if (!trackingApi.isTracking()) {
                trackingApi.startTracking()
            }
        }
    }

    private fun stopTrackingSdkSafe() {
        if (trackingApi.isInitialized()) {

            if (trackingApi.isTracking()) {
                trackingApi.stopTracking()
                trackingApi.setEnableSdk(false)
            }
        }
    }

    private fun safeLog(context: Context, message: String) {
        try {
            val logFile = File(context.getExternalFilesDir(null), "system_changes.log")
            val writer = FileWriter(logFile, true)
            val timestamp =
                SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            writer.appendLine("[$timestamp] $message")
            writer.flush()
            writer.close()
        } catch (e: Exception) {
            Log.e("SystemChangeReceiver", "Error writing log", e)
        }
    }

    private fun showNotification(context: Context, title: String, message: String) {
        val channelId = "system_events"
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Settings Updates",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            nm.createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_tracking_sdk_status_bar)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        nm.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun scheduleWorker(mContext: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<TrackTableCheckWorker>()
            .setConstraints(constraints)
            .setInitialDelay(60, TimeUnit.SECONDS)
            .addTag(TRACK_TABLE_WORKER_TAG)
            .build()

        WorkManager.getInstance(mContext).enqueueUniqueWork(
            "TrackTableCheckWorker",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }
}

