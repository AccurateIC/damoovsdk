package com.example.accuratedamoov.data.network

import android.content.Context
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


object RetrofitClient {
    private var retrofit: Retrofit? = null
    private var currentBaseUrl: String? = null

    private var profileRetrofit: Retrofit? = null
    private var currentProfileBaseUrl: String? = null

    fun getApiService(context: Context): ApiService {
        val sharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val baseUrl = sharedPreferences.getString(
            "api_url",
            "http://192.168.10.41:5556/"
        ) ?: "http://192.168.10.41:5556/"
        return getApiService(baseUrl)
    }

    fun getApiService(baseUrl: String): ApiService {
        if (retrofit == null || currentBaseUrl != baseUrl) {
            retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            currentBaseUrl = baseUrl
        }
        return retrofit!!.create(ApiService::class.java)
    }

    // ✅ New API service for Profile Summary (from SharedPreferences)
    fun getProfileApiService(context: Context): ApiService {
        val sharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val profileBaseUrl = sharedPreferences.getString(
            "score_url",
            "http://192.168.10.41:5000/"
        ) ?: "http://192.168.10.41:5000/"

        if (profileRetrofit == null || currentProfileBaseUrl != profileBaseUrl) {
            profileRetrofit = Retrofit.Builder()
                .baseUrl(profileBaseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            currentProfileBaseUrl = profileBaseUrl
        }
        return profileRetrofit!!.create(ApiService::class.java)
    }
}
