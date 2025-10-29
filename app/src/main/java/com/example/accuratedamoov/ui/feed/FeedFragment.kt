package com.example.accuratedamoov.ui.feed

import android.Manifest
import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.accuratedamoov.MainActivity
import com.example.accuratedamoov.data.model.TripData
import com.example.accuratedamoov.databinding.FragmentDashboardBinding
import com.example.accuratedamoov.databinding.FragmentFeedBinding
import com.example.accuratedamoov.model.FeedUiState
import com.example.accuratedamoov.ui.feed.adapter.DateAdapter
import com.example.accuratedamoov.ui.feed.adapter.ShimmerAdapter
import com.example.accuratedamoov.ui.feed.adapter.TrackAdapter
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.accuratedamoov.R
import com.example.accuratedamoov.ui.feed.filtedialog.CustomDatePickerDialog
import com.example.accuratedamoov.ui.feed.filtedialog.FilterPopup
import java.util.Calendar
import kotlin.let


class FeedFragment : Fragment() {

    private var _binding: FragmentFeedBinding? = null
    private val binding get() = _binding!!

    private val feedViewModel: FeedViewModel by viewModels()
    private lateinit var trackAdapter: TrackAdapter
    private lateinit var shimmerAdapter: ShimmerAdapter

    private var allTrips: List<TripData> = emptyList()

    private val dateParser = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val displayDateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    // Filter selections
    private var selectedStartDate: Date? = null
    private var selectedEndDate: Date? = null
    private var selectedDistanceRange: Pair<Float, Float>? = null
    private var selectedTimeRange: Pair<Int, Int>? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFeedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupRecyclerView()
        setupDefaults()
        setupFilters()
        observeData()
    }

    private fun setupRecyclerView() {
        binding.recycleView.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
        }
        trackAdapter = TrackAdapter(emptyList(), requireContext())
        shimmerAdapter = ShimmerAdapter(6)
        binding.recycleView.adapter = trackAdapter
    }

    private fun setupDefaults() {
        val mainActivity = activity as? MainActivity
        if (mainActivity?.isNetworkAvailable() == true) {
            feedViewModel.loadTripsIfNeeded()
        }

        val today = Calendar.getInstance()
        binding.filterDate.text = displayDateFormat.format(today.time)

        binding.swipeRefreshLayout.setOnRefreshListener {
            if (mainActivity?.isNetworkAvailable() == true) {
                feedViewModel.fetchTrips()
            }
            binding.swipeRefreshLayout.isRefreshing = false
        }
    }

    // -----------------------------
    // FILTER SETUP
    // -----------------------------
    private fun setupFilters() {
        setupDateFilter()
        setupDistanceFilter()
        setupTimeFilter()
    }

    private fun setupDateFilter() {
        binding.filterDate.setOnClickListener {
            CustomDatePickerDialog(requireContext()) { start, end ->
                selectedStartDate = start.time
                selectedEndDate = end.time
                binding.filterDate.apply {
                    text = "${displayDateFormat.format(start.time)} → ${displayDateFormat.format(end.time)}"
                    setBackgroundResource(R.drawable.bg_filter_selected)
                }
                applyAllFilters()
            }.apply {
                // Disallow future dates
                setMaxDate(Calendar.getInstance().timeInMillis)
            }.show()
        }
    }

    private fun setupDistanceFilter() {
        val distanceOptions = listOf("0–5 km", "5–10 km", "10–20 km", "20–50 km", "50–100 km", "100+ km")

        binding.filterDistance.setOnClickListener {
            FilterPopup(requireContext(), binding.filterDistance, "Select Distance", distanceOptions) { selected ->
                selectedDistanceRange = when (selected) {
                    "0–5 km" -> 0f to 5f
                    "5–10 km" -> 5f to 10f
                    "10–20 km" -> 10f to 20f
                    "20–50 km" -> 20f to 50f
                    "50–100 km" -> 50f to 100f
                    "100+ km" -> 100f to Float.MAX_VALUE
                    else -> null
                }
                binding.filterDistance.apply {
                    text = selected
                    setBackgroundResource(R.drawable.bg_filter_selected)
                }
                applyAllFilters()
            }.show()
        }
    }

    private fun setupTimeFilter() {
        val timeOptions = listOf("0–30 min", "30–60 min", "1–2 hrs", "2–3 hrs", "3–5 hrs", "5+ hrs")

        binding.filterTime.setOnClickListener {
            FilterPopup(requireContext(), binding.filterTime, "Select Time Travelled", timeOptions) { selected ->
                selectedTimeRange = when (selected) {
                    "0–30 min" -> 0 to 30
                    "30–60 min" -> 30 to 60
                    "1–2 hrs" -> 60 to 120
                    "2–3 hrs" -> 120 to 180
                    "3–5 hrs" -> 180 to 300
                    "5+ hrs" -> 300 to Int.MAX_VALUE
                    else -> null
                }
                binding.filterTime.apply {
                    text = selected
                    setBackgroundResource(R.drawable.bg_filter_selected)
                }
                applyAllFilters()
            }.show()
        }
    }

    // -----------------------------
    // FILTER LOGIC
    // -----------------------------
    private fun applyAllFilters() {
        val filtered = allTrips.filter { trip ->
            try {
                val tripDate = dateParser.parse(trip.start_date_ist.toString())
                val distanceKm = trip.distance_km
                val durationMinutes = parseDurationToMinutes(trip.duration_hh_mm)

                val matchesDate = tripDate != null &&
                        (selectedStartDate == null || !tripDate.before(selectedStartDate)) &&
                        (selectedEndDate == null || !tripDate.after(selectedEndDate))

                val matchesDistance = selectedDistanceRange?.let { distanceKm in it.first..it.second } ?: true
                val matchesTime = selectedTimeRange?.let { durationMinutes in it.first..it.second } ?: true

                matchesDate && matchesDistance && matchesTime
            } catch (e: Exception) {
                false
            }
        }

        updateTripList(filtered)
    }

    // -----------------------------
    // DATA OBSERVATION
    // -----------------------------
    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            feedViewModel.uiState.collect { state ->
                when (state) {
                    is FeedUiState.Loading -> {}
                    is FeedUiState.Success -> {
                        allTrips = state.trips
                        applyAllFilters() // Retain filters on reload
                        restoreFilterTexts()
                    }
                    is FeedUiState.Error -> {
                        showOnly(binding.tvZeroTrips)
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun restoreFilterTexts() {
        selectedStartDate?.let { start ->
            selectedEndDate?.let { end ->
                binding.filterDate.text = "${displayDateFormat.format(start)} → ${displayDateFormat.format(end)}"
            }
        }
        selectedDistanceRange?.let {
            binding.filterDistance.setBackgroundResource(R.drawable.bg_filter_selected)
        }
        selectedTimeRange?.let {
            binding.filterTime.setBackgroundResource(R.drawable.bg_filter_selected)
        }
    }

    // -----------------------------
    // UI HELPERS
    // -----------------------------
    @SuppressLint("NotifyDataSetChanged")
    private fun updateTripList(trips: List<TripData>) {
        viewLifecycleOwner.lifecycleScope.launch {
            delay(250)
            trackAdapter.updateData(trips)
            showOnly(if (trips.isNotEmpty()) binding.recycleView else binding.tvZeroTrips)
        }
    }

    private fun showOnly(viewToShow: View) {
        binding.recycleView.visibility = if (viewToShow == binding.recycleView) View.VISIBLE else View.GONE
        binding.tvZeroTrips.visibility = if (viewToShow == binding.tvZeroTrips) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


    private fun parseDurationToMinutes(duration: Any?): Int {
        return when (duration) {
            is Int -> duration // already in minutes
            is String -> {
                try {
                    // Example format: "01:35" (1 hour 35 mins)
                    val parts = duration.split(":")
                    if (parts.size == 2) {
                        val hours = parts[0].toIntOrNull() ?: 0
                        val minutes = parts[1].toIntOrNull() ?: 0
                        hours * 60 + minutes
                    } else duration.toIntOrNull() ?: 0
                } catch (e: Exception) {
                    0
                }
            }
            else -> 0
        }
    }


}




