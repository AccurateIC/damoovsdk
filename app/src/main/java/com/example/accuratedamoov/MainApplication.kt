package com.example.accuratedamoov

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat

import com.raxeltelematics.v2.sdk.Settings
import com.raxeltelematics.v2.sdk.TrackingApi

class MainApplication : Application() {
    @SuppressLint("HardwareIds")
    override fun onCreate() {
        super.onCreate()
        val settings = Settings(
            stopTrackingTimeout = Settings.stopTrackingTimeHigh,
            accuracy = Settings.accuracyHigh,
            autoStartOn = true,
            elmOn = false,
            hfOn = true
        )
        val api = TrackingApi.getInstance()
        api.initialize(this, settings)
        api.setDeviceID( android.provider.Settings.Secure.getString(this.contentResolver, android.provider.Settings.Secure.ANDROID_ID))
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            return
        }
        api.setEnableSdk(true)
    }
}