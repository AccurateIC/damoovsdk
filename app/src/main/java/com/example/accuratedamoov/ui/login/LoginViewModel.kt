package com.example.accuratedamoov.ui.login

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.accuratedamoov.data.model.LoginRequest
import com.example.accuratedamoov.data.model.LoginResponse
import com.example.accuratedamoov.data.network.RetrofitClient
import com.google.vr.dynamite.client.a
import kotlinx.coroutines.launch

// LoginViewModel.kt
class LoginViewModel(application: Application) : AndroidViewModel(application) {

    private val _loginResult = MutableLiveData<Result<LoginResponse>>()
    val loginResult: LiveData<Result<LoginResponse>> = _loginResult

    @SuppressLint("StaticFieldLeak")
    private val mContext:Context = application.applicationContext
    private val apiService = RetrofitClient.getApiService(mContext)

    fun loginUser(email: String, password: String,device_id: String) {
        viewModelScope.launch {
            try {
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
                val prefs = mContext.getSharedPreferences("user_prefs", MODE_PRIVATE)


                val response = apiService.loginUser(LoginRequest(email, password,device_id))
                if (response.isSuccessful && response.body() != null) {
                    _loginResult.postValue(Result.success(response.body()!!))
                    enprefs.edit().apply {
                        putString("user_email", email)
                        putString("user_password", password)
                        apply()
                    }
                    Log.d("LoginViewModel", "Login successful for user:" +response.body()!!.user_id)
                    prefs.edit().apply {
                        putBoolean("is_logged_in", true)
                        putInt("user_id", response.body()!!.user_id)
                        apply()
                    }

                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Login failed"
                    _loginResult.postValue(Result.failure(Exception(errorMsg)))
                }
            } catch (e: Exception) {
                _loginResult.postValue(Result.failure(e))
            }
        }
    }
}
