package com.example.accuratedamoov.broadcastreceiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.accuratedamoov.MainApplication.Companion.TRACK_TABLE_WORKER_TAG
import com.example.accuratedamoov.worker.SystemEventScheduler
import com.example.accuratedamoov.worker.TrackTableCheckWorker
import com.google.ar.core.dependencies.c
import com.telematicssdk.tracking.TrackingApi
import java.util.UUID

import java.util.concurrent.TimeUnit

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver","phone restarted")

            try {
                val trackingApi = TrackingApi.getInstance()

                // ✅ No need to re-initialize here if done in MainApplication
                if (trackingApi.isInitialized() &&
                    trackingApi.areAllRequiredPermissionsAndSensorsGranted()
                ) {
                    val androidId = android.provider.Settings.Secure.getString(
                        context.contentResolver,
                        android.provider.Settings.Secure.ANDROID_ID
                    )

// Convert to UUID format
                    val deviceId = UUID.nameUUIDFromBytes(androidId.toByteArray()).toString()

                    trackingApi.setDeviceID(deviceId)
                    trackingApi.setEnableSdk(true)
                    trackingApi.setAutoStartEnabled(true,true)
                    if(!trackingApi.isTracking()) {
                        trackingApi.startTracking()
                    }

                    Log.d("BootReceiver","tracking SDK enabled")

                    // schedule worker for syncing
                    SystemEventScheduler.scheduleTrackTableCheck(context)
                } else {
                    Log.w("BootReceiver", "SDK not initialized yet or missing permissions")
                }
            } catch (e: Exception) {
                Log.e("BootReceiver", "Error during boot handling", e)
            }
        }
    }

}
