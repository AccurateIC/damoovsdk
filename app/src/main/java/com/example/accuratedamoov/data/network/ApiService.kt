package com.example.accuratedamoov.data.network

import com.example.accuratedamoov.data.model.TripApiResponse
import com.example.accuratedamoov.model.GeoPointModel
import okhttp3.RequestBody
import org.osmdroid.util.GeoPoint
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


    @GET("/geopoints")
    suspend fun getGeoPoints(@Query("unique_id") uniqueId: String):  List<GeoPointModel>
}
