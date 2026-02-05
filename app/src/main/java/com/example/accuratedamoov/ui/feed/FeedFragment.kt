package com.example.accuratedamoov.ui.feed

import android.Manifest
import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
import kotlin.math.ceil
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels


class FeedFragment : Fragment() {

    private var _binding: FragmentFeedBinding? = null
    private val binding get() = _binding!!

    private val feedViewModel: FeedViewModel by activityViewModels()
    private lateinit var trackAdapter: TrackAdapter
    private lateinit var shimmerAdapter: ShimmerAdapter

    private var allTrips: List<TripData> = emptyList()
    private var filteredTrips: List<TripData> = emptyList()

    private val dateParser = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val displayDateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    // Filters
    private var selectedStartDate: Date? = null
    private var selectedEndDate: Date? = null
    private var selectedDistanceRange: Pair<Float, Float>? = null
    private var selectedTimeRange: Pair<Int, Int>? = null

    // Pagination
    private var currentPage = 1
    private val pageSize = 5
    private var totalPages = 1

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
        setupPaginationControls()
        observeData()

        binding.clearFilters.setOnClickListener {
            selectedStartDate = null
            selectedEndDate = null
            selectedDistanceRange = null
            selectedTimeRange = null

            binding.filterDate.text = "Select Date"
            binding.filterDistance.text = "Distance"
            binding.filterTime.text = "Time Travelled"

            binding.filterDate.setBackgroundResource(R.drawable.filter_bg)
            binding.filterDistance.setBackgroundResource(R.drawable.filter_bg)
            binding.filterTime.setBackgroundResource(R.drawable.filter_bg)

            filterTrips()
        }

        binding.clearFilters.post {
            binding.filterScroll.smoothScrollTo(
                binding.clearFilters.right,
                0
            )
        }

    }

    // -----------------------------
    // RecyclerView Setup
    // -----------------------------
    private fun setupRecyclerView() {
        binding.recycleView.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
        }
        trackAdapter = TrackAdapter(emptyList(), requireContext())
        shimmerAdapter = ShimmerAdapter(6)
        binding.recycleView.adapter = trackAdapter

        binding.recycleView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return
                val firstVisible = layoutManager.findFirstVisibleItemPosition()
                val lastVisible = layoutManager.findLastVisibleItemPosition()
                val totalScrolled = lastVisible + 1 // number of items scrolled past

                // Show after scrolling past 5 items
                if (totalScrolled >= 5 && binding.pageControls.visibility != View.VISIBLE) {
                    binding.pageControls.animate()
                        .alpha(1f)
                        .setDuration(250)
                        .withStartAction {
                            binding.pageControls.visibility = View.VISIBLE
                        }
                        .start()
                }
                // Hide if user scrolls back up near top
                else if (totalScrolled <= 5 && binding.pageControls.isVisible) {
                    binding.pageControls.animate()
                        .alpha(0f)
                        .setDuration(250)
                        .withEndAction {
                            binding.pageControls.visibility = View.GONE
                        }
                        .start()
                }
            }
        })

    }

    // -----------------------------
    // Defaults
    // -----------------------------
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
    // Filters
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
                    text =
                        "${displayDateFormat.format(start.time)} → ${displayDateFormat.format(end.time)}"
                    setBackgroundResource(R.drawable.bg_filter_selected)
                }
                filterTrips()
            }.apply {
                setMaxDate(Calendar.getInstance().timeInMillis)
                setMinDate(Calendar.getInstance().apply { add(Calendar.YEAR, -10) }.timeInMillis)

            }.show()
        }
    }

    private fun setupDistanceFilter() {
        val distanceOptions =
            listOf("0–5 km", "5–10 km", "10–20 km", "20–50 km", "50–100 km", "100+ km")

        binding.filterDistance.setOnClickListener {
            FilterPopup(
                requireContext(),
                binding.filterDistance,
                "Select Distance",
                distanceOptions
            ) { selected ->
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
                filterTrips()
            }.show()
        }
    }

    private fun setupTimeFilter() {
        val timeOptions = listOf("0–30 min", "30–60 min", "1–2 hrs", "2–3 hrs", "3–5 hrs", "5+ hrs")

        binding.filterTime.setOnClickListener {
            FilterPopup(
                requireContext(),
                binding.filterTime,
                "Select Time Travelled",
                timeOptions
            ) { selected ->
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
                filterTrips()
            }.show()
        }
    }

    // -----------------------------
    // Data Observation
    // -----------------------------
    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            feedViewModel.uiState.collect { state ->
                when (state) {
                    is FeedUiState.Loading -> {

                        binding.recycleView.adapter = shimmerAdapter
                    }
                    is FeedUiState.Success -> {
                        binding.recycleView.adapter = trackAdapter
                        allTrips = state.trips
                        filterTrips()
                        restoreFilterTexts()
                    }

                    is FeedUiState.Error -> {
                        binding.recycleView.adapter = trackAdapter
                        showOnly(binding.tvZeroTrips)
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    // -----------------------------
    // Filtering & Pagination Logic
    // -----------------------------
    private fun filterTrips() {
        filteredTrips = allTrips.filter { trip ->
            try {
                val tripDate = dateParser.parse(trip.start_date_ist.toString())
                val distanceKm = trip.distance_km
                val durationMinutes = parseDurationToMinutes(trip.duration_hh_mm)

                val matchesDate = tripDate != null &&
                        (selectedStartDate == null || !tripDate.before(selectedStartDate)) &&
                        (selectedEndDate == null || !tripDate.after(selectedEndDate))

                val matchesDistance =
                    selectedDistanceRange?.let { distanceKm in it.first..it.second } ?: true
                val matchesTime =
                    selectedTimeRange?.let { durationMinutes in it.first..it.second } ?: true

                matchesDate && matchesDistance && matchesTime
            } catch (e: Exception) {
                false
            }
        }

        totalPages = ceil(filteredTrips.size / pageSize.toDouble()).toInt().coerceAtLeast(1)
        currentPage = 1
        showPage(currentPage)
        updateClearVisibility()
    }

    private fun setupPaginationControls() {

        binding.prevPageBtn.setOnClickListener {
            if (currentPage > 1) {
                currentPage--
                showPage(currentPage)
            }
        }

        binding.nextPageBtn.setOnClickListener {
            if (currentPage < totalPages) {
                currentPage++
                showPage(currentPage)
            }
        }
    }

    private fun showPage(page: Int) {
        val fromIndex = (page - 1) * pageSize
        val toIndex = minOf(fromIndex + pageSize, filteredTrips.size)
        val pageItems = if (fromIndex < filteredTrips.size) filteredTrips.subList(
            fromIndex,
            toIndex
        ) else emptyList()

        trackAdapter.updateData(pageItems)
        showOnly(if (pageItems.isNotEmpty()) binding.recycleView else binding.tvZeroTrips)

        updatePageNumbers()
    }

    // -----------------------------
// Page Number UI Updater
// -----------------------------
    private fun updatePageNumbers() {
        val container = binding.pageNumbersContainer
        container.removeAllViews()

        if (totalPages <= 1) return

        val displayMetrics = resources.displayMetrics
        val size = (40 * displayMetrics.density).toInt() // ~40dp square

// Calculate which 3 numbers to show around currentPage
        val startPage = maxOf(1, currentPage - 1)
        val endPage = minOf(totalPages, startPage + 2)

        for (page in startPage..endPage) {
            val textView = TextView(requireContext()).apply {
                text = page.toString()
                textSize = 14f
                gravity = Gravity.CENTER
                setTextColor(
                    if (page == currentPage) Color.WHITE
                    else Color.BLACK
                )
                background = ContextCompat.getDrawable(
                    requireContext(),
                    if (page == currentPage)
                        R.drawable.rect_active_bg // highlight current
                    else
                        R.drawable.filter_bg // normal style
                )
                setOnClickListener {
                    currentPage = page
                    showPage(currentPage)
                }
            }

            val params = LinearLayout.LayoutParams(size, size)
            val margin = (5 * displayMetrics.density).toInt()
            params.setMargins(margin, margin, margin, margin)
            textView.layoutParams = params

            container.addView(textView)
        }


        // Update arrow states (enable/disable)
        binding.prevPageBtn.alpha = if (currentPage > 1) 1f else 0.3f
        binding.nextPageBtn.alpha = if (currentPage < totalPages) 1f else 0.3f
    }


    // -----------------------------
    // Helpers
    // -----------------------------
    private fun parseDurationToMinutes(duration: Any?): Int {
        return when (duration) {
            is Int -> duration
            is String -> {
                val parts = duration.split(":")
                if (parts.size == 2) {
                    val hours = parts[0].toIntOrNull() ?: 0
                    val minutes = parts[1].toIntOrNull() ?: 0
                    hours * 60 + minutes
                } else duration.toIntOrNull() ?: 0
            }

            else -> 0
        }
    }

    private fun showOnly(viewToShow: View) {
        binding.recycleView.visibility =
            if (viewToShow == binding.recycleView) View.VISIBLE else View.GONE
        binding.tvZeroTrips.visibility =
            if (viewToShow == binding.tvZeroTrips) View.VISIBLE else View.GONE
    }

    private fun restoreFilterTexts() {
        selectedStartDate?.let { start ->
            selectedEndDate?.let { end ->
                binding.filterDate.text =
                    "${displayDateFormat.format(start)} → ${displayDateFormat.format(end)}"
            }
        }
        selectedDistanceRange?.let {
            binding.filterDistance.setBackgroundResource(R.drawable.bg_filter_selected)
        }
        selectedTimeRange?.let {
            binding.filterTime.setBackgroundResource(R.drawable.bg_filter_selected)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun updateClearVisibility() {
        binding.clearFilters.visibility =
            if (selectedStartDate != null ||
                selectedDistanceRange != null ||
                selectedTimeRange != null
            ) View.VISIBLE else View.GONE
    }

}




