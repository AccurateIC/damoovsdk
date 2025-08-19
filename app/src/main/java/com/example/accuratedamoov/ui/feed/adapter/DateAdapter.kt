package com.example.accuratedamoov.ui.feed.adapter


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.accuratedamoov.R
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.*

class DateAdapter(private val onDateSelected: (String) -> Unit) :
    RecyclerView.Adapter<DateAdapter.DateViewHolder>() {
    private val dateList: List<String>
    private var selectedPosition: Int = RecyclerView.NO_POSITION // ✅ nothing selected at start

    init {
        val format = SimpleDateFormat("dd MMM", Locale.getDefault())
        val todayString = format.format(Calendar.getInstance().time)
        val calendar = Calendar.getInstance()
        val tempList = mutableListOf<String>()
        for (i in 0..30) {
            val formattedDate = format.format(calendar.time)
            if (formattedDate != todayString) { // exclude Today
                tempList.add(formattedDate)
            }
            calendar.add(Calendar.DAY_OF_YEAR, -1)
        }
        dateList = tempList.reversed()

        selectedPosition = dateList.lastIndex
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DateViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_date, parent, false)
        return DateViewHolder(view)
    }

    override fun onBindViewHolder(holder: DateViewHolder, position: Int) {
        val date = dateList[position]
        holder.dateText.text = date
        val cardView = holder.itemView as MaterialCardView
        if (selectedPosition == position) {
            cardView.setCardBackgroundColor(holder.itemView.context.getColor(android.R.color.white))
            holder.dateText.setTextColor(holder.itemView.context.getColor(android.R.color.black))
            cardView.strokeWidth = 3
            cardView.strokeColor = holder.itemView.context.getColor(R.color.black)
        } else {
            cardView.setCardBackgroundColor(holder.itemView.context.getColor(R.color.colorPrimaryVariant))
            holder.dateText.setTextColor(holder.itemView.context.getColor(android.R.color.white))
            cardView.strokeWidth = 0
        }
        holder.itemView.setOnClickListener {
            val previousPos = selectedPosition
            selectedPosition = position
            notifyItemChanged(previousPos)
            notifyItemChanged(position)
            onDateSelected(date)
        }
    }

    fun getSelectedDate(): String? {
        return if (selectedPosition != RecyclerView.NO_POSITION) {
            dateList[selectedPosition]
        } else null
    }

    override fun getItemCount(): Int = dateList.size
    inner class DateViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val dateText: TextView = itemView.findViewById(R.id.tvDate)
    }


    fun clearSelection() {
        if (selectedPosition != RecyclerView.NO_POSITION) {
            val prev = selectedPosition
            selectedPosition = RecyclerView.NO_POSITION
            notifyItemChanged(prev)
        }
    }
}


