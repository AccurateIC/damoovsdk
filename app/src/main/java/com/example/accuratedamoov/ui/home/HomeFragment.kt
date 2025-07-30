package com.example.accuratedamoov.ui.home

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.example.accuratedamoov.R
import com.example.accuratedamoov.databinding.FragmentHomeBinding
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.MapView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.snackbar.Snackbar
import com.raxeltelematics.v2.sdk.TrackingApi
import org.mapsforge.core.model.LatLong
import org.mapsforge.map.android.graphics.AndroidGraphicFactory
import org.mapsforge.map.android.util.AndroidUtil
import org.mapsforge.map.layer.renderer.TileRendererLayer
import org.mapsforge.map.reader.MapFile
import org.osmdroid.api.IGeoPoint
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapController
import org.osmdroid.views.overlay.Marker
import java.io.FileInputStream

/*
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
       */
/* AndroidGraphicFactory.createInstance(context)
        val contract =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                result.data?.data?.let { uri ->
                    openMap(uri)
                }

            }*//*


        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        return root

    }

   */
/* private fun openMap(uri: Uri) {
        _binding!!.mapView.mapScaleBar.isVisible = true
        _binding!!.mapView.setBuiltInZoomControls(true)
        val cache = AndroidUtil.createTileCache(
            context,
            "mycache",
            _binding!!.mapView.model.displayModel.tileSize,
            1f,
            _binding!!.mapView.model.frameBufferModel.overdrawFactor
        )
        val stream: FileInputStream =
            context?.contentResolver?.openOutputStream(uri) as FileInputStream
        val mapStore = MapFile(stream)
        val renderLayer = TileRendererLayer(
            cache,
            mapStore,
            _binding!!.mapView.model.mapViewPosition,
            AndroidGraphicFactory.INSTANCE
        )
        _binding!!.mapView.layerManager.layers.add(renderLayer)
        _binding!!.mapView.setCenter(LatLong(18.5889, 73.7031))
    }*//*


    @SuppressLint("HardwareIds")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding!!.mapView.setTileSource(TileSourceFactory.MAPNIK)
        _binding!!.mapView.setBuiltInZoomControls(true)
        _binding!!.mapView.setMultiTouchControls(true)
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())

        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Snackbar.make(
                binding.root, "Please grant all required permissions ", Snackbar.LENGTH_LONG
            ).show()
            return
        }
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val currentPoint = GeoPoint(location.latitude, location.longitude)
                val mapController = _binding!!.mapView.controller as MapController
                mapController.setZoom(15.0)
                mapController.setCenter(currentPoint)

                val marker = Marker(_binding!!.mapView).apply {
                    position = currentPoint
                    icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_start_location)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    title = "me"
                }
                _binding!!.mapView.overlays.add(marker)

                _binding!!.mapView.invalidate()
            } else {
                Toast.makeText(context, "Unable to get current location", Toast.LENGTH_SHORT).show()
            }
        }


        homeViewModel.errMsg.observe(viewLifecycleOwner, Observer { errMsg ->

            if (errMsg.isNotEmpty()) {
                Snackbar.make(binding.root, errMsg, Toast.LENGTH_LONG).show()

            }

        })
        if (!trackinApi.isTracking()) {
            binding.stopTripManually.isClickable = false
        }

        if(trackinApi.isTracking()) {
            binding.startTripManually.visibility = View.GONE
        }else{
            binding.startTripManually.visibility = View.VISIBLE

        }
        binding.startTripManually.setOnClickListener {
            if (trackinApi.isTracking()) {
                Snackbar.make(
                    binding.root, "Tracking is in progress", Snackbar.LENGTH_LONG
                ).show()

            } else {
                homeViewModel.startTracking()

            }
            binding.stopTripManually.visibility = View.VISIBLE
            binding.startTripManually.visibility = View.GONE
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

    override fun onResume() {

        super.onResume()

        if (trackinApi.isTracking()) {
            binding.stopTripManually.visibility = View.VISIBLE
            binding.startTripManually.visibility = View.GONE

            Snackbar.make(
                binding.root, "Tracking is in progress", Snackbar.LENGTH_LONG
            ).show()

        } else {
            binding.stopTripManually.visibility = View.GONE
            binding.startTripManually.visibility = View.VISIBLE
        }

    }
}*/



class HomeFragment : Fragment() {
    private val TAG: String = this::class.java.simpleName
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val trackingApi = TrackingApi.getInstance()
    private val homeViewModel: HomeViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        // Initialize osmdroid Configuration
        val ctx = requireContext().applicationContext
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx))
        Configuration.getInstance().userAgentValue = ctx.packageName

        return binding.root
    }

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize MapView
        binding.mapView.setTileSource(TileSourceFactory.MAPNIK)
        binding.mapView.setBuiltInZoomControls(true)
        binding.mapView.setMultiTouchControls(true)

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())

        if (!checkLocationPermission()) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val currentPoint = GeoPoint(location.latitude, location.longitude)
                val mapController = binding.mapView.controller as MapController
                mapController.setZoom(15.0)
                mapController.setCenter(currentPoint)

                val marker = Marker(binding.mapView).apply {
                    position = currentPoint
                    icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_start_location)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    title = "You are here"
                }
                binding.mapView.overlays.add(marker)
                binding.mapView.invalidate()
            } else {
                Toast.makeText(context, "Unable to get current location", Toast.LENGTH_SHORT).show()
            }
        }

        homeViewModel.errMsg.observe(viewLifecycleOwner, Observer { errMsg ->
            if (errMsg.isNotEmpty()) {
                Snackbar.make(binding.root, errMsg, Snackbar.LENGTH_LONG).show()
            }
        })

        updateTrackingUI()

        binding.startTripManually.setOnClickListener {
            if (!trackingApi.isTracking()) {
                homeViewModel.startTracking()
                Snackbar.make(binding.root, "Tracking started", Snackbar.LENGTH_SHORT).show()
            } else {
                Snackbar.make(binding.root, "Tracking is already in progress", Snackbar.LENGTH_SHORT).show()
            }
            updateTrackingUI()
        }

        binding.stopTripManually.setOnClickListener {
            homeViewModel.stopTracking()
            Snackbar.make(binding.root, "Tracking stopped", Snackbar.LENGTH_SHORT).show()
            updateTrackingUI()
        }

        setupBottomSheet(view)
    }

    private fun updateTrackingUI() {
        val isTracking = try {
            trackingApi.isTracking()
        } catch (e: UninitializedPropertyAccessException) {
            false
        } catch (e: Exception) {
            false
        }
        binding.startTripManually.visibility = if (isTracking) View.GONE else View.VISIBLE
        binding.stopTripManually.visibility = if (isTracking) View.VISIBLE else View.GONE
    }

    private fun setupBottomSheet(view: View) {
        val bottomSheet = view.findViewById<NestedScrollView>(R.id.bottom_sheet)
        val handle = view.findViewById<View>(R.id.bottom_sheet_handle)

        val behavior = BottomSheetBehavior.from(bottomSheet)
        behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onSlide(bottomSheet: View, slideOffset: Float) {}
            override fun onStateChanged(bottomSheet: View, newState: Int) {}
        })

        handle.setOnClickListener {
            behavior.state = if (behavior.state == BottomSheetBehavior.STATE_COLLAPSED)
                BottomSheetBehavior.STATE_EXPANDED else BottomSheetBehavior.STATE_COLLAPSED
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
        _binding?.mapView?.onDetach()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        updateTrackingUI()
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 101
    }
}
