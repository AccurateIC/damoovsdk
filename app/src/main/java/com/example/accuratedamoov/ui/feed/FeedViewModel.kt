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
    private val context = application.applicationContext

    private val _uiState = MutableStateFlow<FeedUiState>(FeedUiState.Loading)
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

    private val _lastTrip = MutableLiveData<TripData>()
    val lastTrip: LiveData<TripData> get() = _lastTrip

    private var hasLoadedTrips = false

    fun loadTripsIfNeeded() {
        if (!hasLoadedTrips) {
            viewModelScope.launch {
                fetchTrips()
                hasLoadedTrips = true
            }
        }
    }

    fun isSdkEnabled() = trackingApi.isSdkEnabled()

    @SuppressLint("MissingPermission")
    fun enableSdk() {
        trackingApi.setEnableSdk(true)
    }

    fun fetchTrips() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val userId = getUserId()
                if (userId == 0) {
                    _uiState.value = FeedUiState.Error("User ID not found")
                    return@launch
                }

                val response = RetrofitClient.getApiService(context).getTripsForDevice(userId)
                if (response.isSuccessful) {
                    val trips = response.body()?.data
                        ?.filter { it.distance_km >= 1 }
                        ?.distinctBy { it.unique_id }
                        ?.sortedByDescending { it.start_date_ist }
                        ?: emptyList()

                    _uiState.value = FeedUiState.Success(trips)
                    updateLastTrip(trips)
                    saveTripSummary(trips)
                } else {
                    val error = response.errorBody()?.string() ?: "Unknown error"
                    _uiState.value = FeedUiState.Error(error)
                }

            } catch (e: Exception) {
                Log.e("FeedViewModel", "Error fetching trips: ${e.message}")
                _uiState.value = FeedUiState.Error("Unable to connect to server")
            }
        }
    }

    private suspend fun updateLastTrip(trips: List<TripData>) {
        withContext(Dispatchers.Main) {
            if (trips.isNotEmpty()) _lastTrip.value = trips.first()
        }
    }

    private fun saveTripSummary(trips: List<TripData>) {
        val sharedPrefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val tripCount = trips.size
        val totalDistance = trips.sumOf { it.distance_km }.toInt()

        sharedPrefs.edit {
            putInt("trip_count", tripCount)
            putInt("total_distance", totalDistance)
        }

        Log.d("FeedViewModel", "Trip summary saved: count=$tripCount, distance=$totalDistance km")
    }

    private fun getUserId(): Int {
        val sharedPrefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        return sharedPrefs.getInt("user_id", 0)
    }

    fun getLastTrip(): TripData? {
        val currentState = _uiState.value
        return if (currentState is FeedUiState.Success && currentState.trips.isNotEmpty()) {
            currentState.trips.first()
        } else null
    }

    fun refreshLastTrip() {
        viewModelScope.launch(Dispatchers.IO) {
            getLastTrip()?.let {
                withContext(Dispatchers.Main) {
                    _lastTrip.value = it
                }
            } ?: Log.w("FeedViewModel", "No last trip available")
        }
    }
}


