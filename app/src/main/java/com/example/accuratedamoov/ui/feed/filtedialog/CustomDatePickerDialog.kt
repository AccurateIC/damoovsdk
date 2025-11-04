package com.example.accuratedamoov.ui.feed.filtedialog

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.Button
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.graphics.drawable.toDrawable
import com.example.accuratedamoov.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

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

    init {
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

        prevBtn.setOnClickListener {
            calendar.add(Calendar.MONTH, -1)
            updateCalendar()
        }

        nextBtn.setOnClickListener {
            val max = maxDate ?: Calendar.getInstance()
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

    override fun onStart() {
        super.onStart()
        updateCalendar()
    }

    private fun updateCalendar() {
        val monthFormat = SimpleDateFormat("MMMM", Locale.getDefault())
        monthTitle.text = monthFormat.format(calendar.time)

        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        grid.removeAllViews()
        grid.columnCount = 7
        val inflater = LayoutInflater.from(context)

        val displayCal = calendar.clone() as Calendar
        displayCal.set(Calendar.DAY_OF_MONTH, 1)

        val firstDayOfWeek = displayCal.get(Calendar.DAY_OF_WEEK) - 1
        val daysInMonth = displayCal.getActualMaximum(Calendar.DAY_OF_MONTH)

        val prevMonthCal = displayCal.clone() as Calendar
        prevMonthCal.add(Calendar.MONTH, -1)
        val daysInPrevMonth = prevMonthCal.getActualMaximum(Calendar.DAY_OF_MONTH)

        val nextMonthCal = displayCal.clone() as Calendar
        nextMonthCal.add(Calendar.MONTH, 1)

        val totalCells = 42
        var dayCounter = 1
        var nextMonthDayCounter = 1

        for (i in 0 until totalCells) {
            val dateView = inflater.inflate(R.layout.item_calendar_day, grid, false) as TextView
            dateView.gravity = Gravity.CENTER

            val day = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            when {
                // Previous month days
                i < firstDayOfWeek -> {
                    val prevDay = daysInPrevMonth - firstDayOfWeek + i + 1
                    day.set(prevMonthCal.get(Calendar.YEAR), prevMonthCal.get(Calendar.MONTH), prevDay)
                    dateView.text = prevDay.toString()
                }

                // Current month days
                i < firstDayOfWeek + daysInMonth -> {
                    val dayNum = dayCounter++
                    day.set(displayCal.get(Calendar.YEAR), displayCal.get(Calendar.MONTH), dayNum)
                    dateView.text = dayNum.toString()
                }

                // Next month days
                else -> {
                    val nextDay = nextMonthDayCounter++
                    day.set(nextMonthCal.get(Calendar.YEAR), nextMonthCal.get(Calendar.MONTH), nextDay)
                    dateView.text = nextDay.toString()
                }
            }

            setDayAppearance(dateView, day, today)
            grid.addView(dateView)
        }
    }

    private fun setDayAppearance(dateView: TextView, day: Calendar, today: Calendar) {
        // Normalize comparison values (strip time)
        val max = maxDate?.clone() as? Calendar ?: today.clone() as Calendar
        max.set(Calendar.HOUR_OF_DAY, 0)
        max.set(Calendar.MINUTE, 0)
        max.set(Calendar.SECOND, 0)
        max.set(Calendar.MILLISECOND, 0)

        val min = minDate?.clone() as? Calendar
        min?.set(Calendar.HOUR_OF_DAY, 0)
        min?.set(Calendar.MINUTE, 0)
        min?.set(Calendar.SECOND, 0)
        min?.set(Calendar.MILLISECOND, 0)

        val isFuture = day.after(max)
        val isBeforeMin = min?.let { day.before(it) } ?: false
        val isSelectedStart = isSameDay(day, startDate)
        val isSelectedEnd = isSameDay(day, endDate)
        val inRange = startDate != null && endDate != null &&
                day.after(startDate) && day.before(endDate)
        val isToday = isSameDay(day, today)

        when {
            isFuture || isBeforeMin -> {
                dateView.setTextColor(Color.GRAY)
                dateView.alpha = 0.4f
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
                dateView.setBackgroundResource(R.drawable.bg_filter_today)
                dateView.setTextColor(Color.WHITE)
            }
            else -> {
                dateView.setBackgroundResource(R.drawable.rect_inactive_bg)
                dateView.setTextColor(Color.BLACK)
            }
        }

        if (!isFuture && !isBeforeMin) {
            dateView.setOnClickListener {
                handleDateSelection(day)
                calendar.set(Calendar.YEAR, day.get(Calendar.YEAR))
                calendar.set(Calendar.MONTH, day.get(Calendar.MONTH))
                updateCalendar()
            }
        }
    }



    private fun handleDateSelection(day: Calendar) {
        when {
            startDate == null -> startDate = (day.clone() as Calendar)
            endDate == null && day.after(startDate) -> endDate = (day.clone() as Calendar)
            else -> {
                startDate = (day.clone() as Calendar)
                endDate = null
            }
        }
        confirmButton.isEnabled = startDate != null && endDate != null
        confirmButton.alpha = if (confirmButton.isEnabled) 1f else 0.5f
        updateCalendar()
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
