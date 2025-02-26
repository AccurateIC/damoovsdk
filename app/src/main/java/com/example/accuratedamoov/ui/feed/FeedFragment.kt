package com.example.accuratedamoov.ui.feed

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.Visibility
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

class FeedFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    val TAG: String = this::class.java.simpleName
    private val trackingApi = TrackingApi.getInstance()
    private val binding get() = _binding!!
    private lateinit var viewManager: RecyclerView.LayoutManager
    private val disposables = CompositeDisposable()
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val feedViewModel =
            ViewModelProvider(this).get(FeedViewModel::class.java)
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        val root: View = binding.root
        return root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewManager = LinearLayoutManager(context)

        _binding?.recycleView?.apply {
            setHasFixedSize(true)
            layoutManager = viewManager
        }
        if (!trackingApi.isSdkEnabled()) {
            if (context?.let {
                    ActivityCompat.checkSelfPermission(
                        it,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    )
                } != PackageManager.PERMISSION_GRANTED
            ) {

                return
            }
            trackingApi.setEnableSdk(true)
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


    private fun loadData() {
        val disposable = Single.fromCallable {
            TrackingApi.getInstance().getTracks(locale = Locale.EN, offset = 0, limit = 10)
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnError { it.printStackTrace() }
            .subscribe(
                { data -> updateData(data) },
                { error -> error.printStackTrace() } // Handle errors properly
            )

        disposables.add(disposable)
    }

    fun updateData(result: Array<Track>?) {
        if (result != null && result.size > 0) {
            Log.d(TAG, result.size.toString())
            val viewModels = result.map {
                TrackModel(
                    addressStart = it.addressStart,
                    addressEnd = it.addressEnd,
                    endDate = it.endDate,
                    startDate = it.startDate,
                    trackId = it.trackId,
                    accelerationCount = it.accelerationCount,
                    decelerationCount = it.decelerationCount,
                    distance = it.distance,
                    duration = it.duration,
                    rating = it.rating,
                    phoneUsage = it.phoneUsage,
                    originalCode = it.originalCode,
                    hasOriginChanged = it.hasOriginChanged,
                    midOverSpeedMileage = it.midOverSpeedMileage,
                    highOverSpeedMileage = it.highOverSpeedMileage,
                    drivingTips = it.drivingTips,
                    shareType = it.shareType,
                    cityStart = it.cityStart,
                    cityFinish = it.cityFinish
                )
            }
            val viewAdapter = TrackAdapter(viewModels) {
                //showTrackDetails(it)
            }
            _binding?.recycleView?.adapter = viewAdapter
        } else {
            Log.d(TAG, "no results")
            _binding?.recycleView?.visibility = View.GONE
            _binding?.tvZeroTrips?.visibility = View.VISIBLE
        }
    }
}