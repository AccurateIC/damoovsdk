package com.example.accuratedamoov.data.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object OTPApiClient {
    private const val BASE_URL = "http://192.168.10.41:5556/"

    val instance: OTPApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OTPApiService::class.java)
    }
}
