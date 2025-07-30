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
                val sharedPrefs = mContext.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                val userId = sharedPrefs.getInt("user_id", 0)
                val response = apiService.getGeoPoints(uniqueId,userId)
                if (response.isSuccessful) {
                    response.body()?.data?.let { geoPoints ->
                        handleResponse(geoPoints.toList())
                    } ?: run {
                        Log.e("TripDetailsViewModel", "Response body is null")
                    }
                }else{
                    Log.e("TripDetailsViewModel",response.message())
                    _geoPoints.value = mutableListOf()
                }
            } catch (e: Exception) {
                Log.e("TripDetailsViewModel",e.message.toString())
                _geoPoints.value = mutableListOf()
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
