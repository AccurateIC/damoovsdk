package com.example.accuratedamoov


import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.example.accuratedamoov.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar
import com.raxeltelematics.v2.sdk.Settings
import com.raxeltelematics.v2.sdk.TrackingApi
import com.raxeltelematics.v2.sdk.utils.permissions.PermissionsWizardActivity

import java.util.UUID


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val TAG: String = this::class.java.simpleName+"Omkar"
    private val trackingApi = TrackingApi.getInstance()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        checkPermissionsAndStartTracking()
    }

    private fun checkPermissionsAndStartTracking() {
        if (!trackingApi.areAllRequiredPermissionsAndSensorsGranted()) {
            Log.d(TAG, "Permissions not granted, launching wizard.")
            startActivityForResult(
                PermissionsWizardActivity.getStartWizardIntent(
                    context = this,
                    enableAggressivePermissionsWizard = false,
                    enableAggressivePermissionsWizardPage = true
                ), PermissionsWizardActivity.WIZARD_PERMISSIONS_CODE
            )
        } else {
            Log.d(TAG, "Permission granted")
            enableTracking()
            setupNavigation()
        }
    }

    private fun setupNavigation() {
        binding.root.post {
            val navController = try {
                findNavController(R.id.nav_host_fragment_activity_main)
            } catch (e: IllegalStateException) {
                Log.e(
                    TAG,
                    "NavController not found. Ensure the nav_host_fragment exists in the layout.",
                    e
                )
                return@post
            }

            /* val appBarConfiguration = AppBarConfiguration(
                 setOf(
                     R.id.navigation_home, R.id.navigation_feed, R.id.navigation_settings
                 )
             )

             //setupActionBarWithNavController(navController, appBarConfiguration)*/
            binding.navView.setupWithNavController(navController)
            binding.navView.setOnItemSelectedListener { item ->
                val currentDestination = navController.currentDestination?.id
                if (currentDestination == item.itemId) {
                    // If already on the selected tab, do nothing
                    return@setOnItemSelectedListener true
                }
                val navOptions =
                    NavOptions.Builder().setEnterAnim(R.anim.fragment_enter)  // Slide in
                        .setExitAnim(R.anim.fragment_exit)  // Slide out
                        .setPopEnterAnim(R.anim.fragment_enter).setPopExitAnim(R.anim.fragment_exit)
                        .build()

                when (item.itemId) {
                    R.id.navigation_home -> navController.navigate(
                        R.id.navigation_home, null, navOptions
                    )

                    R.id.navigation_feed -> navController.navigate(
                        R.id.navigation_feed, null, navOptions
                    )

                    R.id.navigation_settings -> navController.navigate(
                        R.id.navigation_settings, null, navOptions
                    )
                }
                true
            }
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PermissionsWizardActivity.WIZARD_PERMISSIONS_CODE) {
            when (resultCode) {
                PermissionsWizardActivity.WIZARD_RESULT_ALL_GRANTED -> {
                    Log.d(TAG, "onActivityResult: WIZARD_RESULT_ALL_GRANTED")
                    enableTracking()
                    setupNavigation()
                }

                PermissionsWizardActivity.WIZARD_RESULT_NOT_ALL_GRANTED -> {
                    Log.d(TAG, "onActivityResult: WIZARD_RESULT_NOT_ALL_GRANTED")
                    Snackbar.make(
                        binding.root, "All permissions were not granted", Snackbar.LENGTH_LONG
                    ).show()
                }

                PermissionsWizardActivity.WIZARD_RESULT_CANCELED -> {
                    Log.d(TAG, "onActivityResult: WIZARD_RESULT_CANCELED")
                    Snackbar.make(binding.root, "Wizard cancelled", Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }


    @SuppressLint("HardwareIds")
    private fun enableTracking() {
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Snackbar.make(
                binding.root, "Please grant all required permissions ", Snackbar.LENGTH_LONG
            ).show()
            return
        }
        val settings = Settings(
            Settings.stopTrackingTimeHigh, 150, autoStartOn = true, hfOn = true, elmOn = false
        )
        settings.stopTrackingTimeout(10)
        trackingApi.initialize(applicationContext, settings)
            val androidId = android.provider.Settings.Secure.getString(
                contentResolver,
                android.provider.Settings.Secure.ANDROID_ID
            )
            if(!trackingApi.isSdkEnabled()) {
                // for tracking 2.2.63
                trackingApi.setDeviceID(androidId)
                // for tracking 3.0.0
                /* trackingApi.setDeviceID(
                     UUID.nameUUIDFromBytes(androidId.toByteArray(Charsets.UTF_8)).toString()
                 )*/
                trackingApi.setEnableSdk(true)
                Log.d(TAG,"tracking SDK enabled")

            // for tracking 3.0.0,not present in 2.2.263
            //trackingApi.setAutoStartEnabled(true,true)
            /*if(!trackingApi.isTracking()) {
                trackingApi.startTracking()
            }*/
        }


    }


}


