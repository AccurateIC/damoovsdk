package com.example.accuratedamoov.ui.feed

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.GridView
import android.widget.ImageView
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import com.example.accuratedamoov.R
class CustomDatePickerDialog(context: Context, private val onDateSelected: (Calendar) -> Unit)
    : Dialog(context) {

    private val calendar = Calendar.getInstance()
    private lateinit var monthTitle: TextView
    private lateinit var grid: GridView
    private var selectedDate: Calendar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.custom_calendar)
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        monthTitle = findViewById(R.id.monthTitle)
        grid = findViewById(R.id.calendarGrid)

        findViewById<ImageView>(R.id.prevMonth).setOnClickListener {
            calendar.add(Calendar.MONTH, -1)
            updateCalendar()
        }

        findViewById<ImageView>(R.id.nextMonth).setOnClickListener {
            calendar.add(Calendar.MONTH, 1)
            updateCalendar()
        }

        findViewById<Button>(R.id.btnConfirm).setOnClickListener {
            selectedDate?.let { onDateSelected(it) }
            dismiss()
        }

        updateCalendar()
    }

    private fun updateCalendar() {
        val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        monthTitle.text = monthFormat.format(calendar.time)

        val days = mutableListOf<Date>()
        val tempCal = calendar.clone() as Calendar
        tempCal.set(Calendar.DAY_OF_MONTH, 1)
        val monthStart = tempCal.get(Calendar.DAY_OF_WEEK) - 1
        tempCal.add(Calendar.DAY_OF_MONTH, -monthStart)
        while (days.size < 42) {
            days.add(tempCal.time)
            tempCal.add(Calendar.DAY_OF_MONTH, 1)
        }

        val today = Calendar.getInstance()

        grid.adapter = object : ArrayAdapter<Date>(context, android.R.layout.simple_list_item_1, days) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val dateView = (convertView ?: LayoutInflater.from(context)
                    .inflate(android.R.layout.simple_list_item_1, parent, false)) as TextView

                val date = getItem(position)!!
                val day = Calendar.getInstance().apply { time = date }

                dateView.text = day.get(Calendar.DAY_OF_MONTH).toString()
                dateView.gravity = Gravity.CENTER
                dateView.setPadding(0, 16, 0, 16)

                val isCurrentMonth = day.get(Calendar.MONTH) == calendar.get(Calendar.MONTH)
                val isSelected = selectedDate?.let {
                    it.get(Calendar.YEAR) == day.get(Calendar.YEAR) &&
                            it.get(Calendar.MONTH) == day.get(Calendar.MONTH) &&
                            it.get(Calendar.DAY_OF_MONTH) == day.get(Calendar.DAY_OF_MONTH)
                } ?: false

                val isToday =
                    day.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                            day.get(Calendar.MONTH) == today.get(Calendar.MONTH) &&
                            day.get(Calendar.DAY_OF_MONTH) == today.get(Calendar.DAY_OF_MONTH)

                when {
                    isSelected -> {
                        dateView.setBackgroundResource(R.drawable.rect_active_bg)
                        dateView.setTextColor(Color.WHITE)
                    }
                    isToday -> {
                        dateView.setBackgroundResource(R.drawable.rect_active_bg)
                        dateView.setTextColor(Color.BLACK)
                    }
                    else -> {
                        dateView.setBackgroundResource(R.drawable.rect_inactive_bg)
                        dateView.setTextColor(if (isCurrentMonth) Color.BLACK else Color.GRAY)
                    }
                }

                dateView.setOnClickListener {
                    selectedDate = day
                    updateCalendar()
                }

                return dateView
            }
        }
    }

}
