package com.example.accuratedamoov.ui.tripDetails

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.accuratedamoov.R

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.example.accuratedamoov.databinding.ActivityTripDetailsBinding
import com.example.accuratedamoov.model.GeoPointModel
import com.example.accuratedamoov.service.NetworkMonitorService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class TripDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTripDetailsBinding
    private lateinit var mapView: MapView
    private val tripDetailsViewModel: TripDetailsViewModel by viewModels()
    private var dX = 0f
    private var dY = 0f
    private var isExpanded = false
    private var expandedHeight = 0
    private val collapsedHeight = 80

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Configuration.getInstance().load(
            applicationContext,
            PreferenceManager.getDefaultSharedPreferences(applicationContext)
        )

        binding = ActivityTripDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mapView = binding.mapView
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)

        val uniqueId = intent.getStringExtra("ID")


        if (uniqueId != null && NetworkMonitorService.isConnected == true) {
            showLoader(true)
            tripDetailsViewModel.fetchGeoPoints(uniqueId)

            tripDetailsViewModel.geoPoints.observe(this) { geoPoints ->
                showLoader(false)
                if (geoPoints.isNotEmpty()) {
                    plotRouteOnMap(geoPoints)
                } else {
                    Toast.makeText(this, "No geo points found", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        } else {
            Toast.makeText(this, "Something went wrong", Toast.LENGTH_SHORT).show()
            finish()
        }



        /*binding.tripInfoCard.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    dX = v.x - event.rawX
                    dY = v.y - event.rawY
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    v.animate()
                        .x(event.rawX + dX)
                        .y(event.rawY + dY)
                        .setDuration(0)
                        .start()
                    true
                }

                else -> false
            }
        }*/
        //setupTripCardToggle()
        setupFloatingButton()
        setupFloatingButtonClick()
    }

    private fun setupFloatingButton() {
        var dX = 0f
        var dY = 0f
        var startX = 0f
        var startY = 0f
        var isDragging = false
        val dragThreshold = 10

        binding.btnShowTripInfo.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    dX = view.x - event.rawX
                    dY = view.y - event.rawY
                    startX = event.rawX
                    startY = event.rawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = Math.abs(event.rawX - startX)
                    val deltaY = Math.abs(event.rawY - startY)
                    if (deltaX > dragThreshold || deltaY > dragThreshold) {
                        isDragging = true
                        view.animate()
                            .x(event.rawX + dX)
                            .y(event.rawY + dY)
                            .setDuration(0)
                            .start()
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isDragging) {
                        view.performClick()
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun setupFloatingButtonClick() {
        binding.btnShowTripInfo.setOnClickListener {
            showTripDetailsBottomSheet()
        }
    }

    private fun showTripDetailsBottomSheet() {
        val bottomSheetDialog = com.google.android.material.bottomsheet.BottomSheetDialog(this, R.style.TripBottomSheetStyle)
        val view = layoutInflater.inflate(R.layout.trip_details_bottom_sheet, null)

        val startTime = intent.getStringExtra("START_TIME") ?: "Unknown"
        val endTime = intent.getStringExtra("END_TIME") ?: "Unknown"
        val startLoc = intent.getStringExtra("START_LOC") ?: "Unknown"
        val endLoc = intent.getStringExtra("END_LOC") ?: "Unknown"

        view.apply {
            findViewById<TextView>(R.id.startLocation).text = "📍 Start Location: $startLoc"
            findViewById<TextView>(R.id.startTime).text = "⏱ Start Time: $startTime"
            findViewById<TextView>(R.id.endLocation).text = "🏁 End Location: $endLoc"
            findViewById<TextView>(R.id.endTime).text = "⏱ End Time: $endTime"

            // Optional close button if you add one in layout
            findViewById<ImageView?>(R.id.closeButton)?.setOnClickListener {
                bottomSheetDialog.dismiss()
            }
        }

        bottomSheetDialog.setContentView(view)

        // expand fully by default
        bottomSheetDialog.behavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED

        bottomSheetDialog.show()
    }


    private fun showLoader(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun plotRouteOnMap(geoPoints: List<GeoPointModel>) {
        mapView.overlays.clear()

        lifecycleScope.launch(Dispatchers.Default) {
            val geoPointList = filterGeoPointsHalf(geoPoints)

            if (geoPointList.size < 2) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@TripDetailsActivity,
                        "Not enough points to draw route",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                return@launch
            }

            val animatedPolyline = Polyline().apply {
                outlinePaint.color = Color.BLUE
                outlinePaint.strokeWidth = 6f
                outlinePaint.isAntiAlias = true
                outlinePaint.style = Paint.Style.STROKE
            }

            val vehicleMarker = Marker(mapView).apply {
                icon = ContextCompat.getDrawable(this@TripDetailsActivity, R.drawable.ic_point)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                setInfoWindow(null)
            }

            withContext(Dispatchers.Main) {
                mapView.overlays.add(animatedPolyline)
                mapView.overlays.add(vehicleMarker)
                addCustomMarker(geoPointList.first(), R.drawable.ic_start_location, "started")
                addCustomMarker(geoPointList.last(), R.drawable.ic_end_location, "ended")
            }

            withContext(Dispatchers.Main) {
                val boundingBox = BoundingBox.fromGeoPointsSafe(geoPointList)
                mapView.zoomToBoundingBox(boundingBox, true)

                // Store listener instance so we can remove it
                val layoutListener = object : ViewTreeObserver.OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        mapView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                        lifecycleScope.launch(Dispatchers.Main) {
                            delay(1000) // allow zoom animation to settle
                            mapView.controller.animateTo(boundingBox.centerWithDateLine)

                            startRouteAnimation(animatedPolyline, vehicleMarker, geoPointList)
                        }
                    }
                }
                mapView.viewTreeObserver.addOnGlobalLayoutListener(layoutListener)
            }
        }
    }

    private suspend fun startRouteAnimation(
        animatedPolyline: Polyline,
        vehicleMarker: Marker,
        geoPointList: List<GeoPoint>
    ) {
        val totalDurationMs = 3000L
        val segmentCount = geoPointList.size - 1
        val maxTotalSteps = 300
        val stepsPerSegment = (maxTotalSteps / segmentCount).coerceIn(1, 10)
        val totalSteps = segmentCount * stepsPerSegment
        val delayPerStep = (totalDurationMs / totalSteps).coerceAtLeast(1L)

        for (i in 1 until geoPointList.size) {
            val from = geoPointList[i - 1]
            val to = geoPointList[i]

            for (j in 1..stepsPerSegment) {
                val lat = from.latitude + (to.latitude - from.latitude) * j / stepsPerSegment
                val lon = from.longitude + (to.longitude - from.longitude) * j / stepsPerSegment
                val intermediatePoint = GeoPoint(lat, lon)
                val direction = calculateBearing(from, to)

                withContext(Dispatchers.Main) {
                    animatedPolyline.addPoint(intermediatePoint)
                    vehicleMarker.position = intermediatePoint
                    vehicleMarker.rotation = direction
                    mapView.invalidate()
                }

                delay(delayPerStep)
            }
        }

        withContext(Dispatchers.Main) {
            val startTime = intent.getStringExtra("START_TIME")
            val endTime = intent.getStringExtra("END_TIME")
            val startLoc = intent.getStringExtra("START_LOC")
            val endLoc = intent.getStringExtra("END_LOC")

            binding.btnShowTripInfo.apply {
                visibility = View.INVISIBLE
                isClickable = false
                post {
                    alpha = 0f
                    visibility = View.VISIBLE
                    animate()
                        .alpha(1f)
                        .setDuration(400)
                        .withEndAction {
                            isClickable = true
                        }
                        .start()
                }
            }

        }

    }


    private fun addCustomMarker(position: GeoPoint, drawableResId: Int, title: String) {
        val marker = Marker(mapView).apply {
            this.position = position
            this.title = title
            this.icon = ContextCompat.getDrawable(this@TripDetailsActivity, drawableResId)
            this.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        }
        mapView.overlays.add(marker)
    }

    private fun filterGeoPointsHalf(rawPoints: List<GeoPointModel>): List<GeoPoint> {
        val filtered = mutableListOf<GeoPoint>()
        var lastLat = 0.0
        var lastLon = 0.0

        for (i in rawPoints.indices step 2) { // skip every second point
            val point = rawPoints[i]
            val lat = point.latitude
            val lon = point.longitude

            if (filtered.isEmpty() || distanceBetween(lat, lon, lastLat, lastLon) > 3) {
                filtered.add(GeoPoint(lat, lon))
                lastLat = lat
                lastLon = lon
            }
        }

        return filtered
    }

    private fun distanceBetween(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0]
    }


    private fun calculateBearing(start: GeoPoint, end: GeoPoint): Float {
        val lat1 = Math.toRadians(start.latitude)
        val lon1 = Math.toRadians(start.longitude)
        val lat2 = Math.toRadians(end.latitude)
        val lon2 = Math.toRadians(end.longitude)

        val dLon = lon2 - lon1
        val y = sin(dLon) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)

        return (Math.toDegrees(atan2(y, x)).toFloat() + 360) % 360
    }
}
