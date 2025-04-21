package com.example.accuratedamoov

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.raxeltelematics.v2.sdk.TrackingApi
import com.raxeltelematics.v2.sdk.utils.permissions.PermissionsWizardActivity


/*
@SuppressLint("CustomSplashScreen")
class SplashScreenActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        Handler().postDelayed({
            startActivity(Intent(this@SplashScreenActivity, MainActivity::class.java))
            finish()
        }, 2000) // 2-second delay
    }
}*/


@SuppressLint("CustomSplashScreen")
open class SplashScreenActivity : AppCompatActivity() {

    private val TAG = "SplashScreenActivity"
    public val trackingApi = TrackingApi.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        Handler(Looper.getMainLooper()).postDelayed({
            checkPermissionsAndContinue()
        }, 1000) // Slight delay for splash
    }

    public fun checkPermissionsAndContinue() {
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
            navigateToMain()
        }
    }

    public open fun allPermissionGranted() = trackingApi.areAllRequiredPermissionsAndSensorsGranted()

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
                    navigateToMain()
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
}
