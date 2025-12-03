package com.example.accuratedamoov.data.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface OTPApiService {
    @POST("sendotp")
    suspend fun sendOTP(@Body request: Map<String, String>): Response<OTPResponse>

    @POST("verifyotp")
    suspend fun verifyOTP(@Body request: Map<String, String>): Response<OTPResponse>
}

data class OTPResponse(
    val success: Boolean,
    val message: String? = null,
    val error: String? = null
)
