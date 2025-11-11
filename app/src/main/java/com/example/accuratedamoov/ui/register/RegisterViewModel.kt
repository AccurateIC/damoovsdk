package com.example.accuratedamoov.ui.register

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.accuratedamoov.data.model.RegisterModel
import com.example.accuratedamoov.data.model.RegisterResponse
import com.example.accuratedamoov.data.network.RetrofitClient
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.UUID

class RegisterViewModel(application: Application) : AndroidViewModel(application) {

    val registerResult = MutableLiveData<Result<RegisterResponse>>()
    @SuppressLint("StaticFieldLeak")
    private val mContext:Context = application.applicationContext
    private val apiService = RetrofitClient.getApiService(mContext)

    fun registerUser(email: String, password: String, name: String?) {
       val androidId = Settings.Secure.getString(mContext.contentResolver, Settings.Secure.ANDROID_ID)
        val deviceId = UUID.nameUUIDFromBytes(androidId.toByteArray()).toString()
        val deviceName = getDeviceName()

        val model = RegisterModel(
            email = email,
            password = password,
            name = name,
            device_id = deviceId,
            device_name = deviceName
        )
// ✅ Initialize EncryptedSharedPreferences
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

        Log.d("RegisterViewModel", "Registering user with model: ${model.toString()}")
        apiService.registerUserWithDevice(model).enqueue(object : Callback<RegisterResponse> {
            override fun onResponse(call: Call<RegisterResponse>, response: Response<RegisterResponse>) {
                val res = response.body()
                if (res != null && res.success) {
                    registerResult.postValue(Result.success(res))
                    enprefs.edit().apply {
                        putString("user_email", email)
                        putString("user_password", password)
                        apply()
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    // Try to parse JSON error
                    val errorMsg = try {
                        JSONObject(errorBody ?: "").optString("error", "Unknown error")
                    } catch (e: Exception) {
                        "Unknown error"
                    }
                    registerResult.postValue(Result.failure(Throwable(errorMsg)))
                }
            }

            override fun onFailure(call: Call<RegisterResponse>, t: Throwable) {
                registerResult.postValue(Result.failure(t))
            }
        })
    }

    private fun getDeviceName(): String {
        val manufacturer = Build.MANUFACTURER
        val model = Build.MODEL
        return if (model.startsWith(manufacturer, ignoreCase = true)) model else "$manufacturer $model"
    }
}
