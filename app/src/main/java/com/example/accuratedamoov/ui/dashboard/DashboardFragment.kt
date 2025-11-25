package com.example.accuratedamoov.ui.dashboard

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ImageSpan
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.TooltipCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import pl.droidsonroids.gif.GifDrawable
import com.example.accuratedamoov.R
import com.example.accuratedamoov.data.model.SafetySummaryResponse
import com.skydoves.balloon.ArrowOrientation
import com.skydoves.balloon.ArrowPositionRules
import com.skydoves.balloon.Balloon
import com.skydoves.balloon.BalloonAnimation
import com.tomergoldst.tooltips.ToolTip
import com.tomergoldst.tooltips.ToolTipsManager


///stats
class DashboardFragment : Fragment() {

    private val dashboardViewModel: DashboardViewModel by viewModels()
    private var demoValuesSet = false // prevent multiple demo resets

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupAnimatedTitle(view)
        setupToggles(view)
        setupChips(view)

        dashboardViewModel.summary.observe(viewLifecycleOwner) { summary ->
            if (summary != null) {
                setDataIntoGrid(view, summary)
            } else if (!demoValuesSet) {
                setupTextForGrid(view)
                demoValuesSet = true
                Log.e("Dashboard", "API not available, using fallback demo values")
            }
        }
    }

    // -----------------------------
    // Animated title with GIF
    // -----------------------------
    private fun setupAnimatedTitle(view: View) {
        val textView = view.findViewById<TextView>(R.id.titleText)
        val text = textView.text.toString()
        val gifDrawable = GifDrawable(resources, R.drawable.ic_cowboy_animated)
        gifDrawable.setBounds(0, 0, 80, 80)

        val span = SpannableString("$text ")
        val imageSpan = ImageSpan(gifDrawable, ImageSpan.ALIGN_BASELINE)
        span.setSpan(imageSpan, text.length, text.length + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        textView.text = span
        textView.movementMethod = LinkMovementMethod.getInstance()

        gifDrawable.callback = object : Drawable.Callback {
            override fun invalidateDrawable(who: Drawable) {
                textView.invalidate()
            }

            override fun scheduleDrawable(who: Drawable, what: Runnable, `when`: Long) {
                textView.postDelayed(what, `when` - SystemClock.uptimeMillis())
            }

            override fun unscheduleDrawable(who: Drawable, what: Runnable) {
                textView.removeCallbacks(what)
            }
        }
    }

    // -----------------------------
    // Toggle buttons (Safety / General)
    // -----------------------------
    private fun setupToggles(view: View) {
        val leftText = view.findViewById<TextView>(R.id.leftBottomText)
        val rightText = view.findViewById<TextView>(R.id.rightBottomText)
        val safetyLayout = view.findViewById<View>(R.id.safetyParamsLayout)
        val generalLayout = view.findViewById<View>(R.id.generalParamsLayout)

        fun select(leftSelected: Boolean) {
            if (leftSelected) {
                leftText.setBackgroundResource(R.drawable.toggle_selected)
                leftText.setTextColor(Color.BLACK)
                rightText.setBackgroundResource(R.drawable.toggle_unselected)
                rightText.setTextColor(Color.WHITE)
                safetyLayout.visibility = View.VISIBLE
                generalLayout.visibility = View.GONE
            } else {
                rightText.setBackgroundResource(R.drawable.toggle_selected)
                rightText.setTextColor(Color.BLACK)
                leftText.setBackgroundResource(R.drawable.toggle_unselected)
                leftText.setTextColor(Color.WHITE)
                safetyLayout.visibility = View.GONE
                generalLayout.visibility = View.VISIBLE
            }
        }

        select(true)
        leftText.setOnClickListener { select(true) }
        rightText.setOnClickListener { select(false) }
    }

    // -----------------------------
    // Chips (Overall / Past X trips)
    // -----------------------------
    private fun setupChips(view: View) {
        val chips = listOf(
            view.findViewById<TextView>(R.id.tvOverall),
            view.findViewById<TextView>(R.id.tvPast10),
            view.findViewById<TextView>(R.id.tvPast20),
            view.findViewById<TextView>(R.id.tvPast30)
        )

        chips.forEachIndexed { index, chip ->
            chip.isEnabled = index == 0
            chip.isClickable = index == 0
            chip.alpha = if (index == 0) 1f else 0.4f
        }

        val defaultChip = chips[0]
        defaultChip.setBackgroundResource(R.drawable.bg_chip_selected)
        defaultChip.setTextColor(Color.parseColor("#6200EE"))

        chips.forEach { chip ->
            chip.setOnClickListener {
                chips.forEach {
                    it.setBackgroundResource(R.drawable.bg_chip_unselected)
                    it.setTextColor(Color.BLACK)
                }
                chip.setBackgroundResource(R.drawable.bg_chip_selected)
                chip.setTextColor(Color.parseColor("#6200EE"))
            }
        }
    }

    // -----------------------------
    // Demo / fallback values
    // -----------------------------
    private fun setupTextForGrid(view: View) {
        // Safety params
        val safetyValues = mapOf(
            R.id.itemSafetyScore to Pair("Safety Score", "92"),
            R.id.itemTrustLevel to Pair("Trust Level", "90"),
            R.id.itemAvgSpeed to Pair("Average Speed", "46 km/h"),
            R.id.itemMaxSpeed to Pair("Max Speed", "102 km/h"),
            R.id.itemPhoneUsage to Pair("Phone Usage", "50%"),
            R.id.itemUsageSpeed to Pair("Usage Speed", "40"),
            R.id.itemBraking to Pair("Braking", "50"),
            R.id.itemSpeeding to Pair("Speeding", "70")
        )

        safetyValues.forEach { (id, pair) ->
            val item = view.findViewById<View>(id)
            item.findViewById<TextView>(R.id.titleText).text = pair.first
            item.findViewById<TextView>(R.id.subtitleText).text = pair.second
        }

        // General params
        val sharedPrefs = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val tripCount = sharedPrefs.getInt("trip_count", 0)
        val totalDistance = sharedPrefs.getInt("total_distance", 0)
        val totalTimeMs = sharedPrefs.getLong("total_time_driven_ms", 0L)
        val hours = totalTimeMs / (1000 * 60 * 60)
        val minutes = (totalTimeMs / (1000 * 60)) % 60
        val timeFormatted = "${hours}h ${minutes}m"

        setCard(view.findViewById(R.id.itemTrips), "Trips", tripCount.toString())
        setCard(view.findViewById(R.id.itemTimeDriven), "Time Driven", timeFormatted)
        setCard(view.findViewById(R.id.itemMileage), "Mileage", "${totalDistance} km")
    }

    private fun setCard(item: View, title: String, value: String) {
        item.findViewById<TextView>(R.id.titleText).text = title
        item.findViewById<TextView>(R.id.subtitleText).text = value
    }

    // -----------------------------
    // API Values
    // -----------------------------
    private fun setDataIntoGrid(view: View, summary: SafetySummaryResponse) {
        val safetyValues = mapOf(
            R.id.itemSafetyScore to Pair("Safety Score", summary.safety_score.toInt().toString()),
            R.id.itemTrustLevel to Pair("Trust Level", calculateTrustLevel(summary.safety_score)),
            R.id.itemAvgSpeed to Pair("Average Speed", "${summary.average_speed_kmh} km/h"),
            R.id.itemMaxSpeed to Pair("Max Speed", "${summary.max_speed_kmh} km/h"),
            R.id.itemPhoneUsage to Pair("Phone Usage", "${summary.phone_usage_percentage}%"),
            R.id.itemUsageSpeed to Pair("Usage Speed", summary.average_speed_kmh.toString()),
            R.id.itemBraking to Pair("Braking", "-"),
            R.id.itemSpeeding to Pair("Speeding", "-")
        )

        safetyValues.forEach { (id, pair) ->
            val item = view.findViewById<View>(id)
            item.findViewById<TextView>(R.id.titleText).text = pair.first
            item.findViewById<TextView>(R.id.subtitleText).text = pair.second
        }

        setCard(view.findViewById(R.id.itemTrips), "Trips", summary.trips.toString())
        setCard(view.findViewById(R.id.itemTimeDriven), "Time Driven", "${summary.time_driven_minutes} min")
        setCard(view.findViewById(R.id.itemMileage), "Mileage", "${summary.mileage_km} km")
    }

    private fun calculateTrustLevel(score: Double) = when {
        score >= 90 -> "High"
        score >= 70 -> "Medium"
        else -> "Low"
    }
}



