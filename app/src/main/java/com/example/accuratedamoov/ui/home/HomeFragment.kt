package com.example.accuratedamoov.ui.home

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.example.accuratedamoov.R
import com.example.accuratedamoov.databinding.FragmentHomeBinding
import com.google.android.gms.maps.MapView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.snackbar.Snackbar
import com.raxeltelematics.v2.sdk.TrackingApi

class HomeFragment : Fragment() {
    val TAG: String = this::class.java.simpleName

    private var _binding: FragmentHomeBinding? = null


    private val binding get() = _binding!!
    private val trackinApi = TrackingApi.getInstance()
    private val homeViewModel: HomeViewModel by viewModels()
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {


        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        return root

    }

    @SuppressLint("HardwareIds")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val mapView = view.findViewById<MapView>(R.id.mapView)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync { googleMap ->
            // Customize map (e.g., move camera, add markers)
        }

        if(trackinApi.isTracking()){
            binding.stopTripManually.visibility = View.VISIBLE
            binding.startTripManually.visibility = View.GONE

            Snackbar.make(
                binding.root, "Tracking is in progress", Snackbar.LENGTH_LONG
            ).show()

        }else{
            binding.stopTripManually.visibility = View.GONE
            binding.startTripManually.visibility = View.VISIBLE
        }
        homeViewModel.errMsg.observe(viewLifecycleOwner, Observer { errMsg ->

            if (errMsg.isNotEmpty()) {
                Snackbar.make(binding.root, errMsg, Toast.LENGTH_LONG).show()

            }

        })
        if (!trackinApi.isTracking()) {
            binding.stopTripManually.isClickable = false
        }


        binding.startTripManually.setOnClickListener {
            if (trackinApi.isTracking()) {
                Snackbar.make(
                    binding.root, "Tracking is in progress", Snackbar.LENGTH_LONG
                ).show()
            } else {
                homeViewModel.startTracking()
                binding.stopTripManually.visibility = View.VISIBLE
                binding.startTripManually.visibility = View.GONE
            }
        }

        binding.stopTripManually.setOnClickListener {
            Snackbar.make(
                binding.root, "Tracking stopped", Snackbar.LENGTH_LONG
            ).show()
            homeViewModel.stopTracking()
            binding.stopTripManually.visibility = View.GONE
            binding.startTripManually.visibility = View.VISIBLE
        }

        val bottomSheet = view.findViewById<NestedScrollView>(R.id.bottom_sheet)
        val handle = view.findViewById<View>(R.id.bottom_sheet_handle)



        val behavior = BottomSheetBehavior.from(bottomSheet)

        behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                // Animate views as bottom sheet slides
            }

            override fun onStateChanged(bottomSheet: View, newState: Int) {
                // Detect collapsed/expanded/hidden states
            }
        })


        handle.setOnClickListener {
            val behavior = BottomSheetBehavior.from(bottomSheet)
            behavior.state = if (behavior.state == BottomSheetBehavior.STATE_COLLAPSED)
                BottomSheetBehavior.STATE_EXPANDED else BottomSheetBehavior.STATE_COLLAPSED
        }


    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}