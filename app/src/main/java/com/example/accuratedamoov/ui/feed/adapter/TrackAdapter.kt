package com.example.accuratedamoov.ui.feed.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.accuratedamoov.data.local.addresscache.AddressDatabase
import com.example.accuratedamoov.data.local.addresscache.AddressEntity
import com.example.accuratedamoov.data.model.TripData
import com.example.accuratedamoov.databinding.ListItemTripBinding
import com.example.accuratedamoov.databinding.TripInfoCardBinding
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
import java.text.SimpleDateFormat
import java.util.Locale
import com.example.accuratedamoov.R
import java.util.Calendar


class TrackAdapter(
    private var trips: List<TripData>,
    context: Context
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val mContext = context
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val addressDao = AddressDatabase.getDatabase(context).addressDao()

    companion object {
        private const val TYPE_DATE_HEADER = 0
        private const val TYPE_TRIP_ITEM = 1
    }

    private var items: List<ListItem> = emptyList()

    sealed class ListItem {
        data class DateHeader(val date: String) : ListItem()
        data class TripItem(val trip: TripData) : ListItem()
    }

    class DateHeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val dateText: TextView = view.findViewById(R.id.dateText)
        fun bind(date: String) {
            dateText.text = date
        }
    }

    class TripViewHolder(val binding: TripInfoCardBinding) : RecyclerView.ViewHolder(binding.root)

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is ListItem.DateHeader -> TYPE_DATE_HEADER
            is ListItem.TripItem -> TYPE_TRIP_ITEM
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_DATE_HEADER -> {
                val view = LayoutInflater.from(mContext)
                    .inflate(R.layout.item_date_header, parent, false)
                DateHeaderViewHolder(view)
            }

            else -> {
                val binding = TripInfoCardBinding.inflate(
                    LayoutInflater.from(mContext),
                    parent,
                    false
                )
                TripViewHolder(binding)
            }
        }
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is ListItem.DateHeader -> (holder as DateHeaderViewHolder).bind(item.date)
            is ListItem.TripItem -> bindTrip(holder as TripViewHolder, item.trip)
        }
    }

    private fun bindTrip(holder: TripViewHolder, item: TripData) {
        val binding = holder.binding

        binding.tripDate.text = formatDisplayDate(item.start_date_ist)
        binding.distanceText.text = "${item.distance_km?.toInt() ?: 0} km"

        val (startTime, startAmPm) = formatTime(item.start_date_ist)
        val (endTime, endAmPm) = formatTime(item.end_date_ist)

        binding.sourceTime.text = startTime
        binding.sourceAmPm.text = startAmPm.uppercase()
        binding.destTime.text = endTime
        binding.destAmPm.text = endAmPm.uppercase()

        coroutineScope.launch {
            val startAddress = item.start_coordinates?.let { getOrFetchAddress(it) } ?: "Unknown"
            val startParts = startAddress.split(",")
            binding.sourceLocationMain.text = startParts.firstOrNull()?.trim() ?: "Unknown"
            binding.sourceLocationSub.text =
                startParts.drop(1).joinToString(",").trim().ifEmpty { "" }

            val endAddress = item.end_coordinates?.let { getOrFetchAddress(it) } ?: "Unknown"
            val endParts = endAddress.split(",")
            binding.destLocationMain.text = endParts.firstOrNull()?.trim() ?: "Unknown"
            binding.destLocationSub.text =
                endParts.drop(1).joinToString(",").trim().ifEmpty { "" }
        }

        binding.root.setOnClickListener {
            val intent = Intent(mContext, TripDetailsActivity::class.java).apply {
                putExtra("ID", item.unique_id.toString())
                putExtra("START_TIME", item.start_date_ist)
                putExtra("END_TIME", item.end_date_ist)
                putExtra("START_LOC", binding.sourceLocationMain.text)
                putExtra("END_LOC", binding.destLocationMain.text)
            }
            mContext.startActivity(intent)
        }
    }

    private suspend fun getOrFetchAddress(coord: String): String {
        val (latStr, lonStr) = coord.split(",").map { it.trim() }
        val key = "$latStr,$lonStr"

        addressDao.getAddress(key)?.let { return it }

        val lat = latStr.toDoubleOrNull()
        val lon = lonStr.toDoubleOrNull()
        if (lat != null && lon != null) {
            val fetched = getAddressFromCoordinates(lat, lon)
            if (fetched != null) {
                addressDao.insertAddress(AddressEntity(key, lat, lon, fetched))
                return fetched
            }
        }
        return "Unknown"
    }

    private suspend fun getAddressFromCoordinates(lat: Double, lon: Double): String? =
        withContext(Dispatchers.IO) {
            try {
                val url =
                    URL("https://nominatim.openstreetmap.org/reverse?format=json&lat=$lat&lon=$lon&zoom=18&addressdetails=1")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    setRequestProperty("User-Agent", "AccurateDamoov/1.0")
                    connectTimeout = 5000
                    readTimeout = 5000
                }
                conn.inputStream.bufferedReader().use {
                    val response = it.readText()
                    val json = JSONObject(response)
                    json.optString("display_name", null)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

    // Display date inside trip card (simple)
    private fun formatDisplayDate(dateTime: String?): String {
        if (dateTime.isNullOrEmpty()) return ""
        val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val outputFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
        return try {
            val date = inputFormat.parse(dateTime)
            outputFormat.format(date!!)
        } catch (e: Exception) {
            ""
        }
    }

    // Header date formatter — Today / Yesterday / 8th Sep. 2025
    private fun formatHeaderDate(dateTime: String?): String {
        if (dateTime.isNullOrEmpty()) return ""
        val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return try {
            val date = inputFormat.parse(dateTime) ?: return ""

            val cal = Calendar.getInstance()
            val today = Calendar.getInstance()

            cal.time = date
            val isToday = cal.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                    cal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)

            today.add(Calendar.DAY_OF_YEAR, -1)
            val isYesterday = cal.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                    cal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)

            when {
                isToday -> "Today"
                isYesterday -> "Yesterday"
                else -> {
                    val day = SimpleDateFormat("d", Locale.getDefault()).format(date).toInt()
                    val month = SimpleDateFormat("MMM", Locale.getDefault()).format(date)
                    val year = SimpleDateFormat("yyyy", Locale.getDefault()).format(date)
                    val suffix = when {
                        day in 11..13 -> "th"
                        day % 10 == 1 -> "st"
                        day % 10 == 2 -> "nd"
                        day % 10 == 3 -> "rd"
                        else -> "th"
                    }
                    "$day$suffix $month. $year"
                }
            }
        } catch (e: Exception) {
            ""
        }
    }

    private fun formatTime(dateTime: String?): Pair<String, String> {
        if (dateTime.isNullOrEmpty()) return Pair("", "")
        val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val outputFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return try {
            val date = inputFormat.parse(dateTime)
            val formatted = outputFormat.format(date!!)
            val parts = formatted.split(" ")
            Pair(parts[0], parts[1])
        } catch (e: Exception) {
            Pair("", "")
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newTrips: List<TripData>) {
        val sortedTrips = newTrips.sortedByDescending { it.start_date_ist }
        val groupedItems = mutableListOf<ListItem>()
        var lastDate: String? = null

        for (trip in sortedTrips) {
            val headerDate = formatHeaderDate(trip.start_date_ist)
            if (headerDate != lastDate) {
                groupedItems.add(ListItem.DateHeader(headerDate))
                lastDate = headerDate
            }
            groupedItems.add(ListItem.TripItem(trip))
        }

        items = groupedItems
        notifyDataSetChanged()
    }
}



