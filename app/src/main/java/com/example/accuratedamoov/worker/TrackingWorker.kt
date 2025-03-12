package com.example.accuratedamoov.worker

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import android.Manifest
import android.util.Log
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.raxeltelematics.v2.sdk.TrackingApi
import java.util.concurrent.TimeUnit

class TrackingWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {

    override fun doWork(): Result {
        val trackingApi = TrackingApi.getInstance()

        val nextWorkRequest = OneTimeWorkRequestBuilder<TrackingWorker>()
            .setInitialDelay(60, TimeUnit.SECONDS) // Delay before next run
            .build()

        WorkManager.getInstance(applicationContext).enqueue(nextWorkRequest)
        // Ensure required permissions are granted
        if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            return Result.failure()
        }

        // Initialize tracking if not already started

        if(TrackingApi.getInstance() != null && TrackingApi.getInstance().isSdkEnabled()) {
            Log.d("TrackingWorker ->isTracking", TrackingApi.getInstance().isTracking().toString())
        }
        if (!trackingApi.isSdkEnabled()) {
            trackingApi.setEnableSdk(true)
        }

        if (!trackingApi.isTracking()) {
            trackingApi.startTracking()
        }

        return Result.success()
    }
}

