package com.example.accuratedamoov.ui.dashboard

import android.app.AlertDialog
import android.content.Context
import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.Visibility
import com.example.accuratedamoov.MainActivity

import com.example.accuratedamoov.R
import com.example.accuratedamoov.data.local.systemevents.SystemEventDatabase
import com.example.accuratedamoov.data.model.SafetySummaryResponse
import com.example.accuratedamoov.ui.systemevents.adapter.SystemEventsAdapter
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch

class DashboardFragment : Fragment() {

    private lateinit var debugFab: FloatingActionButton
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
        debugFab = view.findViewById(R.id.debugFab)
        // ✅ Safety Score first
        statValueViews = listOf(
            view.findViewById<View>(R.id.safetyScoreItem).findViewById(R.id.statValue),
            view.findViewById<View>(R.id.tripsItem).findViewById(R.id.statValue),
            view.findViewById<View>(R.id.mileageItem).findViewById(R.id.statValue),
            view.findViewById<View>(R.id.timeDrivenItem).findViewById(R.id.statValue),
            view.findViewById<View>(R.id.avgSpeedItem).findViewById(R.id.statValue),
            view.findViewById<View>(R.id.maxSpeedItem).findViewById(R.id.statValue),
            view.findViewById<View>(R.id.phoneUsageItem).findViewById(R.id.statValue)
        )

        statNameViews = listOf(
            view.findViewById<View>(R.id.safetyScoreItem).findViewById(R.id.statName),
            view.findViewById<View>(R.id.tripsItem).findViewById(R.id.statName),
            view.findViewById<View>(R.id.mileageItem).findViewById(R.id.statName),
            view.findViewById<View>(R.id.timeDrivenItem).findViewById(R.id.statName),
            view.findViewById<View>(R.id.avgSpeedItem).findViewById(R.id.statName),
            view.findViewById<View>(R.id.maxSpeedItem).findViewById(R.id.statName),
            view.findViewById<View>(R.id.phoneUsageItem).findViewById(R.id.statName)
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
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                shimmerLayout.stopShimmer()
                shimmerLayout.visibility = View.GONE
            }
        }

        viewModel.errorProfile.observe(viewLifecycleOwner) { errorMsg ->
            errorMsg?.let { Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show() }
        }

        val sharedPrefs = context?.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val userId = sharedPrefs?.getInt("user_id", 0)
        val userName = sharedPrefs?.getString("user_name", "Unknown User") ?: "Unknown User"
        val mainActivity = activity as? MainActivity
        if (mainActivity != null && mainActivity.isNetworkAvailable()) {
            viewModel.fetchDashboardData(userId)
        }

        if (userName == "Unknown User" && mainActivity != null && mainActivity.isNetworkAvailable()) {
            viewModel.getUserProfile(userId)
        } else {
            driverName.text = userName
            driverName.visibility = View.VISIBLE
        }

        debugFab.setOnClickListener {
            val dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_system_events, null)

            val recyclerView = dialogView.findViewById<RecyclerView>(R.id.debugEventsRecycler)
            recyclerView.layoutManager = LinearLayoutManager(requireContext())
            val adapter = SystemEventsAdapter()
            recyclerView.adapter = adapter

            // Load events from DB (suspend → lifecycleScope)
            lifecycleScope.launch {
                val dao = SystemEventDatabase.getInstance(requireContext()).systemEventDao()
                val events =
                    dao.getAllEvents()  // make sure DAO has getAllEvents(): List<SystemEventEntity>
                adapter.submitList(events)
            }

            AlertDialog.Builder(requireContext())
                .setTitle("System Events Log(only for debug)")
                .setView(dialogView)
                .setPositiveButton("Close") { d, _ -> d.dismiss() }
                .show()
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

        // ✅ Safety Score at top
        safetyScoreBar.progress = summary.safety_score.toInt()
        rankText.text = "1"

        statValueViews[0].text = "${summary.safety_score}"
        statValueViews[1].text = "${summary.trips}"
        statValueViews[2].text = "${summary.mileage_km} km"
        statValueViews[3].text = "${summary.time_driven_minutes} min"
        statValueViews[4].text = "${summary.average_speed_kmh} km/h"
        statValueViews[5].text = "${summary.max_speed_kmh} km/h"
        statValueViews[6].text = "${summary.phone_usage_percentage}%"

        updateStatNames(
            listOf(
                "Safety Score",
                "Trips",
                "Mileage",
                "Time Driven",
                "Average Speed",
                "Max Speed",
                "Phone Usage"
            )
        )
    }

    private fun updateStatNames(names: List<String>) {
        statNameViews.forEachIndexed { index, textView ->
            textView.text = names.getOrNull(index) ?: ""
        }
    }
}


