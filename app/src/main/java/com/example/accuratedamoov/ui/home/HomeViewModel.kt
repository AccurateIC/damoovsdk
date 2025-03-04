package com.example.accuratedamoov.ui.home

import android.annotation.SuppressLint
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.accuratedamoov.MainApplication

@SuppressLint("MissingPermission")
class HomeViewModel : ViewModel() {

    val TAG: String = this::class.java.simpleName
    private val _errMsg = MutableLiveData<String>()

    val errMsg: LiveData<String> = _errMsg


    private val trackingApi = MainApplication.getTrackingApi()


    fun startTracking() {
        if(!trackingApi!!.isSdkEnabled()) {
            Log.d(TAG, "SDK not enabled")
            trackingApi.setEnableSdk(true)
        }
        trackingApi.startTracking()
        Log.d(TAG, "trip started")
    }


    fun stopTracking() {
        Log.d(TAG, "trip stopped")

        trackingApi!!.stopTracking()
    }

}