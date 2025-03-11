package com.example.accuratedamoov

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.Manifest
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.accuratedamoov.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar
import com.raxeltelematics.v2.sdk.Settings
import com.raxeltelematics.v2.sdk.TrackingApi
import com.raxeltelematics.v2.sdk.utils.permissions.PermissionsDialogFragment
import com.raxeltelematics.v2.sdk.utils.permissions.PermissionsWizardActivity







class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val TAG: String = this::class.java.simpleName
    private var isTrackingInitialized = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initializeTrackingApi()

        checkPermissionsAndStartTracking()
    }

    private fun checkPermissionsAndStartTracking() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Permissions not granted, launching wizard.")
            startActivityForResult(
                PermissionsWizardActivity.getStartWizardIntent(
                    this,
                    enableAggressivePermissionsWizard = false,
                    enableAggressivePermissionsWizardPage = true
                ), PermissionsWizardActivity.WIZARD_PERMISSIONS_CODE
            )
        } else {
            Log.d(TAG, "Permissions already granted, enabling tracking.")
            enableTracking()
            setupNavigation()
        }
    }

    private fun setupNavigation() {
        // Ensure the fragment is attached before finding NavController
        binding.root.post {
            val navController = try {
                findNavController(R.id.nav_host_fragment_activity_main)
            } catch (e: IllegalStateException) {
                Log.e(TAG, "NavController not found. Ensure the nav_host_fragment exists in the layout.", e)
                return@post
            }

            val appBarConfiguration = AppBarConfiguration(
                setOf(
                    R.id.navigation_home, R.id.navigation_feed, R.id.navigation_settings
                )
            )

            setupActionBarWithNavController(navController, appBarConfiguration)
            binding.navView.setupWithNavController(navController)
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
                    Snackbar.make(binding.root, "All permissions were not granted", Snackbar.LENGTH_LONG).show()
                }
                PermissionsWizardActivity.WIZARD_RESULT_CANCELED -> {
                    Log.d(TAG, "onActivityResult: WIZARD_RESULT_CANCELED")
                    Snackbar.make(binding.root, "Wizard cancelled", Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun initializeTrackingApi() {
        if (isTrackingInitialized) {
            Log.d(TAG, "Tracking API is already initialized, skipping re-initialization.")
            return
        }

        val settings = Settings(
            stopTrackingTimeout = Settings.stopTrackingTimeHigh,
            accuracy = Settings.accuracyHigh,
            autoStartOn = false,
            elmOn = false,
            hfOn = true
        )

        val api = TrackingApi.getInstance()
        api.initialize(this, settings)
        api.setDeviceID(android.provider.Settings.Secure.getString(this.contentResolver, android.provider.Settings.Secure.ANDROID_ID))

        isTrackingInitialized = true
    }

    private fun enableTracking() {
        val api = TrackingApi.getInstance()
        api.setEnableSdk(true)
    }
}
