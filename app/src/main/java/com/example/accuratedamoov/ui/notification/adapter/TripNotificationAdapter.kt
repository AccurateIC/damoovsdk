package com.example.accuratedamoov.ui.notification.adapter

import android.location.Geocoder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.accuratedamoov.model.TripNotification
import com.example.accuratedamoov.ui.notification.model.NotificationListItem
import com.example.accuratedamoov.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class TripNotificationAdapter(
    private val items: List<NotificationListItem>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val TYPE_HEADER = 0
    private val TYPE_NOTIFICATION = 1

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is NotificationListItem.DateHeader -> TYPE_HEADER
            is NotificationListItem.NotificationItem -> TYPE_NOTIFICATION
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_HEADER) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_date_header, parent, false)
            DateHeaderViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_trip_notification, parent, false)
            NotificationViewHolder(view)
        }
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {

            is NotificationListItem.DateHeader ->
                (holder as DateHeaderViewHolder).bind(item)

            is NotificationListItem.NotificationItem ->
                (holder as NotificationViewHolder).bind(item.data)
        }
    }

    inner class DateHeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val dateHeaderTv: TextView = view.findViewById(R.id.dateText)
        fun bind(header: NotificationListItem.DateHeader) {
            dateHeaderTv.text = header.title
        }
    }

    inner class NotificationViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        private val messageTv: TextView = view.findViewById(R.id.messageTv)
        private val subTextTv: TextView = view.findViewById(R.id.subTextTv)

        fun bind(item: TripNotification) {

            messageTv.text = item.message

            // Format time (10:30 AM)
            val time = formatTime(item.timestamp)

            // Resolve address names
            val fromAddress = getAddress(item.lat, item.lng)
            val toAddress   = getAddress(item.lat, item.lng)

            val subText = when (item.message) {

                "Trip Started" -> {
                    "Your trip has been started from $fromAddress at $time"
                }

                "Trip Ended" -> {
                    "Your trip has been ended at $toAddress at $time"
                }

                else -> ""
            }

            subTextTv.text = subText
        }

        // Reverse-geocode coordinates
        private fun getAddress(lat: Double?, lng: Double?): String {
            if (lat == null || lng == null) return "Unknown location"

            return try {
                val geocoder = Geocoder(itemView.context, Locale.getDefault())
                val result = geocoder.getFromLocation(lat, lng, 1)
                result?.firstOrNull()?.locality ?: "Unknown location"
            } catch (e: Exception) {
                "Unknown location"
            }
        }

        private fun formatTime(timestamp: Long): String {
            val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
            val millis = timestamp * 1000L
            return sdf.format(Date(millis))
        }
    }
}
