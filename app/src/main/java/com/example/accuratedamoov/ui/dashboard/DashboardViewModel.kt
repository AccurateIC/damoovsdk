package com.example.accuratedamoov.ui.dashboard

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.accuratedamoov.data.model.TripSummaryResponse
import com.example.accuratedamoov.data.model.UserProfileResponse
import com.example.accuratedamoov.data.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DashboardViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val _userProfile = MutableStateFlow<UserProfileResponse?>(null)
    val userProfile = _userProfile.asStateFlow()
    private val apiService = RetrofitClient.getApiService(application.applicationContext)
    private val _tripSummary = MutableStateFlow<TripSummaryResponse?>(null)
    val tripSummary = _tripSummary.asStateFlow()

    fun loadDashboardData(userId: Int, deviceId: String) {
        viewModelScope.launch {
            Log.d("DashboardViewModel", "Calling API for userId=$userId, deviceId=$deviceId")

            try {
                val userResponse = apiService.getUserProfile(userId.toString())
                if (userResponse.isSuccessful) {
                    Log.d("DashboardViewModel", "User profile loaded successfully")

                    _userProfile.value = userResponse.body()
                }else{
                    Log.e("DashboardViewModel", "Failed to load")
                }

                /*val tripResponse = apiService.getTripSummary(deviceId, userId.toString())
                if (tripResponse.isSuccessful) {
                    Log.d("DashboardViewModel", "trips loaded successfully")
                    Log.d("tripresponse", tripResponse.body().toString())
                    _tripSummary.value = tripResponse.body()
                }else
                {
                    Log.e("DashboardViewModel", "Failed to load trip summary: ${tripResponse.message()}")
                }
*/
            } catch (e: Exception) {
                Log.e("DashboardViewModel", "Error loading dashboard data", e)
            }
        }
    }
}

