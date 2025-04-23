package com.example.accuratedamoov

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.example.accuratedamoov.service.NetworkMonitorService
import com.google.android.material.snackbar.Snackbar
import com.raxeltelematics.v2.sdk.TrackingApi
import com.raxeltelematics.v2.sdk.utils.permissions.PermissionsWizardActivity


@SuppressLint("CustomSplashScreen")
open class SplashScreenActivity : AppCompatActivity() {

    private val TAG = "SplashScreenActivity"
    val trackingApi = TrackingApi.getInstance()
    private val networkReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val isConnected = intent?.getBooleanExtra("isConnected", false) ?: false
            if (isConnected) {
                checkPermissionsAndContinue()
            } else {
                Toast.makeText(
                    this@SplashScreenActivity,
                    "Waiting for internet...",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(
                networkReceiver,
                IntentFilter("network_status_changed"),
                RECEIVER_NOT_EXPORTED
            )
        } else {
            registerReceiver(networkReceiver, IntentFilter("network_status_changed"))
        }
        Handler(Looper.getMainLooper()).postDelayed({
            checkPermissionsAndContinue()
        }, 1000)


    }

    private fun checkPermissionsAndContinue() {
        if (!allPermissionGranted()) {
            Log.d(TAG, "Permissions not granted, launching wizard.")
            startActivityForResult(
                PermissionsWizardActivity.getStartWizardIntent(
                    context = this,
                    enableAggressivePermissionsWizard = false,
                    enableAggressivePermissionsWizardPage = true
                ), PermissionsWizardActivity.WIZARD_PERMISSIONS_CODE
            )
        } else {
            Log.d(TAG, "All permissions granted, navigating to MainActivity")
            if (NetworkMonitorService.isConnected == true) {
                navigateToMain()
            } else {
                Snackbar.make(
                    findViewById(android.R.id.content),
                    "No internet, Try again",
                    Snackbar.LENGTH_LONG
                ).show()

            }
        }
    }

    open fun allPermissionGranted() = trackingApi.areAllRequiredPermissionsAndSensorsGranted()

    private fun navigateToMain() {
        startActivity(Intent(this@SplashScreenActivity, MainActivity::class.java))
        finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PermissionsWizardActivity.WIZARD_PERMISSIONS_CODE) {
            when (resultCode) {
                PermissionsWizardActivity.WIZARD_RESULT_ALL_GRANTED -> {
                    Log.d(TAG, "Permissions granted from wizard")
                    if (NetworkMonitorService.isConnected == true) {
                        navigateToMain()
                    } else {
                        Snackbar.make(
                            findViewById(android.R.id.content),
                            "No internet, Try again",
                            Snackbar.LENGTH_LONG
                        ).show()

                    }
                }

                PermissionsWizardActivity.WIZARD_RESULT_NOT_ALL_GRANTED -> {
                    Log.d(TAG, "Permissions not fully granted")
                    Snackbar.make(
                        findViewById(android.R.id.content),
                        "All permissions are required to proceed.",
                        Snackbar.LENGTH_LONG
                    ).show()
                }

                PermissionsWizardActivity.WIZARD_RESULT_CANCELED -> {
                    Log.d(TAG, "Permissions wizard canceled")
                    Snackbar.make(
                        findViewById(android.R.id.content),
                        "Permission setup canceled.",
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
        }
    }


    override fun onResume() {
        super.onResume()
    }
}
