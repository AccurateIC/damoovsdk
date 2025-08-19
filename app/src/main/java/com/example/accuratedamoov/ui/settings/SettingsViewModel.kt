package com.example.accuratedamoov.ui.settings


import android.app.Application
import android.util.Patterns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.accuratedamoov.data.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Response

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val PREFS_NAME = "appSettings"
        private const val KEY_CLOUD_URL = "api_url"
    }

    private val prefs = application.getSharedPreferences(PREFS_NAME, Application.MODE_PRIVATE)

    private val _cloudUrl = MutableLiveData<String>()
    val cloudUrl: LiveData<String> get() = _cloudUrl

    private val _message = MutableLiveData<String>()
    val message: LiveData<String> get() = _message

    init {
        _cloudUrl.value = prefs.getString(KEY_CLOUD_URL, "") ?: ""
    }

    fun saveCloudUrl(url: String) {
        if (url.isBlank() || !Patterns.WEB_URL.matcher(url).matches()) {
            _message.value = "Please enter a valid URL"
            return
        }

        val formattedUrl = if (!url.endsWith("/")) "$url/" else url

        // Check health endpoint before saving permanently
        viewModelScope.launch(Dispatchers.IO) {
            val healthOk = checkHealth(formattedUrl)
            if (healthOk) {
                prefs.edit().putString(KEY_CLOUD_URL, formattedUrl).apply()
                _cloudUrl.postValue(formattedUrl)
                _message.postValue("Success! Cloud URL saved")
                //// logout current user and navigate to login screen
            } else {
                _message.postValue("Looks like the server is unavailable. Please try saving the URL again later.")
            }
        }
    }

    private suspend fun checkHealth(baseUrl: String): Boolean {
        return try {
            val api = RetrofitClient.getApiService(baseUrl)
            val response: Response<Void> = api.checkHealth()
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }
}
