package com.example.accuratedamoov.worker

// SystemEventSyncWorker.kt

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.accuratedamoov.data.local.systemevents.SystemEventDatabase
import com.example.accuratedamoov.data.local.systemevents.SystemEventEntity
import com.example.accuratedamoov.data.model.SystemEventRequest
import com.example.accuratedamoov.data.network.ApiService
import com.example.accuratedamoov.data.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class SystemEventSyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val db = SystemEventDatabase.getInstance(context)
    private val dao = db.systemEventDao()
    private val api = RetrofitClient.getApiService(context)

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d("SystemEventSync", "Worker started")

            val pendingEvents = dao.getPendingEvents()
            if (pendingEvents.isEmpty()) {
                Log.d("SystemEventSync", "No pending events to sync")
                SystemEventScheduler.scheduleSystemEvent(applicationContext)
                return@withContext Result.success()
            }

            val syncedEvents = mutableListOf<SystemEventEntity>()

            for (event in pendingEvents) {
                val request = SystemEventRequest(
                    device_id = event.device_id,
                    user_id = event.user_id,
                    event_message = event.event_message,
                    event_type = event.event_type,
                    timestamp = event.timestamp
                )

                try {
                    val response = api.logSystemEvent(request)
                    if (response.isSuccessful) {
                        Log.d("SystemEventSync", "✅ Synced event ID: ${event.id}")
                        syncedEvents.add(event.copy(synced = true))
                    } else {
                        Log.e(
                            "SystemEventSync",
                            "❌ Failed to sync event ID: ${event.id}, code=${response.code()}"
                        )
                    }
                } catch (e: Exception) {
                    Log.e("SystemEventSync", "⚠️ Exception syncing event ID: ${event.id}", e)
                }
            }

            if (syncedEvents.isNotEmpty()) {
                dao.updateEvents(syncedEvents)
                Log.d(
                    "SystemEventSync",
                    "Run → Marked ${syncedEvents.size} events as synced (kept in DB)"
                )
            }

            SystemEventScheduler.scheduleSystemEvent(applicationContext)
            Result.success()

        } catch (e: Exception) {
            Log.e("SystemEventSync", "❌ Worker error", e)
            SystemEventScheduler.scheduleSystemEvent(applicationContext)
            Result.retry()
        }
    }
}

