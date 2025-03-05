package com.example.accuratedamoov.ui.feed

import android.annotation.SuppressLint
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.accuratedamoov.MainApplication
import com.example.accuratedamoov.model.TrackModel
import com.raxeltelematics.v2.sdk.TrackingApi
import com.raxeltelematics.v2.sdk.server.model.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class FeedViewModel : ViewModel() {

    private val trackingApi = MainApplication.getTrackingApi()
    private val _tracks = MutableStateFlow<List<TrackModel>>(emptyList())
    val tracks: StateFlow<List<TrackModel>> = _tracks.asStateFlow()

    init {
        viewModelScope.launch {
            loadData()
        }

    }

    fun isSdkEnabled(): Boolean = trackingApi!!.isSdkEnabled()

    @SuppressLint("MissingPermission")
    fun enableSdk() {
        trackingApi!!.setEnableSdk(true)
    }

    private suspend fun loadData() {
        try {
            fetchTracks()
        } catch (e: Exception) {
            Log.e("FeedViewModel", "Error fetching tracks: ${e.message}")
        }
    }

    private suspend fun fetchTracks() {
        withContext(Dispatchers.IO) {
            // Currently getting zero tracks as getTracks failed due to an API key issue.
            // TODO(): Fetch data from our own REST API instead of using the Damoov API.
            val result = trackingApi!!.getTracks(
                locale = Locale.EN,
                offset = 0,
                limit = 10
            )

            if(result.isNotEmpty()) {
                val trackModels = result?.map {
                    TrackModel(
                        addressStart = it.addressStart,
                        addressEnd = it.addressEnd,
                        endDate = it.endDate,
                        startDate = it.startDate,
                        trackId = it.trackId,
                        accelerationCount = it.accelerationCount,
                        decelerationCount = it.decelerationCount,
                        distance = it.distance,
                        duration = it.duration,
                        rating = it.rating,
                        phoneUsage = it.phoneUsage,
                        originalCode = it.originalCode,
                        hasOriginChanged = it.hasOriginChanged,
                        midOverSpeedMileage = it.midOverSpeedMileage,
                        highOverSpeedMileage = it.highOverSpeedMileage,
                        drivingTips = it.drivingTips,
                        shareType = it.shareType,
                        cityStart = it.cityStart,
                        cityFinish = it.cityFinish
                    )
                } ?: emptyList()

                _tracks.value = trackModels
            }else{
              val trackModels = listOf(
                    TrackModel(
                        addressStart = "123 Main St, New York, NY",
                        addressEnd = "456 Elm St, Los Angeles, CA",
                        endDate = "2025-03-05T18:30:00Z",
                        startDate = "2025-03-05T15:00:00Z",
                        trackId = "track_001",
                        accelerationCount = 12,
                        decelerationCount = 8,
                        distance = 350.5,
                        duration = 12600.0, // in seconds (3.5 hours)
                        rating = 4.5,
                        phoneUsage = 2.0,
                        originalCode = "ORIG_001",
                        hasOriginChanged = false,
                        midOverSpeedMileage = 5.2,
                        highOverSpeedMileage = 1.1,
                        drivingTips = "Avoid sudden braking",
                        shareType = "private",
                        cityStart = "New York",
                        cityFinish = "Los Angeles"
                    ),
                    TrackModel(
                        addressStart = "789 Pine St, Chicago, IL",
                        addressEnd = "321 Oak St, Houston, TX",
                        endDate = "2025-03-06T22:00:00Z",
                        startDate = "2025-03-06T18:45:00Z",
                        trackId = "track_002",
                        accelerationCount = 15,
                        decelerationCount = 10,
                        distance = 450.2,
                        duration = 11700.0, // in seconds (3.25 hours)
                        rating = 4.8,
                        phoneUsage = 0.0,
                        originalCode = "ORIG_002",
                        hasOriginChanged = true,
                        midOverSpeedMileage = 3.8,
                        highOverSpeedMileage = 0.9,
                        drivingTips = "Maintain a steady speed",
                        shareType = "public",
                        cityStart = "Chicago",
                        cityFinish = "Houston"
                    )
                )
                _tracks.value = trackModels
            }

        }
    }


}
