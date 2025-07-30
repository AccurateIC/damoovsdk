package com.example.accuratedamoov.ui.tripDetails

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.AttributeSet
import android.util.Log
import android.view.View
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

class TripDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTripDetailsBinding
    private lateinit var mapView: MapView
    private val tripDetailsViewModel: TripDetailsViewModel by viewModels()

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
    }

    private fun showLoader(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun plotRouteOnMap(geoPoints: List<GeoPointModel>) {
        mapView.overlays.clear()

        lifecycleScope.launch(Dispatchers.Default) {
            val geoPointList = filterGeoPointsHalf(geoPoints)
            Log.d("TripDetailsActivity", "Filtered_GeoPoints: ${geoPointList.toString()}")
            if (geoPointList.size < 2) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@TripDetailsActivity, "Not enough points to draw route", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }

            val line = Polyline().apply {
                setPoints(geoPointList)
                outlinePaint.color = Color.BLUE
                outlinePaint.strokeWidth = 5f
                outlinePaint.isAntiAlias = true
                outlinePaint.style = Paint.Style.STROKE
            }

            withContext(Dispatchers.Main) {
                mapView.overlays.add(line)
                addCustomMarker(geoPointList.first(), R.drawable.ic_start_location, "started")
                addCustomMarker(geoPointList.last(), R.drawable.ic_end_location, "ended")
                mapView.zoomToBoundingBox(BoundingBox.fromGeoPointsSafe(geoPointList), true)
                mapView.invalidate()
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
}
