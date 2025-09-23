package com.example.accuratedamoov


import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
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
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.telematicssdk.tracking.Settings
import com.telematicssdk.tracking.TrackingApi
import java.util.UUID
import com.example.accuratedamoov.BuildConfig
import com.example.accuratedamoov.worker.SystemEventScheduler
import com.example.accuratedamoov.worker.SystemEventScheduler.EVENTSYNC_TAG
import java.util.concurrent.TimeUnit


class MainApplication : Application() {

    private val trackingApi: TrackingApi by lazy { TrackingApi.getInstance() }

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        FirebaseCrashlytics.getInstance().setCustomKey("AppVersion",BuildConfig.APP_VERSION_NAME)
        FirebaseCrashlytics.getInstance().setCustomKey("Device", Build.MODEL)

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        initializeTrackingSdk()
        if (!isUserLoggedIn()) {
            Log.d(TAG, "User not logged in, skipping SDK init and workers.")
            return
        }
        SystemEventScheduler.scheduleSystemEvent(this)

        enableTrackingIfPossible()

        val syncInterval = getSyncInterval()
        SystemEventScheduler.scheduleTrackTableCheck(applicationContext)
        logAllWorkerRequests()
        SystemEventScheduler.observeAndCancelOtherWork(applicationContext)
    }

    private fun isUserLoggedIn(): Boolean {
        val prefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        return prefs.getBoolean("is_logged_in", false)
    }

    private fun initializeTrackingSdk() {
        if (trackingApi.isInitialized()) return
        try {
            val settings = Settings(
                Settings.stopTrackingTimeHigh,
                Settings.accuracyHigh,
                true,
                true,
                false
            ).apply {
                stopTrackingTimeout(5)
            }
            trackingApi.initialize(applicationContext, settings)
            Log.d(TAG, "Tracking SDK initialized")
        } catch (e: Exception) {
            Log.e(TAG, "SDK initialization failed: ${e.message}", e)
        }
    }


    private fun enableTrackingIfPossible() {
        if (!trackingApi.isInitialized() || !trackingApi.areAllRequiredPermissionsAndSensorsGranted()) return

        val androidId = android.provider.Settings.Secure.getString(
            contentResolver,
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

        Log.d(TAG, "Tracking SDK enabled with Device ID: $androidId")
    }

    private fun getSyncInterval(): Long {
        val sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        return sharedPreferences.getInt("sync_interval", 10).toLong()
    }

    private fun logAllWorkerRequests() {
        val workQuery = WorkQuery.Builder.fromStates(WorkInfo.State.entries).build()
        val workInfoList = WorkManager.getInstance(applicationContext).getWorkInfos(workQuery).get()

        workInfoList.forEach { workInfo ->
            Log.d(TAG_WORK, "ID: ${workInfo.id}, State: ${workInfo.state}, Tags: ${workInfo.tags}")
        }
    }



    private fun scheduleTrackingWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<TrackingWorker>()
            .setInitialDelay(60, TimeUnit.SECONDS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniqueWork(
            TRACKING_WORKER_TAG,
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    companion object {
        private const val TAG = "MainApplication"
        private const val TAG_WORK = "WorkManager"
        const val TRACK_TABLE_WORKER_TAG = "TrackTableCheckWorker"
        private const val TRACKING_WORKER_TAG = "TrackingWorker"
    }
}

