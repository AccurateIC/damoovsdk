package com.example.accuratedamoov

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.example.accuratedamoov.databinding.ActivityMainBinding
import com.example.accuratedamoov.service.NetworkMonitorService
import com.raxeltelematics.v2.sdk.Settings
import com.raxeltelematics.v2.sdk.TrackingApi
import com.raxeltelematics.v2.sdk.utils.permissions.PermissionsWizardActivity
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var networkReceiver: BroadcastReceiver
    private val trackingApi = TrackingApi.getInstance()
    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        registerNetworkReceiver()
        val settings = Settings(
            Settings.stopTrackingTimeHigh, 150,
            autoStartOn = true,
            hfOn = true,
            elmOn = false
        ).apply {
            stopTrackingTimeout(15)
        }
        trackingApi.initialize(applicationContext, settings)
        if (trackingApi.isInitialized() && trackingApi.areAllRequiredPermissionsAndSensorsGranted()) {
            initializeTrackingSdkAndNavigation()
        } else {
            requestPermissions()
        }
    }

    private fun registerNetworkReceiver() {
        networkReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val isConnected = intent?.getBooleanExtra("isConnected", true) ?: true
                binding.networkOverlay.visibility = if (isConnected) View.GONE else View.VISIBLE
            }
        }
        registerReceiver(networkReceiver, IntentFilter("network_status_changed"))
    }

    private fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        startActivityForResult(
            PermissionsWizardActivity.getStartWizardIntent(
                context = this,
                enableAggressivePermissionsWizard = false,
                enableAggressivePermissionsWizardPage = true
            ),
            PermissionsWizardActivity.WIZARD_PERMISSIONS_CODE
        )
    }

    private fun initializeTrackingSdkAndNavigation() {
        if (!trackingApi.isInitialized()) {
            val settings = Settings(
                Settings.stopTrackingTimeHigh, 150,
                autoStartOn = true,
                hfOn = true,
                elmOn = false
            ).apply {
                stopTrackingTimeout(15)
            }
            trackingApi.initialize(applicationContext, settings)
        }

        setupNavigation()

        if (!trackingApi.isSdkEnabled()) {
            val androidId = android.provider.Settings.Secure.getString(
                contentResolver,
                android.provider.Settings.Secure.ANDROID_ID
            )
            trackingApi.setDeviceID(androidId)
            trackingApi.setEnableSdk(true)
        }

        if (!trackingApi.isTracking()) {
            trackingApi.startTracking()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PermissionsWizardActivity.WIZARD_PERMISSIONS_CODE) {
            if (resultCode == PermissionsWizardActivity.WIZARD_RESULT_ALL_GRANTED) {
                initializeTrackingSdkAndNavigation()
            } else {
                Snackbar.make(
                    binding.root,
                    "All permissions are required to proceed.",
                    Snackbar.LENGTH_LONG
                ).show()
                finish()
            }
        }
    }

    private fun setupNavigation() {
        binding.root.post {
            val navController = try {
                findNavController(R.id.nav_host_fragment_activity_main)
            } catch (e: IllegalStateException) {
                Log.e(TAG, "NavController not found in layout", e)
                return@post
            }

            navController.navigate(R.id.navigation_home)

            binding.navView.setupWithNavController(navController)

            binding.navView.setOnItemSelectedListener { item ->
                val currentDestination = navController.currentDestination?.id
                if (currentDestination == item.itemId) return@setOnItemSelectedListener true

                val navOptions = NavOptions.Builder()
                    .setEnterAnim(R.anim.fragment_enter)
                    .setExitAnim(R.anim.fragment_exit)
                    .setPopEnterAnim(R.anim.fragment_enter)
                    .setPopExitAnim(R.anim.fragment_exit)
                    .build()

                when (item.itemId) {
                    R.id.navigation_dashboard -> navController.navigate(
                        R.id.navigation_dashboard, null, navOptions
                    )
                    R.id.navigation_home -> navController.navigate(
                        R.id.navigation_home, null, navOptions
                    )
                    R.id.navigation_feed -> navController.navigate(
                        R.id.navigation_feed, null, navOptions
                    )
                }
                true
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(networkReceiver)
    }
}


