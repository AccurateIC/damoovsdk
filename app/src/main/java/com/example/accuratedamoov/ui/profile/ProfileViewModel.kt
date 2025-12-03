package com.example.accuratedamoov.ui.profile

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import androidx.core.content.edit
import com.example.accuratedamoov.data.model.SafetySummaryResponse
import com.example.accuratedamoov.data.network.RetrofitClient

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    private val appContext = application.applicationContext
    private val _profileImagePath = MutableLiveData<String?>()
    val profileImagePath: LiveData<String?> get() = _profileImagePath
    private val _summary = MutableLiveData<SafetySummaryResponse>()
    val summary: LiveData<SafetySummaryResponse> get() = _summary


    private val _error = MutableLiveData<String>()
    val error get() = _error
    init {
        _profileImagePath.value = prefs.getString("profile_image_path", null)
    }

    fun saveProfileImage(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {

            val input = context.contentResolver.openInputStream(uri)
            val file = File(context.filesDir, "profile.jpg")
            val output = FileOutputStream(file)

            input?.copyTo(output)
            input?.close()
            output.close()

            prefs.edit {
                putString("profile_image_path", file.absolutePath)
            }

            _profileImagePath.postValue(file.absolutePath)
        }
    }


    fun fetchDashboardData(user_Id: Int?) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val api = RetrofitClient.getProfileApiService(appContext)
                val response = api.getSafetySummary(user_Id)
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
