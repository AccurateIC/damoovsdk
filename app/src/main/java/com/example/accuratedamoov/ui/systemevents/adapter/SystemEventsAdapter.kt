package com.example.accuratedamoov.ui.systemevents.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.accuratedamoov.data.local.systemevents.SystemEventEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.accuratedamoov.R
class SystemEventsAdapter :
    ListAdapter<SystemEventEntity, SystemEventsAdapter.EventViewHolder>(
        object : DiffUtil.ItemCallback<SystemEventEntity>() {
            override fun areItemsTheSame(oldItem: SystemEventEntity, newItem: SystemEventEntity) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: SystemEventEntity, newItem: SystemEventEntity) =
                oldItem == newItem
        }
    ) {

    class EventViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val message: TextView = view.findViewById(R.id.eventMessage)
        val type: TextView = view.findViewById(R.id.eventType)
        val timestamp: TextView = view.findViewById(R.id.eventTimestamp)
        val meta: TextView = view.findViewById(R.id.eventMeta)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_system_event, parent, false)
        return EventViewHolder(v)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val event = getItem(position)
        holder.message.text = event.event_message
        holder.type.text = "Type: ${event.event_type ?: "Unknown"}"
        holder.timestamp.text = SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss", Locale.getDefault()
        ).format(Date(event.timestamp))
        holder.meta.text = "Device: ${event.device_id}, User: ${event.user_id}, Synced: ${event.synced}"
    }
}
