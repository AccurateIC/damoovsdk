package com.example.accuratedamoov.ui.profile

import android.app.Application
import android.content.Context
import android.net.Uri
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

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    private val _profileImagePath = MutableLiveData<String?>()
    val profileImagePath: LiveData<String?> get() = _profileImagePath

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
}
