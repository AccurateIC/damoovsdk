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
import com.raxeltelematics.v2.sdk.TrackingApi
import kotlinx.coroutines.CoroutineScope


import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Response
import java.util.UUID

class FeedViewModel(application: Application) : AndroidViewModel(application) {

    private val trackingApi = TrackingApi.getInstance()
    private val _uiState = MutableStateFlow<FeedUiState>(FeedUiState.Loading)
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()
    @SuppressLint("StaticFieldLeak")
    private val context = application.applicationContext

    /*init {
        viewModelScope.launch {
            if(NetworkMonitorService.isConnected == true) {
                loadData()
            }
        }

    }*/

    private var hasLoadedTrips = false

    fun loadTripsIfNeeded() {
        if (!hasLoadedTrips ) {
            viewModelScope.launch {
                fetchTrips()
            }
            hasLoadedTrips = true
        }
    }

    fun isSdkEnabled(): Boolean = trackingApi.isSdkEnabled()

    @SuppressLint("MissingPermission")
    fun enableSdk() {
        trackingApi.setEnableSdk(true)
    }

    private fun loadData() {
        try {
            if(NetworkMonitorService.isConnected == true) {
                fetchTrips()
            }else{
                _uiState.value = FeedUiState.Error("No internet connection")
            }
        } catch (e: Exception) {
            Log.e("FeedViewModel", "Error fetching tracks: ${e.message}")
        }
    }

    fun fetchTrips() {
        viewModelScope.launch((Dispatchers.IO)) {
            try {
                val androidId = getString(context.contentResolver, Secure.ANDROID_ID)
                val deviceId = UUID.nameUUIDFromBytes(androidId.toByteArray()).toString()
                val response:
                        Response<TripApiResponse> = RetrofitClient.getApiService(context).getTripsForDevice(deviceId)
                if (response.isSuccessful) {
                    val trips = response.body()?.data?.sortedByDescending { it.start_date } ?: emptyList()

                    _uiState.value = FeedUiState.Success(trips)
                    trips.forEach {
                        Log.d("Trip", "Trip ID: ${it.UNIQUE_ID}, Distance: ${it.distance_km}")
                    }
                } else {
                    val error = response.errorBody()?.string() ?: "Unknown error"
                    _uiState.value = FeedUiState.Error(error)
                }
            } catch (e: Exception) {
                Log.e("Network Error", e.message.toString())
                _uiState.value = FeedUiState.Error(e.message ?: "Unknown network error")
            }
        }
    }


}
