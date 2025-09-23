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
        private const val PREFS_NAME = "user_prefs"
        private const val KEY_CLOUD_URL = "api_url"
        private const val KEY_SCORE_URL = "score_url"
    }

    private val prefs = application.getSharedPreferences(PREFS_NAME, Application.MODE_PRIVATE)

    private val _cloudUrl = MutableLiveData<String>()
    val cloudUrl: LiveData<String> get() = _cloudUrl

    private val _scoreUrl = MutableLiveData<String>() // new
    val scoreUrl: LiveData<String> get() = _scoreUrl

    private val _message = MutableLiveData<String>()
    val message: LiveData<String> get() = _message

    init {
        _cloudUrl.value = prefs.getString(KEY_CLOUD_URL, "") ?: ""
        _scoreUrl.value = prefs.getString(KEY_SCORE_URL, "") ?: "" // new
    }

    fun saveCloudUrl(url: String) {
        if (url.isBlank() || !Patterns.WEB_URL.matcher(url).matches()) {
            _message.value = "Please enter a valid Cloud URL"
            return
        }

        val formattedUrl = if (!url.endsWith("/")) "$url/" else url

        viewModelScope.launch(Dispatchers.IO) {
            val healthOk = checkHealth(formattedUrl)
            if (healthOk) {
                prefs.edit().putString(KEY_CLOUD_URL, formattedUrl).apply()
                _cloudUrl.postValue(formattedUrl)
                _message.postValue("Success! Cloud URL saved")
            } else {
                _message.postValue("Cloud server unavailable. Try again later.")
            }
        }
    }

    fun saveScoreUrl(url: String) { // simple save
        if (url.isBlank() || !Patterns.WEB_URL.matcher(url).matches()) {
            _message.value = "Please enter a valid Score URL"
            return
        }

        val formattedUrl = if (!url.endsWith("/")) "$url/" else url

        prefs.edit().putString(KEY_SCORE_URL, formattedUrl).apply()
        _scoreUrl.value = formattedUrl
        _message.value = "Success! Score URL saved"
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
