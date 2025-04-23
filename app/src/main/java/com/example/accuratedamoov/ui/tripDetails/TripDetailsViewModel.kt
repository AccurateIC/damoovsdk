package com.example.accuratedamoov.ui.tripDetails



import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.accuratedamoov.data.network.RetrofitClient
import com.example.accuratedamoov.model.GeoPointModel
import kotlinx.coroutines.launch

class TripDetailsViewModel(application: Application) : AndroidViewModel(application) {

    private val _geoPoints = MutableLiveData<List<GeoPointModel>>()
    val geoPoints: LiveData<List<GeoPointModel>> get() = _geoPoints
    @SuppressLint("StaticFieldLeak")
    private val mContext:Context = application.applicationContext
    private val apiService = RetrofitClient.getApiService(mContext)

    // Function to fetch geo points based on the unique ID
    fun fetchGeoPoints(uniqueId: String) {
        viewModelScope.launch {
            try {
                val response = apiService.getGeoPoints(uniqueId)
                handleResponse(response)
            } catch (e: Exception) {

            }
        }
    }

    private fun handleResponse(responseList: List<GeoPointModel>) {
        if (responseList.isNotEmpty()) {
            _geoPoints.value = responseList
        } else {
            _geoPoints.value = mutableListOf()
        }
    }
}
