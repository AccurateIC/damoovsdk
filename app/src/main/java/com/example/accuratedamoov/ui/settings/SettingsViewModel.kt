package com.example.accuratedamoov.ui.settings

import androidx.lifecycle.*

import com.example.accuratedamoov.data.model.DeviceRequest
import com.example.accuratedamoov.data.network.ApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class SettingsViewModel : ViewModel() {

    private val _registrationResult = MutableLiveData<String>()
    val registrationResult: LiveData<String> = _registrationResult

    private var deviceApi: ApiService? = null

    fun initApi(baseUrl: String) {
        deviceApi = Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    fun registerDevice(deviceId: String, deviceName: String) {
        val api = deviceApi ?: return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = api.registerDevice(DeviceRequest(deviceId, deviceName)).execute()
                if (response.isSuccessful) {
                    _registrationResult.postValue("Device registered: ${response.body()?.message}")
                } else {
                    _registrationResult.postValue("Device registration failed: ${response.code()}")
                }
            } catch (e: Exception) {
                _registrationResult.postValue("Error: ${e.localizedMessage}")
            }
        }
    }
}
