package com.example.accuratedamoov.broadcastreceiver

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.accuratedamoov.R
import com.telematicssdk.tracking.Settings.Companion.stopTrackingTimeHigh
import com.telematicssdk.tracking.TrackingApi
import java.util.UUID


class PermissionChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        try {
            val packageName = intent?.data?.schemeSpecificPart
            val trackingApi = TrackingApi.getInstance()
            if (packageName == context?.packageName) {
                Log.d("PermissionChangeReceiver", "Permissions might have changed!")


                // Check if location permission is granted
                if (context?.let {
                        ContextCompat.checkSelfPermission(
                            it,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        )
                    } == PackageManager.PERMISSION_GRANTED) {
                    Log.d("PermissionChangeReceiver", "Location permission was granted!")
                    val androidId = android.provider.Settings.Secure.getString(
                        context.contentResolver,
                        android.provider.Settings.Secure.ANDROID_ID
                    )

// Convert to UUID format
                    val deviceId = UUID.nameUUIDFromBytes(androidId.toByteArray()).toString()

                    trackingApi.setDeviceID(deviceId)

                    if (!trackingApi.isInitialized()) {
                        Log.d("PermissionMonitorService", "SDK not initialized")
                        val settings = com.telematicssdk.tracking.Settings(
                            stopTrackingTimeHigh,
                            150,
                            true,
                            true,
                            false
                        )
                        trackingApi.initialize(context, settings)
                        Log.d("PermissionMonitorService", "SDK initialized")
                        // for tracking 2.2.63
                        trackingApi.setDeviceID(deviceId)
                        // for tracking 3.0.0
                        /* trackingApi.setDeviceID(
                             UUID.nameUUIDFromBytes(androidId.toByteArray(Charsets.UTF_8)).toString()
                         )*/
                        trackingApi.setEnableSdk(true)
                        trackingApi.setAutoStartEnabled(true,true)
                        Log.d("PermissionChangeReceiver", "tracking SDK enabled")
                    }


                }
                // Check if location permission is revoked
                else if (context?.let {
                        ContextCompat.checkSelfPermission(
                            it,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        )
                    } == PackageManager.PERMISSION_DENIED) {
                    Log.e("PermissionChangeReceiver", "Location permission was revoked!")
                    showNotification(context)
                    // Stop tracking if permissions are revoked
                    if (trackingApi.isTracking()) {
                        Log.d(
                            "PermissionChangeReceiver",
                            "Stopping tracking due to permission revocation."
                        )
                        trackingApi.stopTracking()

                    }
                }
            }
        } catch (e: Exception) {
            Log.d("PermissionChangeReceiver", e.message.toString())
        } finally {
        }
    }

    private fun showNotification(context: Context) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "tracking_alerts",
                "Tracking Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifies when tracking is disabled"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(context, "tracking_alerts")
            .setSmallIcon(R.drawable.ic_stop_circle)
            .setContentTitle("Tracking Disabled")
            .setContentText("Location permissions are disabled. Enable them to resume tracking.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        notificationManager.notify(1001, builder.build())
    }

}
