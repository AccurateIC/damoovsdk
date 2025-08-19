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
import com.example.accuratedamoov.worker.TrackTableCheckWorker
import com.raxeltelematics.v2.sdk.Settings
import com.raxeltelematics.v2.sdk.Settings.Companion.stopTrackingTimeHigh
import com.raxeltelematics.v2.sdk.TrackingApi
import java.util.concurrent.TimeUnit

class BootReceiver : BroadcastReceiver() {

    val trackingApi = TrackingApi.getInstance()
    lateinit var mContext: Context

    override fun onReceive(context: Context, intent: Intent) {
        mContext = context
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver","phone restarted")
            val androidId = android.provider.Settings.Secure.getString(
                context.contentResolver,
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
                settings.stopTrackingTimeout(10)
                trackingApi.initialize(context, settings)
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
                if(!trackingApi.isTracking()){
                    trackingApi.startTracking()
                    scheduleWorker(15)
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

        WorkManager.getInstance(mContext).enqueueUniqueWork(
            "TrackTableCheckWorker",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

}
