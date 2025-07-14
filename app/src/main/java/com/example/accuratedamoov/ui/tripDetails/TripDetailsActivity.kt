package com.example.accuratedamoov.ui.tripDetails

import android.content.Context
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
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
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
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

        val uniqueId = intent.getStringExtra("ID")
        mapView = binding.mapView
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)

        if (uniqueId != null && NetworkMonitorService.isConnected == true) {
            tripDetailsViewModel.fetchGeoPoints(uniqueId)
            Log.d("omkarid",uniqueId)
            tripDetailsViewModel.geoPoints.observe(this) { geoPoints ->
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

    private fun plotRouteOnMap(geoPoints: List<GeoPointModel>) {
        val geoPointList = geoPoints.map { GeoPoint(it.latitude, it.longitude) }

        // Center on the first point
        mapView.controller.setZoom(15.0)
        mapView.controller.setCenter(geoPointList.first())

        // Draw polyline
        val line = Polyline().apply {
            setPoints(geoPointList)
            outlinePaint.color = Color.BLUE
            outlinePaint.strokeWidth = 5f
        }
        mapView.overlays.add(line)

        // Add start marker
        addCustomMarker(geoPointList.first(), R.drawable.ic_start_location, "started")

        // Add end marker
        addCustomMarker(geoPointList.last(), R.drawable.ic_end_location, "ended")

        mapView.invalidate()
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

}