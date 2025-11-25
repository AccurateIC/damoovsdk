package com.example.accuratedamoov.ui.register

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.accuratedamoov.data.model.RegisterModel
import com.example.accuratedamoov.data.model.RegisterResponse
import com.example.accuratedamoov.data.network.RetrofitClient
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.HttpException
import retrofit2.Response
import java.util.UUID

class RegisterViewModel(application: Application) : AndroidViewModel(application) {

    val registerResult = MutableLiveData<Result<RegisterResponse>>()
    private val mContext: Context = application.applicationContext
    private val apiService = RetrofitClient.getApiService(mContext)
    private val prefs by lazy {
        mContext.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    }

    fun registerUser(email: String, password: String, name: String?) {
        // Generate unique device ID
        val androidId = Settings.Secure.getString(mContext.contentResolver, Settings.Secure.ANDROID_ID)
        val deviceId = UUID.nameUUIDFromBytes(androidId.toByteArray()).toString()
        val deviceName = getDeviceName()

        // Build request model
        val model = RegisterModel(
            email = email,
            password = password,
            name = name,
            device_id = deviceId,
            device_name = deviceName
        )

        // Initialize EncryptedSharedPreferences
        val masterKey = MasterKey.Builder(mContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val enprefs = EncryptedSharedPreferences.create(
            mContext,
            "user_creds",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        Log.d("RegisterViewModel", "Registering user with model: $model")

        // Launch coroutine in ViewModel scope
        viewModelScope.launch {
            runCatching {
                apiService.registerUserWithDevice(model)
            }.onSuccess { res ->
                if (res.success) {
                    // Success: post value and save credentials
                    registerResult.postValue(Result.success(res))
                    enprefs.edit().apply {
                        model.email?.let { putString("user_email", it) }
                        model.password?.let { putString("user_password", it) }
                        res.user_id?.let { id ->
                            prefs.edit { putInt("user_id", id)}
                            Log.d("RegisterUserApi", "Saved user_id: $id in SharedPreferences")
                        }
                        apply()
                    }
                } else {
                    // API returned error
                    registerResult.postValue(Result.failure(Throwable(res.error ?: "Registration failed")))
                }
            }.onFailure { throwable ->
                // Handle exceptions (HTTP, network, etc.)
                val errorMsg = when (throwable) {
                    is HttpException -> {
                        try {
                            JSONObject(throwable.response()?.errorBody()?.string() ?: "")
                                .optString("error", throwable.message())
                        } catch (ex: Exception) {
                            throwable.message
                        }
                    }
                    else -> throwable.message
                } ?: "Unknown error"
                registerResult.postValue(Result.failure(Throwable(errorMsg)))
            }
        }
    }

    private fun getDeviceName(): String {
        val manufacturer = Build.MANUFACTURER
        val model = Build.MODEL
        return if (model.startsWith(manufacturer, ignoreCase = true)) model else "$manufacturer $model"
    }
}

