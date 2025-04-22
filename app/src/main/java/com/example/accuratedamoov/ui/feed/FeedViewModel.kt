package com.example.accuratedamoov.ui.feed

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.accuratedamoov.data.model.TripApiResponse
import com.example.accuratedamoov.data.model.TripData
import com.example.accuratedamoov.data.network.RetrofitClient
import com.example.accuratedamoov.model.TrackModel
import com.raxeltelematics.v2.sdk.TrackingApi
import kotlinx.coroutines.CoroutineScope


import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Response

class FeedViewModel(application: Application) : AndroidViewModel(application) {

    private val trackingApi = TrackingApi.getInstance()
    private val _tracks = MutableStateFlow<List<TripData>>(emptyList())
    val tracks: StateFlow<List<TripData>> = _tracks.asStateFlow()
    @SuppressLint("StaticFieldLeak")
    private val context = application.applicationContext

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
            fetchTrips()
        } catch (e: Exception) {
            Log.e("FeedViewModel", "Error fetching tracks: ${e.message}")
        }
    }

    fun fetchTrips() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response:
                        Response<TripApiResponse> = RetrofitClient.getApiService(context).getTrips()
                if (response.isSuccessful) {
                    val trips = response.body()?.data
                    if (trips != null) {
                        _tracks.value = trips
                    }
                    trips?.forEach {
                        Log.d("TripOmkar", "Trip ID: ${it.UNIQUE_ID}, Distance: ${it.distance_km}")
                    }
                } else {
                    Log.e("API Error", response.errorBody()?.string() ?: "Unknown error")
                }
            } catch (e: Exception) {
                Log.e("Network Error", e.message.toString())
            }
        }
    }


}
