package com.example.accuratedamoov.ui.feed

import android.Manifest
import android.annotation.SuppressLint
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
import kotlinx.coroutines.delay

import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale


class FeedFragment : Fragment() {

    private var _binding: FragmentFeedBinding? = null
    private val binding get() = _binding!!
    private val feedViewModel: FeedViewModel by viewModels()
    private var allTrips: List<TripData> = emptyList()
    private var selectedDate: String? = null
    private lateinit var trackAdapter: TrackAdapter
    private lateinit var shimmerAdapter: ShimmerAdapter
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFeedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recycleView.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
        }

        trackAdapter = TrackAdapter(emptyList(), requireContext())
        binding.recycleView.adapter = trackAdapter
        shimmerAdapter = ShimmerAdapter(6)
        binding.shimmerRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.shimmerRecyclerView.adapter = shimmerAdapter

        binding.shimmerLayout.startShimmer()
        binding.shimmerLayout.visibility = View.VISIBLE
        val dividerItemDecoration = DividerItemDecoration(context, LinearLayoutManager.VERTICAL)
        binding.recycleView.addItemDecoration(dividerItemDecoration)
        val mainActivity = activity as? MainActivity
        if(mainActivity!= null && mainActivity.isNetworkAvailable()) {
            feedViewModel.loadTripsIfNeeded()
        }
        observeData()

        binding.swipeRefreshLayout.setOnRefreshListener {
            if(mainActivity!= null && mainActivity.isNetworkAvailable()) {
                feedViewModel.fetchTrips()
            }
            binding.swipeRefreshLayout.isRefreshing = false
        }
        val dateAdapter = DateAdapter { date ->
            selectedDate = date
            binding.tvDateLabel.text = date
            filterTripsByDate()
        }
        dateAdapter.clearSelection()

        binding.dateRecyclerView.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.dateRecyclerView.adapter = dateAdapter

        // ✅ Always select Today at launch
        selectedDate = "Today"
        binding.tvDateLabel.text = "Today"
        filterTripsByDate()

        binding.dateRecyclerView.post {
            binding.dateRecyclerView.scrollToPosition(dateAdapter.itemCount - 1)
        }

        binding.tvToday.setOnClickListener {
            selectedDate = "Today"
            binding.tvDateLabel.text = "Today"
            dateAdapter.clearSelection()

            val lastPos = dateAdapter.itemCount - 1
            if (lastPos >= 0) {
                binding.dateRecyclerView.smoothScrollToPosition(lastPos)
            }
            filterTripsByDate()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            feedViewModel.uiState.collect { state ->
                when (state) {
                    is FeedUiState.Loading -> {
                        binding.shimmerLayout.startShimmer()
                        showOnly(binding.shimmerLayout)
                    }

                    is FeedUiState.Success -> {
                        binding.shimmerLayout.stopShimmer()
                        if (state.trips.isNotEmpty()) {
                            allTrips = state.trips
                            filterTripsByDate()
                            showOnly(binding.recycleView)
                        } else {
                            showOnly(binding.tvZeroTrips)
                        }
                    }

                    is FeedUiState.Error -> {
                        binding.shimmerLayout.stopShimmer()
                        showOnly(binding.tvZeroTrips)
                        Toast.makeText(
                            requireContext(),
                            "Error: ${state.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun filterTripsByDate() {
        _binding?.let { binding ->
            binding.shimmerLayout.startShimmer()
            showOnly(binding.shimmerLayout)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            delay(800) // short shimmer delay for smoothness

            val dateFormatInput = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val dateFormatOutput = SimpleDateFormat("dd MMM", Locale.getDefault())
            val todayString = dateFormatOutput.format(Calendar.getInstance().time)

            val targetDate = if (selectedDate == "Today") todayString else selectedDate

            val filteredTrips = if (targetDate.isNullOrEmpty()) {
                allTrips
            } else {
                allTrips.filter { trip ->
                    try {
                        val parsedDate = dateFormatInput.parse(trip.start_date_ist)
                        val formattedDate = dateFormatOutput.format(parsedDate!!)
                        formattedDate == targetDate
                    } catch (e: Exception) {
                        false
                    }
                }
            }

            _binding?.let { binding ->
                trackAdapter.updateData(filteredTrips)

                if (filteredTrips.isNotEmpty()) {
                    showOnly(binding.recycleView)
                } else {
                    showOnly(binding.tvZeroTrips)
                }

                // stop shimmer
                binding.shimmerLayout.stopShimmer()
            }
        }
    }

    private fun showOnly(viewToShow: View) {
        binding.recycleView.visibility = if (viewToShow == binding.recycleView) View.VISIBLE else View.GONE
        binding.shimmerLayout.visibility = if (viewToShow == binding.shimmerLayout) View.VISIBLE else View.GONE
        binding.tvZeroTrips.visibility = if (viewToShow == binding.tvZeroTrips) View.VISIBLE else View.GONE
    }
}

