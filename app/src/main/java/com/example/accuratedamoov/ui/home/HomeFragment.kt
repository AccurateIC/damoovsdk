package com.example.accuratedamoov.ui.home

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Geocoder
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.OptIn
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.accuratedamoov.R
import com.example.accuratedamoov.data.model.TripData
import com.example.accuratedamoov.database.DatabaseHelper
import com.example.accuratedamoov.databinding.FragmentHomeBinding
import com.example.accuratedamoov.ui.feed.FeedFragment
import com.example.accuratedamoov.ui.feed.FeedViewModel
import com.example.accuratedamoov.ui.notification.NotificationsActivity
import com.example.accuratedamoov.ui.tripDetails.TripDetailsActivity
import com.google.android.gms.location.LocationServices
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.badge.BadgeUtils
import com.google.android.material.badge.ExperimentalBadgeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.views.overlay.gridlines.LatLonGridlineOverlay.lineWidth
import java.text.SimpleDateFormat
import java.util.*


class HomeFragment : Fragment() {

    private lateinit var recentTrip: TripData
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val homeViewModel: HomeViewModel by viewModels()
    private val feedViewModel: FeedViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        // Initialize osmdroid configuration
        val ctx = requireContext().applicationContext
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx))
        Configuration.getInstance().userAgentValue = ctx.packageName

        return binding.root
    }

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val weekContainer = binding.weekContainer
        val weekLine = binding.underline

        // Get today index (0 = Monday, 6 = Sunday)
        val todayIndex = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 2
        val clampedIndex = if (todayIndex < 0) 6 else todayIndex

        weekContainer.post {
            // Calculate start = left of first day (Monday)
            val startX = weekContainer.getChildAt(0).left

            // Calculate end = center of today's day
            val todayView = weekContainer.getChildAt(clampedIndex)
            val endX = todayView.left + todayView.width / 2

            // Set line width dynamically
            val lineWidth = endX - startX
            weekLine.layoutParams.width = lineWidth
            weekLine.requestLayout()
        }

        val tvDate = binding.tvdate

        // Get current date
        val calendar = Calendar.getInstance()
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        fun getDaySuffix(day: Int): String {
            return if (day in 11..13) "th"
            else when (day % 10) {
                1 -> "st"
                2 -> "nd"
                3 -> "rd"
                else -> "th"
            }
        }

        // Format month
        val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())
        val month = monthFormat.format(calendar.time)

        // Set text
        tvDate.text = "$day${getDaySuffix(day)} $month"

        val weekTv = binding.weektv

        // Get current week number
        val weekNumber = calendar.get(Calendar.WEEK_OF_YEAR)
        weekTv.text = "Week $weekNumber"

        val currentDay = (calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7 + 1
        // Makes Monday=1, ..., Sunday=7 (since your layout starts with Monday)

        for (i in 0 until weekContainer.childCount) {
            val dayView = weekContainer.getChildAt(i) as TextView
            val dayIndex = i + 1

            when {
                dayIndex < currentDay -> { // past days
                    dayView.background.setTint(Color.WHITE)
                    dayView.setTextColor(Color.BLACK)
                }

                dayIndex == currentDay -> { // current day
                    dayView.background.setTint(Color.parseColor("#2196F3")) // blue
                    dayView.setTextColor(Color.WHITE)
                }

                else -> { // future days
                    dayView.background.setTint(Color.LTGRAY) // grey
                    dayView.setTextColor(Color.DKGRAY)
                }
            }
        }

        // ===== Location =====
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
        if (!checkLocationPermission()) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }

        lifecycleScope.launch(Dispatchers.IO) {
            // Trigger loading trips once (this updates shared prefs)
            feedViewModel.loadTripsIfNeeded()

            // Get trip info from shared preferences
            val sharedPref =
                requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            val tripCount = sharedPref.getInt("trip_count", 0)
            val totalDistance = sharedPref.getInt("total_distance", 0)

            // Update UI on main thread
            launch(Dispatchers.Main) {
                if (tripCount == 0) {
                    binding.firstTimell.visibility = View.VISIBLE
                    binding.weekcardll.visibility = View.GONE
                } else {
                    binding.firstTimell.visibility = View.GONE
                    binding.weekcardll.visibility = View.VISIBLE
                }

                feedViewModel.lastTrip.observe(viewLifecycleOwner) { trip ->
                    trip?.let {
                        updateTripUI(it)
                        recentTrip = it
                    }
                }


            }
        }

        binding.tripCardInclude.root.setOnClickListener {

            val intent = Intent(activity, TripDetailsActivity::class.java).apply {
                putExtra("ID", recentTrip.unique_id.toString())
                putExtra("START_TIME", recentTrip.start_date_ist)
                putExtra("END_TIME", recentTrip.end_date_ist)
                putExtra(
                    "START_LOC",
                    "${binding.tripCardInclude.sourceLocationMain.text}, ${binding.tripCardInclude.sourceLocationMain.text}"
                )
                putExtra(
                    "END_LOC",
                    "${binding.tripCardInclude.destLocationMain.text}, ${binding.tripCardInclude.destLocationSub.text}"
                )
            }
            activity?.startActivity(intent)

        }

        binding.tvAllTrips.setOnClickListener {
            findNavController().navigate(R.id.navigation_feed)
        }


        binding.tvAllstats.setOnClickListener {
            findNavController().navigate(R.id.navigation_dashboard)
        }

        binding.bellIcon.setOnClickListener {
            val intent = Intent(activity, NotificationsActivity::class.java)
            startActivity(intent)

        }


    }

    private fun checkLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 101
    }

    override fun onResume() {
        super.onResume()

        val weekContainer = binding.weekContainer
        val weekLine = binding.underline
        val weekIcon = binding.underlineIcon

        val todayIndex = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 2
        val clampedIndex = if (todayIndex < 0) 6 else todayIndex

        weekContainer.post {
            val mondayView = weekContainer.getChildAt(0)
            val startX = mondayView.left

            val todayView = weekContainer.getChildAt(clampedIndex)
            val endX = todayView.left + todayView.width / 2

            val lineWidth = endX - startX
            weekLine.layoutParams.width = lineWidth
            weekLine.requestLayout()

            weekLine.visibility = View.VISIBLE
            weekLine.pivotX = 0f
            weekLine.scaleX = 0f
            weekLine.animate()
                .scaleX(1f)
                .setDuration(1500)
                .setInterpolator(DecelerateInterpolator())
                .start()

            val startIconX = startX - weekIcon.width / 2f
            val endIconX = endX - weekIcon.width / 2f
            weekIcon.x = startIconX
            weekIcon.animate()
                .x(endIconX)
                .setDuration(1500)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }

        feedViewModel.refreshLastTrip()

        updateNotificationBadge()

    }

    private fun formatDate(dateTime: String): String {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val outputFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
        return try {
            val date = inputFormat.parse(dateTime)
            outputFormat.format(date!!)
        } catch (e: Exception) {
            ""
        }
    }

    private fun formatTime(dateTime: String): Pair<String, String> {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val outputFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return try {
            val date = inputFormat.parse(dateTime)
            val formatted = outputFormat.format(date!!)
            val parts = formatted.split(" ")
            Pair(parts[0], parts[1]) // e.g., ("10:30", "AM")
        } catch (e: Exception) {
            Pair("", "")
        }
    }

    private fun getLocationFromCoords(coords: String): Pair<String, String> {
        return try {
            val (lat, lng) = coords.split(",").map { it.trim().toDouble() }
            val geocoder = Geocoder(requireActivity().applicationContext, Locale.getDefault())
            val addresses = geocoder.getFromLocation(lat, lng, 1)
            if (!addresses.isNullOrEmpty()) {
                val city = addresses[0].locality ?: ""
                val state = addresses[0].adminArea ?: ""
                Pair(city, "$state, ${addresses[0].countryName}")
            } else Pair("", "")
        } catch (e: Exception) {
            Pair("", "")
        }
    }

    private fun updateTripUI(trip: TripData) {
        binding.tripCardInclude.tripDate.text = formatDate(trip.start_date_ist)
        val (startTime, startAmPm) = formatTime(trip.start_date_ist)
        binding.tripCardInclude.sourceTime.text = startTime
        binding.tripCardInclude.sourceAmPm.text = startAmPm

        val (endTime, endAmPm) = formatTime(trip.end_date_ist)
        binding.tripCardInclude.destTime.text = endTime
        binding.tripCardInclude.destAmPm.text = endAmPm

        val distanceText = "${trip.distance_km} km"
        binding.tripCardInclude.centerIcons.findViewById<TextView>(R.id.distanceText)?.text =
            distanceText

        val (startCity, startSub) = getLocationFromCoords(trip.start_coordinates)
        val (endCity, endSub) = getLocationFromCoords(trip.end_coordinates)
        binding.tripCardInclude.sourceLocationMain.text = startCity
        binding.tripCardInclude.sourceLocationSub.text = startSub
        binding.tripCardInclude.destLocationMain.text = endCity
        binding.tripCardInclude.destLocationSub.text = endSub
    }

    fun updateNotificationBadge() {
        val unread = DatabaseHelper.getInstance(requireContext())
            .getUnreadNotificationCount()

        // Show or hide dot
        binding.notificationDot.visibility =
            if (unread > 0) View.VISIBLE else View.GONE
    }



}
