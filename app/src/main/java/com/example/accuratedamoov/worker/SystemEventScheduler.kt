package com.example.accuratedamoov.worker

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkQuery
import java.util.concurrent.TimeUnit

object SystemEventScheduler {
    const val EVENTSYNC_TAG = "system_event_sync_tag"
    const val TRACK_TABLE_WORKER_TAG = "track_table_worker_tag"

    fun scheduleSystemEvent(context: Context) {
        val request = OneTimeWorkRequestBuilder<SystemEventSyncWorker>()
            .setInitialDelay(30, TimeUnit.SECONDS)
            .addTag(EVENTSYNC_TAG)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "SystemEventSync",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun scheduleTrackTableCheck(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<TrackTableCheckWorker>()
            .setConstraints(constraints)
            .setInitialDelay(60, TimeUnit.SECONDS)
            .addTag(TRACK_TABLE_WORKER_TAG)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "TrackTableCheckWorker",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    fun observeAndCancelOtherWork(context: Context) {
        val workManager = WorkManager.getInstance(context)

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

                    // Cancel work if it's NOT TrackTableCheckWorker or SystemEventSync
                    if (!tags.contains(TRACK_TABLE_WORKER_TAG) && !tags.contains(EVENTSYNC_TAG)) {
                        workManager.cancelWorkById(workInfo.id)
                    }
                }
            }
        }
    }
}
