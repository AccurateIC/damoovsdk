package com.example.accuratedamoov.service

import android.app.Service
import android.content.Intent
import android.location.Location
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.accuratedamoov.R
import com.raxeltelematics.v2.sdk.TrackingApi

class TrackingService : Service() {

    private val callback = object : com.raxeltelematics.v2.sdk.LocationListener {
        override fun onLocationChanged(location: Location?) {
            val trackingApi = TrackingApi.getInstance()
            if (trackingApi.isSdkEnabled() && !trackingApi.isTracking()) {
                trackingApi.startTracking()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        startForegroundService()
        if (TrackingApi.getInstance().isSdkEnabled()) {
            TrackingApi.getInstance().setLocationListener(callback)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        TrackingApi.getInstance().setLocationListener(null) // Remove listener
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startForegroundService() {
        val notification = NotificationCompat.Builder(this, "tracking_channel")
            .setContentTitle("Tracking Active")
            .setContentText("Location tracking is running in the background")
            .setSmallIcon(R.drawable.ic_splash)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(1, notification)
    }
}
