package com.example.accuratedamoov.ui.feed

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.accuratedamoov.databinding.FragmentDashboardBinding
import com.example.accuratedamoov.ui.feed.adapter.TrackAdapter
import com.example.accuratedamoov.ui.tripDetails.TripDetailsActivity

import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch



class FeedFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private val feedViewModel: FeedViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recycleView.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
        }

        if (!feedViewModel.isSdkEnabled()) {
            requestLocationPermission()
            feedViewModel.enableSdk()
        }
        val dividerItemDecoration = DividerItemDecoration(context, LinearLayoutManager.VERTICAL)
        binding.recycleView.addItemDecoration(dividerItemDecoration)
        observeData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun observeData() {
        lifecycleScope.launch {
            feedViewModel.tracks.collectLatest { trackList ->
                if (!isAdded || _binding == null) return@collectLatest  // Check before accessing binding

                binding.recycleView.adapter = TrackAdapter(trackList) {
                    val intent = Intent(activity, TripDetailsActivity::class.java)
                    startActivity(intent)
                }
                binding.recycleView.visibility = if (trackList.isNotEmpty()) View.VISIBLE else View.GONE
                binding.tvZeroTrips.visibility = if (trackList.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }


    private fun requestLocationPermission() {
        context?.let {
            if (ActivityCompat.checkSelfPermission(it, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }
    }
}
