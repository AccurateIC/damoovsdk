package com.example.accuratedamoov.ui.home

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.accuratedamoov.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {
    val TAG:String = this::class.java.simpleName

    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    
    private val homeViewModel: HomeViewModel by viewModels {
        ViewModelProvider.AndroidViewModelFactory(requireActivity().application)
    }
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

        homeViewModel.errMsg.observe(viewLifecycleOwner, Observer {errMsg ->

            if(errMsg.isNotEmpty()){
                Toast.makeText(requireContext(),errMsg,Toast.LENGTH_LONG).show()

            }

        })

        binding.startTripManually.setOnClickListener {
            homeViewModel.startTracking()
        }

        binding.stopTripManually.setOnClickListener {
            homeViewModel.stopTracking()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}