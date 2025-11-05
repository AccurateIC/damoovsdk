package com.example.accuratedamoov.ui.dashboard

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
import pl.droidsonroids.gif.GifDrawable
import com.example.accuratedamoov.R
import com.skydoves.balloon.ArrowOrientation
import com.skydoves.balloon.ArrowPositionRules
import com.skydoves.balloon.Balloon
import com.skydoves.balloon.BalloonAnimation
import com.tomergoldst.tooltips.ToolTip
import com.tomergoldst.tooltips.ToolTipsManager


///stats
class DashboardFragment : Fragment() {


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val textView = view.findViewById<TextView>(R.id.titleText)
        val text = textView.text.toString()
        val gifDrawable = GifDrawable(resources, R.drawable.ic_cowboy_animated)
        gifDrawable.setBounds(0, 0, 80, 80) // size in px

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

        // Toggle buttons setup
        val leftText = view.findViewById<TextView>(R.id.leftBottomText)
        val rightText = view.findViewById<TextView>(R.id.rightBottomText)

        fun select(leftSelected: Boolean) {
            if (leftSelected) {
                leftText.setBackgroundResource(R.drawable.toggle_selected)
                leftText.setTextColor(Color.BLACK)
                rightText.setBackgroundResource(R.drawable.toggle_unselected)
                rightText.setTextColor(Color.WHITE)
            } else {
                rightText.setBackgroundResource(R.drawable.toggle_selected)
                rightText.setTextColor(Color.BLACK)
                leftText.setBackgroundResource(R.drawable.toggle_unselected)
                leftText.setTextColor(Color.WHITE)
            }
        }

        select(true)

        rightText.setOnClickListener { select(false) }
        val chips = listOf<TextView>(
            view.findViewById(R.id.tvOverall),
            view.findViewById(R.id.tvPast10),
            view.findViewById(R.id.tvPast20),
            view.findViewById(R.id.tvPast30)
        )

        chips.forEach { chip ->
            chip.setOnClickListener {
                chips.forEach {
                    it.setBackgroundResource(R.drawable.bg_chip_unselected)
                    it.setTextColor(Color.parseColor("#000000"))
                }

                chip.setBackgroundResource(R.drawable.bg_chip_selected)
                chip.setTextColor(Color.parseColor("#6200EE"))
            }
        }


        val leftBottomText = view.findViewById<TextView>(R.id.leftBottomText)
        val rightBottomText = view.findViewById<TextView>(R.id.rightBottomText)
        val safetyParamsLayout = view.findViewById<View>(R.id.safetyParamsLayout)
        val generalParamsLayout = view.findViewById<View>(R.id.generalParamsLayout)

        leftBottomText.setOnClickListener {
            select(true)
            safetyParamsLayout.visibility = View.VISIBLE
            generalParamsLayout.visibility = View.GONE
        }

        rightBottomText.setOnClickListener {
            select(false)
            safetyParamsLayout.visibility = View.GONE
            generalParamsLayout.visibility = View.VISIBLE
        }

        setupTextForGrid(view)

    }

    private fun setupTextForGrid(view: View) {
// Example inside an Activity or Fragment
        val itemSafetyScore = view.findViewById<View>(R.id.itemSafetyScore)
        val itemTrustLevel = view.findViewById<View>(R.id.itemTrustLevel)
        val itemAvgSpeed = view.findViewById<View>(R.id.itemAvgSpeed)
        val itemMaxSpeed = view.findViewById<View>(R.id.itemMaxSpeed)
        val itemPhoneUsage = view.findViewById<View>(R.id.itemPhoneUsage)
        val itemUsageSpeed = view.findViewById<View>(R.id.itemUsageSpeed)
        val itemBraking = view.findViewById<View>(R.id.itemBraking)
        val itemSpeeding = view.findViewById<View>(R.id.itemSpeeding)

// Now get titleText from each item
        val safetyScoreTitle = itemSafetyScore.findViewById<TextView>(R.id.titleText)
        val trustLevelTitle = itemTrustLevel.findViewById<TextView>(R.id.titleText)
        val avgSpeedTitle = itemAvgSpeed.findViewById<TextView>(R.id.titleText)
        val maxSpeedTitle = itemMaxSpeed.findViewById<TextView>(R.id.titleText)
        val phoneUsageTitle = itemPhoneUsage.findViewById<TextView>(R.id.titleText)
        val usageSpeedTitle = itemUsageSpeed.findViewById<TextView>(R.id.titleText)
        val brakingTitle = itemBraking.findViewById<TextView>(R.id.titleText)
        val speedingTitle = itemSpeeding.findViewById<TextView>(R.id.titleText)

// Set texts
        safetyScoreTitle.text = "Safety Score"
        trustLevelTitle.text = "Trust Level"
        avgSpeedTitle.text = "Average Speed"
        maxSpeedTitle.text = "Max Speed"
        phoneUsageTitle.text = "Phone Usage"
        usageSpeedTitle.text = "Usage Speed"
        brakingTitle.text = "Braking"
        speedingTitle.text = "Speeding"


        val safetyScoreValue = itemSafetyScore.findViewById<TextView>(R.id.subtitleText)
        val trustLevelValue = itemTrustLevel.findViewById<TextView>(R.id.subtitleText)
        val avgSpeedValue = itemAvgSpeed.findViewById<TextView>(R.id.subtitleText)
        val maxSpeedValue = itemMaxSpeed.findViewById<TextView>(R.id.subtitleText)
        val phoneUsageValue = itemPhoneUsage.findViewById<TextView>(R.id.subtitleText)
        val usageSpeedValue = itemUsageSpeed.findViewById<TextView>(R.id.subtitleText)
        val brakingValue = itemBraking.findViewById<TextView>(R.id.subtitleText)
        val speedingValue = itemSpeeding.findViewById<TextView>(R.id.subtitleText)

        // Set demo values
        safetyScoreValue.text = "92"
        trustLevelValue.text = "90"
        avgSpeedValue.text = "46 km/h"
        maxSpeedValue.text = "102 km/h"
        phoneUsageValue.text = "50%"
        usageSpeedValue.text = "40"
        brakingValue.text = "50"
        speedingValue.text = "70"

        // Get views
        val itemTrips = view.findViewById<View>(R.id.itemTrips)
        val itemTimeDriven = view.findViewById<View>(R.id.itemTimeDriven)
        val itemMileage = view.findViewById<View>(R.id.itemMileage)
        val itemUniqueTags = view.findViewById<View>(R.id.itemUniqueTags)

        // Helper function
        fun setCardData(item: View, title: String, value: String) {
            item.findViewById<TextView>(R.id.titleText).text = title
            item.findViewById<TextView>(R.id.subtitleText).text = value
        }

// Set titles and demo values
        setCardData(itemTrips, "Trips", "24")
        setCardData(itemTimeDriven, "Time Driven", "12h 35m")
        setCardData(itemMileage, "Mileage", "257 km")
        setCardData(itemUniqueTags, "Unique Tags", "8")


        // --- Tooltips setup ---
        val tooltips = mapOf(
            R.id.itemSafetyScore to "Overall safety rating based on driving behavior.",
            R.id.itemTrustLevel to "Reliability score from driving consistency.",
            R.id.itemAvgSpeed to "Average speed recorded during trips.",
            R.id.itemMaxSpeed to "Highest speed achieved during trips.",
            R.id.itemPhoneUsage to "Frequency of mobile phone usage while driving.",
            R.id.itemUsageSpeed to "Average speed during phone usage events.",
            R.id.itemBraking to "Count of harsh braking events.",
            R.id.itemSpeeding to "Instances of overspeeding.",
            R.id.itemTrips to "Total number of trips completed.",
            R.id.itemTimeDriven to "Cumulative driving time.",
            R.id.itemMileage to "Total distance covered in all trips.",
            R.id.itemUniqueTags to "Unique identifiers or trip tags."
        )

        for ((id, text) in tooltips) {
            val item = view.findViewById<View>(id)
            val infoIcon = item.findViewById<ImageView>(R.id.infoIcon)

            infoIcon.setOnClickListener { iconView ->
                showCustomTooltip(iconView, text)
            }
        }


    }



    private fun showCustomTooltip(anchor: View, message: String) {
        val context = anchor.context

        // Create the custom layout
        val customView = LayoutInflater.from(context)
            .inflate(R.layout.layout_custom_tooltip, null)
        val tooltipText = customView.findViewById<TextView>(R.id.tooltipText)
        tooltipText.text = message

        // Measure available space
        val location = IntArray(2)
        anchor.getLocationOnScreen(location)
        val screenHeight = context.resources.displayMetrics.heightPixels
        val spaceAbove = location[1]
        val spaceBelow = screenHeight - (location[1] + anchor.height)
        val showAbove = spaceAbove > spaceBelow

        // Build Balloon tooltip
        val balloon = Balloon.Builder(context)
            .setLayout(R.layout.layout_custom_tooltip)
            .setArrowSize(10)
            .setArrowPosition(0.5f)
            .setArrowOrientation(
                if (showAbove) ArrowOrientation.BOTTOM else ArrowOrientation.TOP
            )
            .setArrowPositionRules(ArrowPositionRules.ALIGN_ANCHOR)
            .setCornerRadius(8f)
            .setBackgroundColorResource(R.color.colorPrimaryVariant)
            .setBalloonAnimation(BalloonAnimation.FADE)
            .setIsVisibleOverlay(false)
            .setAutoDismissDuration(2000L)
            .build()

        // Set text dynamically after building
        val balloonText = balloon.getContentView().findViewById<TextView>(R.id.tooltipText)
        balloonText.text = message

        // Show aligned based on available space
        if (showAbove) {
            balloon.showAlignTop(anchor)
        } else {
            balloon.showAlignBottom(anchor)
        }
    }








    override fun onDestroyView() {
        super.onDestroyView()
    }
}

