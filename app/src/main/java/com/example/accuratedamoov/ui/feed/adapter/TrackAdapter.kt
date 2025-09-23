package com.example.accuratedamoov.ui.feed.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.accuratedamoov.data.local.addresscache.AddressDatabase
import com.example.accuratedamoov.data.local.addresscache.AddressEntity
import com.example.accuratedamoov.data.model.TripData
import com.example.accuratedamoov.databinding.ListItemTripBinding
import com.example.accuratedamoov.service.NetworkMonitorService
import com.example.accuratedamoov.ui.tripDetails.TripDetailsActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class TrackAdapter(
    private var trips: List<TripData>,
    context: Context
) : RecyclerView.Adapter<TrackAdapter.MyViewHolder>() {

    private var selectedPosition: Int = 0
    private val mContext = context
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val addressDao = AddressDatabase.getDatabase(context).addressDao()

    class MyViewHolder(val binding: ListItemTripBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val binding = ListItemTripBinding.inflate(LayoutInflater.from(mContext), parent, false)
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val item = trips[position]

        with(holder.binding) {
            departureAddressView.text = "Start :\nLoading..."
            destinationAddressView.text = "End :\nLoading..."

            coroutineScope.launch {
                // START LOCATION
                item.start_coordinates?.let {
                    val (latStr, lonStr) = it.split(",").map { s -> s.trim() }
                    val key = "$latStr,$lonStr"
                    Log.d("omkarnotparsed: ", item.start_coordinates)
                    Log.d("omkar: ", key)

                    val cachedAddress = addressDao.getAddress(key)
                    if (cachedAddress != null) {
                        departureAddressView.text = "Start :\n$cachedAddress"
                    } else {
                        val lat = latStr.toDoubleOrNull()
                        val lon = lonStr.toDoubleOrNull()
                        val fetched = if (lat != null && lon != null) getAddressFromCoordinates(
                            lat,
                            lon
                        ) else null
                        departureAddressView.text = "Start :\n${fetched ?: "N/A"}"
                        if (fetched != null) addressDao.insertAddress(
                            AddressEntity(
                                key,
                                lat ?: 0.0,
                                lon ?: 0.0,
                                fetched
                            )
                        )
                    }
                }

                // END LOCATION
                item.end_coordinates?.let {
                    val (latStr, lonStr) = it.split(",").map { s -> s.trim() }
                    val key = "$latStr,$lonStr"

                    val cachedAddress = addressDao.getAddress(key)
                    if (cachedAddress != null) {
                        destinationAddressView.text = "End :\n$cachedAddress"
                    } else {
                        val lat = latStr.toDoubleOrNull()
                        val lon = lonStr.toDoubleOrNull()
                        val fetched = if (lat != null && lon != null) getAddressFromCoordinates(
                            lat,
                            lon
                        ) else null
                        destinationAddressView.text = "End :\n${fetched ?: "N/A"}"
                        if (fetched != null) addressDao.insertAddress(
                            AddressEntity(
                                key,
                                lat ?: 0.0,
                                lon ?: 0.0,
                                fetched
                            )
                        )
                    }
                }

            }

            departureDateView.text = "start: ${item.start_date_ist ?: "N/A"}"
            totalDistanceView.text =
                "Distance covered:\n${item.distance_km?.toInt()?.toString()?.plus("km") ?: "N/A"}"
            destinationDateView.text = "end: ${item.end_date_ist ?: "N/A"}"
            totalTimeView.text = item.duration_hh_mm?.let {
                val parts = it.split(":")
                if (parts.size == 2) "${parts[0]}h ${parts[1]}m" else it
            } ?: "N/A"

            val clickListener = View.OnClickListener {
                val startAddr = departureAddressView.text.toString().removePrefix("Start :\n")
                val endAddr = destinationAddressView.text.toString().removePrefix("End :\n")
                val intent = Intent(mContext, TripDetailsActivity::class.java)
                intent.putExtra("ID", item.unique_id.toString())
                intent.putExtra("START_TIME", item.start_date_ist)
                intent.putExtra("END_TIME", item.end_date_ist)
                intent.putExtra("START_LOC", startAddr)
                intent.putExtra("END_LOC", endAddr)
                mContext.startActivity(intent)

            }

            root.setOnClickListener(clickListener)
            detailsButton.setOnClickListener(clickListener)
        }
    }

    override fun getItemCount() = trips.size

    suspend fun getAddressFromCoordinates(lat: Double, lon: Double): String? {
        return withContext(Dispatchers.IO) {
            try {
                val url =
                    URL("https://nominatim.openstreetmap.org/reverse?format=json&lat=$lat&lon=$lon&zoom=18&addressdetails=1")
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    setRequestProperty("User-Agent", "AccurateDamoov/1.0")
                    connectTimeout = 5000
                    readTimeout = 5000
                }

                connection.inputStream.bufferedReader().use {
                    val response = it.readText()
                    val json = JSONObject(response)
                    json.optString("display_name", null)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newTrips: List<TripData>) {
        trips = newTrips
        notifyDataSetChanged()
    }


}



