package com.example.accuratedamoov.service
import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkQuery


class NetworkMonitorService : Service() {

    private lateinit var connectivityManager: ConnectivityManager
    private var isConnected: Boolean? = null
    private val CHANNEL_ID = "network_monitor_service"

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            if (isConnected == false || isConnected == null) { // Notify only if status changes
                isConnected = true
                Log.d("NetworkMonitorService", "Internet is available")
                observeAndCancelWork()
            }
        }

        override fun onLost(network: Network) {
            if (isConnected == true || isConnected == null) { // Notify only if status changes
                isConnected = false
                Log.d("NetworkMonitorService", "No internet connection")
                Toast.makeText(applicationContext,"No internet connection",Toast.LENGTH_SHORT).show()
                observeAndCancelWork()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createSilentNotificationChannel()
        startForeground(1, createSilentNotification()) // Silent foreground service

        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
        observeAndCancelWork()
    }

    override fun onDestroy() {
        super.onDestroy()
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createSilentNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Network Monitor",
                NotificationManager.IMPORTANCE_NONE // No sound, no visual interruption
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    @SuppressLint("NewApi")
    private fun createSilentNotification(): Notification {
        return Notification.Builder(this, CHANNEL_ID)
            .setPriority(Notification.PRIORITY_MIN) // Lowest priority
            .setSmallIcon(android.R.color.transparent) // Transparent icon (invisible)
            .setAutoCancel(true)
            .build()
    }




    private fun observeAndCancelWork() {
        val workManager = WorkManager.getInstance(applicationContext)

        val workQuery = WorkQuery.Builder
            .fromStates(listOf(WorkInfo.State.ENQUEUED))
            .build()

        Handler(Looper.getMainLooper()).post {
            workManager.getWorkInfosLiveData(workQuery).observeForever { workInfos ->
                workInfos?.forEach { workInfo ->
                    val tags = workInfo.tags
                    val workId = workInfo.id

                    Log.d("WorkManager", "ID: $workId")
                    Log.d("WorkManager", "State: ${workInfo.state}")
                    Log.d("WorkManager", "Tags: $tags")

                    // Cancel work if it's NOT TrackTableCheckWorker
                    if (!tags.contains("com.example.accuratedamoov.worker.TrackTableCheckWorker")) {
                        Log.d("WorkManager", "Cancelling Work: $workId")
                        workManager.cancelWorkById(workId)
                    }
                }
            }
        }
    }

}





