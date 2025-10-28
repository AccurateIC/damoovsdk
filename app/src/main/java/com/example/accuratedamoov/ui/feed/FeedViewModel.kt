package com.example.accuratedamoov.ui.feed

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.provider.Settings
import android.provider.Settings.*
import android.provider.Settings.Secure.getString
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.accuratedamoov.data.model.TripApiResponse
import com.example.accuratedamoov.data.model.TripData
import com.example.accuratedamoov.data.network.RetrofitClient
import com.example.accuratedamoov.model.FeedUiState
import com.example.accuratedamoov.model.TrackModel
import com.example.accuratedamoov.service.NetworkMonitorService
import kotlinx.coroutines.CoroutineScope


import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Response
import java.util.UUID
import androidx.core.content.edit
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.telematicssdk.tracking.TrackingApi
import kotlinx.coroutines.withContext

class FeedViewModel(application: Application) : AndroidViewModel(application) {

    private val trackingApi = TrackingApi.getInstance()
    private val _uiState = MutableStateFlow<FeedUiState>(FeedUiState.Loading)
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()


    private val _lastTrip = MutableLiveData<TripData>()
    val lastTrip: LiveData<TripData> = _lastTrip

    @SuppressLint("StaticFieldLeak")
    private val context = application.applicationContext

    private var hasLoadedTrips = false

    fun loadTripsIfNeeded() {
        if (!hasLoadedTrips) {
            viewModelScope.launch {
                fetchTrips()
                hasLoadedTrips = true // ✅ set this only after fetchTrips is complete
            }
        }
    }

    fun isSdkEnabled(): Boolean = trackingApi.isSdkEnabled()

    @SuppressLint("MissingPermission")
    fun enableSdk() {
        trackingApi.setEnableSdk(true)
    }

    fun fetchTrips() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val androidId = Settings.Secure.getString(
                    context.contentResolver,
                    Settings.Secure.ANDROID_ID
                )
                val deviceId = UUID.nameUUIDFromBytes(androidId.toByteArray()).toString()

                val sharedPrefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                val userId = sharedPrefs.getInt("user_id", 0)

                if (userId == 0) {
                    _uiState.value = FeedUiState.Error("User ID not found")
                    return@launch
                }

                val response = RetrofitClient.getApiService(context)
                    .getTripsForDevice(userId)

                if (response.isSuccessful) {
                    Log.d("tripcounts", response.body()?.data?.size.toString())

                    val trips = response.body()?.data
                        ?.filter { it.distance_km >= 1 }
                        ?.distinctBy { it.unique_id }
                        ?.sortedByDescending { it.start_date_ist }
                        ?: emptyList()

                    _uiState.value = FeedUiState.Success(trips)
                    withContext(Dispatchers.Main) {
                        if (trips.isNotEmpty()) {
                            _lastTrip.value = trips.first()
                        }
                    }

                    val sharedPref =
                        context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

                    val tripCount = trips.size
                    val totalDistanceKm = trips.sumOf { it.distance_km }

                    sharedPref.edit {
                        putInt("trip_count", tripCount)
                            .putInt("total_distance", totalDistanceKm.toInt())
                    }

                    Log.d(
                        "SharedPref",
                        "Saved Trip Count: $tripCount, Distance: $totalDistanceKm km"
                    )

                } else {
                    val error = response.errorBody()?.string() ?: "Unknown error"
                    _uiState.value = FeedUiState.Error(error)
                }
            } catch (e: Exception) {
                Log.e("FeedViewModel", "Oops! We couldn’t connect to the server")
                _uiState.value = FeedUiState.Error("Oops! We couldn’t connect to the server")
            }
        }
    }

    fun getLastTrip(): TripData? {
        val currentState = _uiState.value
        Log.d("LastTrip", "Current UI State: $currentState")
        return if (currentState is FeedUiState.Success && currentState.trips.isNotEmpty()) {
            currentState.trips.first()
        } else null
    }


    fun refreshLastTrip() {
        viewModelScope.launch(Dispatchers.IO) {
            val latestTrip = getLastTrip()
            withContext(Dispatchers.Main) {
                if (latestTrip != null) {
                    _lastTrip.value = latestTrip
                } else {
                    Log.w("FeedViewModel", "No last trip found when refreshing")
                }
            }
        }
    }
}


