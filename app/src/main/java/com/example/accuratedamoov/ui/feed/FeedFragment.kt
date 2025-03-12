package com.example.accuratedamoov.ui.feed

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.accuratedamoov.databinding.FragmentDashboardBinding
import com.example.accuratedamoov.model.TrackModel
import com.example.accuratedamoov.ui.feed.adapter.TrackAdapter
import com.raxeltelematics.v2.sdk.TrackingApi
import com.raxeltelematics.v2.sdk.server.model.Locale
import com.raxeltelematics.v2.sdk.server.model.sdk.Track
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext



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
                    // TODO: show Trip details with/without map
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
