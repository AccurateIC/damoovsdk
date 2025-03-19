package com.example.accuratedamoov.ui.feed.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.RecyclerView
import com.example.accuratedamoov.databinding.ListItemTripBinding
import com.example.accuratedamoov.model.TrackModel
import com.example.accuratedamoov.ui.tripDetails.TripDetailsActivity

class TrackAdapter(
    private val objects: List<TrackModel>,
    private val selectedBlock: (trackId: String) -> Unit
) : RecyclerView.Adapter<TrackAdapter.MyViewHolder>() {

    class MyViewHolder(val binding: ListItemTripBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val binding = ListItemTripBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val item = objects[position]

        with(holder.binding) {
            departureAddressView.text = "Start address:\n${item.addressStart}"
            destinationAddressView.text = "End address:\n${item.addressEnd}"
            departureDateView.text = "Date start:\n${item.startDate}"
            destinationDateView.text = "Date end:\n${item.endDate}"
            mileageView.text = String.format("Mileage: %.1f km", item.distance)
            totalTimeView.text = String.format("Total time: %d mins", item.duration.toInt())

            root.setOnClickListener {
                item.trackId?.let { selectedBlock(it) }

            }
            detailsButton.setOnClickListener {
                item.trackId?.let { selectedBlock(it) }
            }
        }
    }

    override fun getItemCount() = objects.size


}