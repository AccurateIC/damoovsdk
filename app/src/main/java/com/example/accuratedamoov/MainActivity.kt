package com.example.accuratedamoov

import android.Manifest
import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.example.accuratedamoov.broadcastreceiver.SystemChangeReceiver
import com.example.accuratedamoov.database.DatabaseHelper
import com.example.accuratedamoov.databinding.ActivityMainBinding
import com.example.accuratedamoov.service.NetworkMonitorService
import com.example.accuratedamoov.service.PermissionMonitorService
import com.example.accuratedamoov.worker.SystemEventScheduler

import com.google.android.material.snackbar.Snackbar
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.telematicssdk.tracking.TrackingApi
import com.telematicssdk.tracking.utils.permissions.PermissionsWizardActivity
import java.util.UUID

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val trackingApi = TrackingApi.getInstance()
    private val TAG = "MainActivity"
    private var isNavigationSetup = false

    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var networkCallback: ConnectivityManager.NetworkCallback
    private lateinit var systemChangeReceiver: SystemChangeReceiver
    private var networkSnackbar: Snackbar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }

        setupNetworkMonitoring()
        //startPermissionMonitorIfNeeded()
        setupTrackingIfReady()
        /*FirebaseCrashlytics.getInstance().log("Forcing a test crash")
        throw RuntimeException("Test Crash")*/
        systemChangeReceiver = SystemChangeReceiver()

    }

    // ----------------------------
    // Network Monitoring
    // ----------------------------
    private fun setupNetworkMonitoring() {
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                runOnUiThread {
                   dismissNetworkSnackbar()
                }
            }

            override fun onLost(network: Network) {
                runOnUiThread {
                    showNetworkSnackbar()
                }
            }

        }
        // Register network callback safely
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            connectivityManager.registerDefaultNetworkCallback(networkCallback)
        } else {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager.registerNetworkCallback(request, networkCallback)
        }

        // Show Snackbar immediately if no network at startup
        if (!isNetworkAvailable()) {
            binding.root.post {
                networkSnackbar = Snackbar.make(
                    binding.root,
                    "No internet connection. Waiting to reconnect...",
                    Snackbar.LENGTH_INDEFINITE
                )
                networkSnackbar?.show()
            }
        }
    }


    private fun startPermissionMonitorIfNeeded() {
        if (!isServiceRunning(PermissionMonitorService::class.java)) {
            val intent = Intent(this, PermissionMonitorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(this, intent)
            } else {
                startService(intent)
            }
        }
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


    private fun setupTrackingIfReady() {

        if (trackingApi.isInitialized() && trackingApi.areAllRequiredPermissionsAndSensorsGranted()) {
            enableTrackingAndNavigation()
        } else {
            requestPermissions()
        }
    }


    private fun enableTrackingAndNavigation() {
        if (!trackingApi.isSdkEnabled()) {
            val androidId = android.provider.Settings.Secure.getString(
                contentResolver,
                android.provider.Settings.Secure.ANDROID_ID
            )

// Convert to UUID format
            val deviceId = UUID.nameUUIDFromBytes(androidId.toByteArray()).toString()

            trackingApi.setDeviceID(deviceId)
            trackingApi.setEnableSdk(true)
            trackingApi.setAutoStartEnabled(true, true)
            if (!trackingApi.isTracking()) {
                trackingApi.startTracking()
            }


        }
        if (trackingApi.isSdkEnabled()) {
            if (!::systemChangeReceiver.isInitialized) {
                systemChangeReceiver = SystemChangeReceiver()
                val filter = IntentFilter().apply {
                    addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
                    addAction(Intent.ACTION_PACKAGE_REPLACED)
                    addAction(LocationManager.MODE_CHANGED_ACTION)
                }
                registerReceiver(systemChangeReceiver, filter)
                SystemEventScheduler.scheduleSystemEvent(this)

            }
        }

        if (!isNavigationSetup) {
            setupNavigation()
            isNavigationSetup = true
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
                        R.id.navigation_dashboard,
                        null,
                        navOptions
                    )

                    R.id.navigation_home -> navController.navigate(
                        R.id.navigation_home,
                        null,
                        navOptions
                    )

                    R.id.navigation_feed -> navController.navigate(
                        R.id.navigation_feed,
                        null,
                        navOptions
                    )

                    R.id.navigation_profile -> navController.navigate(
                        R.id.navigation_profile,
                        null,
                        navOptions
                    )

                    R.id.navigation_settings -> navController.navigate(
                        R.id.navigation_settings,
                        null,
                        navOptions
                    )
                }
                true
            }
        }
    }


    override fun onResume() {
        super.onResume()
        setupTrackingIfReady()
        checkTracks()
    }

    private fun checkTracks() {
        val dbHelper = DatabaseHelper.getInstance(applicationContext)

        if (dbHelper.hasMultipleTripsWithReasons()) {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(
                    applicationContext,
                    "Trips are recorded, waiting for sync",
                    Toast.LENGTH_LONG
                ).show()
            }
        } else {
            Toast.makeText(
                applicationContext,
                "No trips with reasons found in the database",
                Toast.LENGTH_LONG
            ).show()

            Log.d(TAG, "")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PermissionsWizardActivity.WIZARD_PERMISSIONS_CODE) {
            if (resultCode == PermissionsWizardActivity.WIZARD_RESULT_ALL_GRANTED) {
                enableTrackingAndNavigation()
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


    @Suppress("DEPRECATION")
    private fun isServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        // getRunningServices is deprecated but still works for own app services
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finishAffinity()
    }

    private fun ensureApiConfigured(): Boolean {
        val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val apiUrl = prefs.getString("api_url", null)
        return !apiUrl.isNullOrEmpty()
    }

     fun isNetworkAvailable(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }


    private fun showNetworkSnackbar() {
        binding.root.post {
            networkSnackbar = Snackbar.make(
                binding.root,
                "No internet connection. Waiting to reconnect...",
                Snackbar.LENGTH_INDEFINITE
            )
            networkSnackbar?.anchorView = binding.navView
            networkSnackbar?.show()
        }
    }

    private fun dismissNetworkSnackbar() {
        networkSnackbar?.dismiss()
        networkSnackbar = null
    }


}


