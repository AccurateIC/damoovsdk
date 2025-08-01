package com.example.accuratedamoov.ui.dashboard

import android.content.Context
import androidx.fragment.app.viewModels
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.accuratedamoov.databinding.FragmentDashboardBinding
import kotlinx.coroutines.launch
import java.util.UUID

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    // Correct usage of viewModel with AndroidViewModelFactory
    private val viewModel: DashboardViewModel by viewModels {
        ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sharedPref = requireActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val userId = sharedPref.getInt("user_id", 0)
        val androidId = Settings.Secure.getString(context?.contentResolver, Settings.Secure.ANDROID_ID)
        val deviceId = UUID.nameUUIDFromBytes(androidId.toByteArray()).toString()        // Observe user profile
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.userProfile.collect { profile ->
                profile?.let {
                    binding.nameText.text = "Name: ${it.data?.name}"
                    binding.mailText.text = "Email: ${it.data?.email}"
                }
            }
        }

        // Observe trip summary
        /*viewLifecycleOwner.lifecycleScope.launch {
            viewModel.tripSummary.collect { summary ->
                summary?.let {
                    binding.totalDistanceText.text = "Total Distance: ${it.data.total_distance_km} km"
                    binding.totalRidesText.text = "Total Rides: ${it.data.trip_count}"
                }
            }
        }*/


        val tripCount = sharedPref.getInt("trip_count", 0)
        val totalDistance = sharedPref.getInt("total_distance", 0)

        Log.d("SharedPref", "Trip Count: $tripCount, Total Distance: $totalDistance km")
        binding.totalDistanceText.text = "Total Distance: ${totalDistance} km"
        binding.totalRidesText.text = "Total Rides: ${tripCount}"

        viewModel.loadDashboardData(userId, deviceId)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

