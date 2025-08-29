package com.example.accuratedamoov.ui.dashboard

import android.app.Application
import android.content.Context
import android.util.Log
import android.widget.Toast
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


    private val _summary = MutableLiveData<SafetySummaryResponse>()
    val summary: LiveData<SafetySummaryResponse> get() = _summary

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> get() = _error

    private val _userProfile = MutableLiveData<UserProfile?>()
    val userProfile: LiveData<UserProfile?> = _userProfile
    private val prefs = appContext.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    private val _errorProfile = MutableLiveData<String?>()
    val errorProfile: LiveData<String?> = _errorProfile

    fun getUserProfile(userId: Int?) {
        viewModelScope.launch {
            try {
                val api = RetrofitClient.getApiService(appContext)
                val response = api.getUserProfile(userId)
                if (response.isSuccessful) {
                    val body = response.body()
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

    fun fetchDashboardData(filter: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val api = RetrofitClient.getProfileApiService(appContext)
                val response = api.getSafetySummary(filter)
                Log.d("API_Response", "Response: $response")

                if (response.isSuccessful) {
                    //Toast.makeText(appContext, response.body()?.data?.safety_score.toString(),Toast.LENGTH_LONG).show()
                    _summary.postValue(response.body())
                } else {
                    _error.postValue("Error: ${response.errorBody()?.string()}")
                    //Toast.makeText(appContext, response.errorBody()?.string(),Toast.LENGTH_LONG).show()

                }
            } catch (e: Exception) {
                _error.postValue("Network error: ${"Oops! We couldn’t connect to the server"}")
                //Toast.makeText(appContext, e.message,Toast.LENGTH_LONG).show()

            }
        }
    }
}
