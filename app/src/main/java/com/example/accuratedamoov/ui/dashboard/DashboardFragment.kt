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
import android.widget.TextView
import android.widget.Toast

import com.example.accuratedamoov.R
import com.example.accuratedamoov.data.model.SafetySummary

class DashboardFragment : Fragment() {

    private val viewModel: DashboardViewModel by viewModels()

    private lateinit var driverName: TextView
    private lateinit var safetyScoreBar: ProgressBar
    private lateinit var rankText: TextView
    private lateinit var statViews: List<TextView> // from stat_item includes
    private lateinit var statValueViews: List<TextView>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_dashboard, container, false)
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

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.summary.observe(viewLifecycleOwner) { summary ->
            summary?.let { updateUI(it.data) }
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let { Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show() }
        }

        viewModel.userProfile.observe(viewLifecycleOwner) { profile ->
            profile?.let {
                view.findViewById<TextView>(R.id.driverName).text = "Name: ${it.name}"
            }
        }
        viewModel.errorProfile.observe(viewLifecycleOwner) { errorMsg ->
            errorMsg?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            }
        }


        // Call API (example filter: "weekly")
        //viewModel.fetchDashboardData("weekly")
        val sharedPrefs = context?.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val userId = sharedPrefs?.getInt("user_id", 0)
        val userName = sharedPrefs?.getString("user_name", "Unknown User") ?: "Unknown User"
        if (userName == "Unknown User") {
            // If no cache → fetch from API
            Log.d("API_for_profile", "Fetching user profile from API")
            viewModel.getUserProfile(userId)
        } else {
            // Use cached profile directly
            Log.d("API_for_profile", "Fetching user profile from shared preferences")
            driverName.text = userName
        }
    }

    private fun updateUI(summary: SafetySummary?) {
        driverName.text = "Kevin Watkins"

        // Safety score bar (default to 0 if null)
        safetyScoreBar.progress = (summary?.safety_score ?: 0.0).toInt()

        // Rank calculation (default to 0 if null)
        rankText.text = "#${(100 - (summary?.safety_score ?: 0.0)).toInt()}"

        statValueViews[0].text = "${summary?.trips ?: 0}"
        statValueViews[1].text = "${summary?.driver_trips ?: 0}"
        statValueViews[2].text = "${summary?.mileage_km ?: 0.0} km"
        statValueViews[3].text = "${summary?.time_driven_minutes ?: 0.0} min"
        statValueViews[4].text = "${summary?.average_speed_kmh ?: 0.0} km/h"
        statValueViews[5].text = "${summary?.max_speed_kmh ?: 0.0} km/h"
        statValueViews[6].text = "${summary?.phone_usage_percentage ?: 0.0}%"
        statValueViews[7].text = "${summary?.speeding_percentage ?: 0.0}%"
        statValueViews[8].text = "${summary?.phone_usage_speeding_percentage ?: 0.0}%"
        statValueViews[9].text = "${summary?.unique_tags_count ?: 0}"
    }



    private fun getStatValue(view: View): TextView {
        return view.findViewById(R.id.statValue)
    }
}

