package com.example.accuratedamoov.ui.feed.filtedialog

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.Button
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.example.accuratedamoov.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import androidx.core.graphics.drawable.toDrawable


class CustomDatePickerDialog(
    context: Context,
    private val onDateRangeSelected: (Calendar, Calendar) -> Unit
) : Dialog(context) {

    private val calendar = Calendar.getInstance()
    private lateinit var monthTitle: TextView
    private lateinit var grid: GridLayout
    private lateinit var confirmButton: Button
    private var startDate: Calendar? = null
    private var endDate: Calendar? = null

    private var maxDate: Calendar? = null
    private var minDate: Calendar? = null


    init{
        setCancelable(true)
        setCanceledOnTouchOutside(true)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.custom_calendar)
        window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())

        val displayMetrics = context.resources.displayMetrics
        val width = (displayMetrics.widthPixels * 0.9).toInt()
        val height = (displayMetrics.heightPixels * 0.65).toInt()
        window?.setLayout(width, height)

        monthTitle = findViewById(R.id.monthTitle)
        grid = findViewById(R.id.calendarGrid)
        confirmButton = findViewById(R.id.btnConfirm)
        confirmButton.isEnabled = false
        confirmButton.alpha = 0.5f

        val prevBtn = findViewById<ImageView>(R.id.prevMonth)
        val nextBtn = findViewById<ImageView>(R.id.nextMonth)
        val closeBtn = findViewById<ImageView>(R.id.closeIv)
        val dlgCalendarll = findViewById<LinearLayout>(R.id.dlgCalendarll)
        prevBtn.setOnClickListener {
            calendar.add(Calendar.MONTH, -1)
            updateCalendar()
        }

        //dlgCalendarll.setOnClickListener { dismiss() }
        nextBtn.setOnClickListener {
            val max = maxDate ?: Calendar.getInstance()
            // Prevent navigation beyond maxDate’s month
            if (calendar.get(Calendar.YEAR) < max.get(Calendar.YEAR) ||
                (calendar.get(Calendar.YEAR) == max.get(Calendar.YEAR) &&
                        calendar.get(Calendar.MONTH) < max.get(Calendar.MONTH))
            ) {
                calendar.add(Calendar.MONTH, 1)
                updateCalendar()
            }
        }

        closeBtn.setOnClickListener { dismiss() }

        confirmButton.setOnClickListener {
            if (startDate != null && endDate != null) {
                onDateRangeSelected(startDate!!, endDate!!)
                dismiss()
            } else {
                Toast.makeText(context, "Select start and end date", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ✅ move updateCalendar here (after setMaxDate() applied)
    override fun onStart() {
        super.onStart()
        updateCalendar()
    }

    private fun updateCalendar() {
        val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        monthTitle.text = monthFormat.format(calendar.time)

        val today = Calendar.getInstance()
        grid.removeAllViews()
        grid.columnCount = 7
        val inflater = LayoutInflater.from(context)

        val tempCal = calendar.clone() as Calendar
        val daysInMonth = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH)
        tempCal.set(Calendar.DAY_OF_MONTH, 1)
        val firstDayOfWeek = tempCal.get(Calendar.DAY_OF_WEEK) - 1

        for (i in 0 until firstDayOfWeek) {
            val emptyView = inflater.inflate(R.layout.item_calendar_day, grid, false) as TextView
            emptyView.text = ""
            grid.addView(emptyView)
        }

        for (dayNum in 1..daysInMonth) {
            val day = calendar.clone() as Calendar
            day.set(Calendar.DAY_OF_MONTH, dayNum)

            val dateView = inflater.inflate(R.layout.item_calendar_day, grid, false) as TextView
            dateView.text = dayNum.toString()
            dateView.gravity = Gravity.CENTER

            val isToday = isSameDay(day, today)
            val isFuture = maxDate?.let { day.after(it) } ?: day.after(today)
            val isBeforeMin = minDate?.let { day.before(it) } ?: false
            val isSelectedStart = isSameDay(day, startDate)
            val isSelectedEnd = isSameDay(day, endDate)
            val inRange = startDate != null && endDate != null &&
                    day.after(startDate) && day.before(endDate)

            when {
                isFuture || isBeforeMin -> {
                    dateView.setTextColor(Color.GRAY)
                    dateView.alpha = 0.4f
                    dateView.isEnabled = false
                }
                isSelectedStart || isSelectedEnd -> {
                    dateView.setBackgroundResource(R.drawable.rect_active_bg)
                    dateView.setTextColor(Color.WHITE)
                }
                inRange -> {
                    dateView.setBackgroundResource(R.drawable.rect_range_bg)
                    dateView.setTextColor(Color.BLACK)
                }
                isToday -> {
                    dateView.setBackgroundResource(R.drawable.rect_today_bg)
                    dateView.setTextColor(Color.WHITE)
                }
                else -> {
                    dateView.setBackgroundResource(R.drawable.rect_inactive_bg)
                    dateView.setTextColor(Color.BLACK)
                }
            }

            if (!isFuture && !isBeforeMin) {
                dateView.setOnClickListener {
                    when {
                        startDate == null -> startDate = day
                        endDate == null && day.after(startDate) -> endDate = day
                        else -> { // reset
                            startDate = day
                            endDate = null
                        }
                    }
                    confirmButton.isEnabled = startDate != null && endDate != null
                    confirmButton.alpha = if (confirmButton.isEnabled) 1f else 0.5f
                    updateCalendar()
                }
            }

            grid.addView(dateView)
        }
    }

    private fun isSameDay(cal1: Calendar?, cal2: Calendar?): Boolean {
        if (cal1 == null || cal2 == null) return false
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    fun setMaxDate(maxDateMillis: Long) {
        maxDate = Calendar.getInstance().apply { timeInMillis = maxDateMillis }
    }

    fun setMinDate(minDateMillis: Long) {
        minDate = Calendar.getInstance().apply { timeInMillis = minDateMillis }
    }
}
