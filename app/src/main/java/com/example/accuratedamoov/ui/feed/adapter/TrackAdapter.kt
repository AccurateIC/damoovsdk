package com.example.accuratedamoov.ui.feed.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.RecyclerView
import com.example.accuratedamoov.data.model.TripData
import com.example.accuratedamoov.data.network.RetrofitClient
import com.example.accuratedamoov.databinding.ListItemTripBinding
import com.example.accuratedamoov.model.TrackModel
import com.example.accuratedamoov.service.NetworkMonitorService
import com.example.accuratedamoov.ui.tripDetails.TripDetailsActivity
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class TrackAdapter(
    private val objects: List<TripData>,
) : RecyclerView.Adapter<TrackAdapter.MyViewHolder>() {
    lateinit var mContext: Context

    class MyViewHolder(val binding: ListItemTripBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        mContext = parent.context
        val binding =
            ListItemTripBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewHolder(binding)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val item = objects[position]

        with(holder.binding) {
            departureAddressView.text =
                "Start address:\n${item.start_latitude.toString() + "," + item.start_longitude}"
            destinationAddressView.text =
                "End address:\n${item.end_latitude.toString() + "," + item.end_longitude}"
            departureDateView.text = "Date start:\n${convertUnixToIST(item.start_date)}"
            totalDistanceView.text =
                "Distance covered: \n${item.distance_km.toInt().toString() + "km"}"
            /* mileageView.text = String.format("Mileage: %.1f km", item.distance_km)
             totalTimeView.text = String.format("Total time: %d mins",)*/

            root.setOnClickListener {
                if (NetworkMonitorService.isConnected == true) {
                    val intent = Intent(mContext, TripDetailsActivity::class.java)
                    intent.putExtra("ID", item.UNIQUE_ID.toString())
                    mContext.startActivity(intent)
                }

            }
            detailsButton.setOnClickListener {
                if (NetworkMonitorService.isConnected == true) {
                    val intent = Intent(mContext, TripDetailsActivity::class.java)
                    intent.putExtra("ID", item.UNIQUE_ID.toString())
                    mContext.startActivity(intent)
                }
            }
        }
    }

    override fun getItemCount() = objects.size


    @SuppressLint("NewApi")
    fun convertUnixToIST(unixTime: Long): String {
        val instant = Instant.ofEpochMilli(unixTime)
        val zoneId = ZoneId.of("Asia/Kolkata")
        val zonedDateTime = instant.atZone(zoneId)
        val formatter = DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy HH:mm:ss.SSS z")
        return formatter.format(zonedDateTime)
    }


}