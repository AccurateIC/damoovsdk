package com.example.accuratedamoov.ui.dashboard

import android.content.Context
import androidx.fragment.app.viewModels
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView

import com.example.accuratedamoov.R
import com.example.accuratedamoov.data.model.SafetySummaryResponse
import com.facebook.shimmer.ShimmerFrameLayout

class DashboardFragment : Fragment() {

    private val viewModel: DashboardViewModel by viewModels()

    private lateinit var profileCard: CardView
    private lateinit var shimmerLayout: ShimmerFrameLayout
    private lateinit var driverName: TextView
    private lateinit var safetyScoreBar: ProgressBar
    private lateinit var rankText: TextView
    private lateinit var statValueViews: List<TextView>
    private lateinit var statNameViews: List<TextView>
    private lateinit var summaryScrollView: ScrollView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_dashboard, container, false)

        profileCard = view.findViewById(R.id.profileCard)
        shimmerLayout = view.findViewById(R.id.shimmerLayout)
        summaryScrollView = view.findViewById(R.id.summaryScrollView)

        driverName = view.findViewById(R.id.driverName)
        safetyScoreBar = view.findViewById(R.id.safetyScoreBar)
        rankText = view.findViewById(R.id.rankText)

        statValueViews = listOf(
            view.findViewById<View>(R.id.tripsItem).findViewById(R.id.statValue),
            view.findViewById<View>(R.id.driverTripsItem).findViewById(R.id.statValue),
            view.findViewById<View>(R.id.mileageItem).findViewById(R.id.statValue),
            view.findViewById<View>(R.id.timeDrivenItem).findViewById(R.id.statValue),
            view.findViewById<View>(R.id.avgSpeedItem).findViewById(R.id.statValue),
            view.findViewById<View>(R.id.maxSpeedItem).findViewById(R.id.statValue),
            view.findViewById<View>(R.id.phoneUsageItem).findViewById(R.id.statValue),
            view.findViewById<View>(R.id.speedingItem).findViewById(R.id.statValue),
            view.findViewById<View>(R.id.phoneUsageSpeedingItem).findViewById(R.id.statValue),
            view.findViewById<View>(R.id.uniqueTagsItem).findViewById(R.id.statValue)
        )

        statNameViews = listOf(
            view.findViewById<View>(R.id.tripsItem).findViewById(R.id.statName),
            view.findViewById<View>(R.id.driverTripsItem).findViewById(R.id.statName),
            view.findViewById<View>(R.id.mileageItem).findViewById(R.id.statName),
            view.findViewById<View>(R.id.timeDrivenItem).findViewById(R.id.statName),
            view.findViewById<View>(R.id.avgSpeedItem).findViewById(R.id.statName),
            view.findViewById<View>(R.id.maxSpeedItem).findViewById(R.id.statName),
            view.findViewById<View>(R.id.phoneUsageItem).findViewById(R.id.statName),
            view.findViewById<View>(R.id.speedingItem).findViewById(R.id.statName),
            view.findViewById<View>(R.id.phoneUsageSpeedingItem).findViewById(R.id.statName),
            view.findViewById<View>(R.id.uniqueTagsItem).findViewById(R.id.statName)
        )

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        showLoading()

        viewModel.summary.observe(viewLifecycleOwner) { summary ->
            if (summary != null) {
                showDashboardData(summary)
            } else {
                showLoading()
            }
        }

        viewModel.userProfile.observe(viewLifecycleOwner) { profile ->
            profile?.let {
                driverName.text = it.name
                driverName.visibility = View.VISIBLE
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let { Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show() }
        }

        viewModel.errorProfile.observe(viewLifecycleOwner) { errorMsg ->
            errorMsg?.let { Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show() }
        }

        viewModel.fetchDashboardData("last_1_week")

        val sharedPrefs = context?.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val userId = sharedPrefs?.getInt("user_id", 0)
        val userName = sharedPrefs?.getString("user_name", "Unknown User") ?: "Unknown User"

        if (userName == "Unknown User") {
            viewModel.getUserProfile(userId)
        } else {
            driverName.text = userName
            driverName.visibility = View.VISIBLE
        }
    }

    private fun showLoading() {
        shimmerLayout.visibility = View.VISIBLE
        shimmerLayout.startShimmer()
        summaryScrollView.visibility = View.GONE
    }

    private fun showDashboardData(summary: SafetySummaryResponse) {
        shimmerLayout.stopShimmer()
        shimmerLayout.visibility = View.GONE
        summaryScrollView.visibility = View.VISIBLE

        driverName.visibility = View.VISIBLE
        safetyScoreBar.visibility = View.VISIBLE
        rankText.visibility = View.VISIBLE
        statValueViews.forEach { it.visibility = View.VISIBLE }
        statNameViews.forEach { it.visibility = View.VISIBLE }

        safetyScoreBar.progress = (summary.safety_score ?: 0.0).toInt()
        rankText.text = "#${(100 - (summary.safety_score ?: 0.0)).toInt()}"

        statValueViews[0].text = "${summary.trips ?: 0}"
        statValueViews[1].text = "${summary.driver_trips ?: 0}"
        statValueViews[2].text = "${summary.mileage_km ?: 0.0} km"
        statValueViews[3].text = "${summary.time_driven_minutes ?: 0.0} min"
        statValueViews[4].text = "${summary.average_speed_kmh ?: 0.0} km/h"
        statValueViews[5].text = "${summary.max_speed_kmh ?: 0.0} km/h"
        statValueViews[6].text = "${summary.phone_usage_percentage ?: 0.0}%"
        statValueViews[7].text = "${summary.speeding_percentage ?: 0.0}%"
        statValueViews[8].text = "${summary.phone_usage_speeding_percentage ?: 0.0}%"
        statValueViews[9].text = "${summary.unique_tags_count ?: 0}"

        updateStatNames(
            listOf(
                "Trips",
                "Driver Trips",
                "Mileage",
                "Time Driven",
                "Average Speed",
                "Max Speed",
                "Phone Usage",
                "Speeding",
                "Phone Usage Speeding",
                "Unique Tags"
            )
        )
    }

    private fun updateStatNames(names: List<String>) {
        statNameViews.forEachIndexed { index, textView ->
            textView.text = names.getOrNull(index) ?: ""
        }
    }
}


