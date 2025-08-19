package com.example.accuratedamoov.data.network

import com.example.accuratedamoov.data.model.DeviceRequest
import com.example.accuratedamoov.data.model.DeviceResponse
import com.example.accuratedamoov.data.model.LoginRequest
import com.example.accuratedamoov.data.model.LoginResponse
import com.example.accuratedamoov.data.model.RegisterModel
import com.example.accuratedamoov.data.model.RegisterResponse
import com.example.accuratedamoov.data.model.SafetySummaryResponse
import com.example.accuratedamoov.data.model.TripApiResponse
import com.example.accuratedamoov.data.model.TripSummaryResponse
import com.example.accuratedamoov.data.model.UserProfileResponse
import com.example.accuratedamoov.model.GeoPointResponse
import retrofit2.Call
import retrofit2.Response

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

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


    @GET("triprecords")
    suspend fun getTrips(): Response<TripApiResponse>


    @GET("geopoints")
    suspend fun getGeoPoints(
        @Query("unique_id") uniqueId: String,
        @Query("user_id") userId: Int
    ): Response<GeoPointResponse>


    @GET("triprecordfordevice")
    suspend fun getTripsForDevice(
        @Query("user_id") userId: Int
    ): Response<TripApiResponse>


    @Headers("Content-Type: application/json")
    @POST("api/devices")
    fun registerDevice(
        @Body request: DeviceRequest
    ): Call<DeviceResponse>

    @GET("health")
    suspend fun checkHealth(): Response<Void>

    @POST("/api/registerWithDevice")
    fun registerUserWithDevice(@Body body: RegisterModel): Call<RegisterResponse>

    @POST("/api/login")
    suspend fun loginUser(@Body request: LoginRequest): Response<LoginResponse>

    @GET("/userprofile")
    suspend fun getUserProfile(
        @Query("user_id") userId: Int?
    ): Response<UserProfileResponse>


    @GET("/tripsummaryfordevice")
    suspend fun getTripSummary(
        @Query("device_id") deviceId: String,
        @Query("user_id") userId: String
    ): Response<TripSummaryResponse>


    @GET("safety_dashboard_summary")
    suspend fun getSafetySummary(
        @Query("filter") filter: String
    ): Response<SafetySummaryResponse>
}
