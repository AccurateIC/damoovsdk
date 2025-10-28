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

        // RecyclerView setup
        binding.recycleView.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
        }

        trackAdapter = TrackAdapter(emptyList(), requireContext())
        binding.recycleView.adapter = trackAdapter

        // Shimmer setup
        shimmerAdapter = ShimmerAdapter(6)


       /* // Divider between trip cards
        val dividerItemDecoration = DividerItemDecoration(context, LinearLayoutManager.VERTICAL)
        binding.recycleView.addItemDecoration(dividerItemDecoration)*/

        val mainActivity = activity as? MainActivity
        if (mainActivity != null && mainActivity.isNetworkAvailable()) {
            feedViewModel.loadTripsIfNeeded()
        }

        observeData()

        // Swipe-to-refresh
        binding.swipeRefreshLayout.setOnRefreshListener {
            if (mainActivity != null && mainActivity.isNetworkAvailable()) {
                feedViewModel.fetchTrips()
            }
            binding.swipeRefreshLayout.isRefreshing = false
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

                    }

                    is FeedUiState.Success -> {
                        if (state.trips.isNotEmpty()) {
                            allTrips = state.trips
                            updateTripList(allTrips)
                            showOnly(binding.recycleView)
                        } else {
                            showOnly(binding.tvZeroTrips)
                        }
                    }

                    is FeedUiState.Error -> {
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
    private fun updateTripList(trips: List<TripData>) {
        viewLifecycleOwner.lifecycleScope.launch {
            // short shimmer delay for smooth UI
            delay(500)
            trackAdapter.updateData(trips)

            if (trips.isNotEmpty()) {
                showOnly(binding.recycleView)
            } else {
                showOnly(binding.tvZeroTrips)
            }

        }
    }

    private fun showOnly(viewToShow: View) {
        binding.recycleView.visibility =
            if (viewToShow == binding.recycleView) View.VISIBLE else View.GONE

        binding.tvZeroTrips.visibility =
            if (viewToShow == binding.tvZeroTrips) View.VISIBLE else View.GONE
    }
}


