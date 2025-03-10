package com.example.accuratedamoov.data.network

import okhttp3.RequestBody
import retrofit2.Call

import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path

data class SyncRequest(
    val data: List<Map<String, Any>>
)

data class ApiResponse(
    val message: String,
    val insertedRows: Int?
)

interface ApiService {
    /*@Headers("Content-Type: application/json")
    @POST("api/{tableName}")
    fun syncData(
        @Path("tableName") tableName: String,
        @Body request: RequestBody
    ): Call<ApiResponse>
*/
    @Headers("Content-Type: application/json")
    @POST("api/{tableName}")
    fun syncData(
        @Path("tableName") tableName: String,
        @Body request: SyncRequest
    ): Call<ApiResponse>
}
