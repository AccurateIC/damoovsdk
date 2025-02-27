package com.example.accuratedamoov.ui.feed

import android.annotation.SuppressLint
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.accuratedamoov.model.TrackModel
import com.raxeltelematics.v2.sdk.TrackingApi
import com.raxeltelematics.v2.sdk.server.model.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FeedViewModel : ViewModel() {

    private val trackingApi = TrackingApi.getInstance()
    private val _tracks = MutableStateFlow<List<TrackModel>>(emptyList())
    val tracks: StateFlow<List<TrackModel>> = _tracks.asStateFlow()

    init {
        viewModelScope.launch {
            loadData()
        }

    }

    fun isSdkEnabled(): Boolean = trackingApi.isSdkEnabled()

    @SuppressLint("MissingPermission")
    fun enableSdk() {
        trackingApi.setEnableSdk(true)
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
            val result = trackingApi.getTracks(locale = Locale.EN, offset = 0, limit = 10)
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
        }
    }
}
