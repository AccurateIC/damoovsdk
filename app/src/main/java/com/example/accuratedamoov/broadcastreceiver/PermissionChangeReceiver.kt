package com.example.accuratedamoov.broadcastreceiver

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat

class PermissionChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val packageName = intent?.data?.schemeSpecificPart

        if (packageName == context?.packageName) {
            Log.d("PermissionChangeReceiver", "Permissions might have changed!")

            // Check if specific permissions are revoked
            if (context?.let { ContextCompat.checkSelfPermission(it, Manifest.permission.ACCESS_FINE_LOCATION) }
                == PackageManager.PERMISSION_DENIED) {
                Log.e("PermissionChangeReceiver", "Location permission was revoked!")
            }
        }
    }
}
