package com.example.accuratedamoov.ui.home

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.accuratedamoov.data.network.RetrofitClient
import kotlinx.coroutines.launch

@SuppressLint("MissingPermission")
class HomeViewModel(context: Context) : ViewModel() {

    private val _errMsg = MutableLiveData<String>()
    val errMsg: LiveData<String> = _errMsg

    private val api = RetrofitClient.getApiService(context)

    private val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    fun fetchUserProfile() {

        Log.d("HomeViewModel", "Fetching user profile from API")
        val userId = prefs.getInt("user_id", -1)

        if (userId == -1) {
            _errMsg.postValue("User ID missing in SharedPreferences")
            return
        }

        viewModelScope.launch {
            try {
                Log.d("HomeViewModel", "API call for user ID: $userId")
                val response = api.getUserProfile(userId)

                if (response.isSuccessful) {
                    val body = response.body()
                    Log.d("HomeViewModel", "API call for user ID: $userId successful, processing response")

                    if (body?.success == true && body.data != null) {
                        val data = body.data

                        prefs.edit().apply {
                            putInt("user_id", data.id)
                            putString("name", data.name ?: "")
                            putString("email", data.email ?: "")
                            putString("phone", data.phone ?: "")
                            apply()
                        }

                        Log.d("HomeViewModel", "User profile updated from API")
                    } else {
                        _errMsg.postValue(body?.error ?: "Failed to fetch user profile")
                    }

                } else {
                    Log.d("HomeViewModel", "Failed to fetch user profile")
                    _errMsg.postValue("HTTP ${response.code()} - ${response.message()}")
                }

            } catch (e: Exception) {
                _errMsg.postValue("API error: ${e.localizedMessage}")
            }
        }
    }


}

