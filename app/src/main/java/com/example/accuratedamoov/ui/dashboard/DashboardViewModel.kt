package com.example.accuratedamoov.ui.dashboard

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.accuratedamoov.data.model.SafetySummaryResponse
import com.example.accuratedamoov.data.model.UserProfile
import com.example.accuratedamoov.data.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val appContext = application.applicationContext


    private val _summary = MutableLiveData<SafetySummaryResponse?>()
    val summary: LiveData<SafetySummaryResponse?> get() = _summary

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> get() = _error

    private val _userProfile = MutableLiveData<UserProfile?>()
    val userProfile: LiveData<UserProfile?> = _userProfile
    private val prefs = appContext.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    private val _errorProfile = MutableLiveData<String?>()
    val errorProfile: LiveData<String?> = _errorProfile


    init {
        fetchDashboardData(prefs.getInt("user_id", -1))
    }

    fun getUserProfile(userId: Int?) {
        viewModelScope.launch {
            try {
                val api = RetrofitClient.getApiService(appContext)
                val response = userId?.let { api.getUserProfile(it) }
                if (response?.isSuccessful ?: false) {
                    val body = response?.body()
                    if (body != null && body.success && body.data != null) {
                        prefs.edit()
                            .putString("user_name", response.body()?.data?.name)
                            .putString("user_email", response.body()?.data?.email)
                            .apply()
                        _userProfile.postValue(body.data)
                    } else {
                        _errorProfile.postValue("no data found")
                    }
                } else {
                    _errorProfile.postValue("Error: Oops! We couldn’t connect to the server")
                }
            } catch (e: Exception) {
                _errorProfile.postValue(e.message ?: "Unknown error")
            }
        }
    }

    fun fetchDashboardData(user_Id: Int?) {
        Log.d("DashboardVM", "fetchDashboardData() called with user_Id = $user_Id")

        viewModelScope.launch(Dispatchers.IO) {

            try {
                Log.d("DashboardVM", "Creating API service instance...")
                val api = RetrofitClient.getProfileApiService(appContext)

                Log.d("DashboardVM", "Calling getSafetySummary API...")
                val response = api.getSafetySummary(user_Id)

                Log.d("DashboardVM", "API raw response: $response")

                if (response.isSuccessful) {
                    val body = response.body()
                    Log.d("DashboardVM", "API success. Response body: $body")

                    _summary.postValue(body)
                } else {
                    val errorText = response.errorBody()?.string()
                    Log.e("DashboardVM", "API failed. HTTP ${response.code()} | Error: $errorText")

                    _error.postValue("Error: $errorText")
                }

            } catch (e: Exception) {
                Log.e("DashboardVM", "Exception occurred: ${e.message}", e)

                _error.postValue("Network error: Oops! We couldn’t connect to the server")
            }
        }
    }
}
