package com.example.accuratedamoov.data.network

import android.content.Context
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private var retrofit: Retrofit? = null
    private var currentBaseUrl: String? = null

    fun getApiService(context: Context): ApiService {
        val sharedPreferences = context.getSharedPreferences("appSettings", Context.MODE_PRIVATE)
        val baseUrl = sharedPreferences.getString("api_url", "http://192.168.1.119:5000/") ?: "http://192.168.1.119:5000/"
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
}
