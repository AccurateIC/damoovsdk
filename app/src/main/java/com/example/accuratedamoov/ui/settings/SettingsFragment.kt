package com.example.accuratedamoov.ui.settings


import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.EditText
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.work.*
import com.example.accuratedamoov.data.network.ApiService
import com.example.accuratedamoov.data.network.RetrofitClient
import com.example.accuratedamoov.databinding.FragmentSettingsBinding
import com.example.accuratedamoov.worker.TrackTableCheckWorker
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.TimeUnit

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var sharedPreferences: android.content.SharedPreferences
    private lateinit var viewModel: SettingsViewModel
    private lateinit var androidId: String
    private lateinit var deviceId: String

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        val root = binding.root

        sharedPreferences =
            requireContext().getSharedPreferences("appSettings", Context.MODE_PRIVATE)

        setupSpinner()
        loadSettings()

        binding.saveButton.setOnClickListener { saveSettings() }

        androidId =
            Settings.Secure.getString(requireContext().contentResolver, Settings.Secure.ANDROID_ID)
        deviceId = UUID.nameUUIDFromBytes(androidId.toByteArray()).toString()
        binding.textViewDeviceId.text = "Device ID: " + deviceId
        return root
    }

    private fun setupSpinner() {
        val intervals = listOf("15 min", "30 min", "1 hour", "2 hours", "6 hours")
        val adapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, intervals)
        binding.syncIntervalSpinner.adapter = adapter
    }

    private fun loadSettings() {
        val baseUrl = sharedPreferences.getString("api_url", "http://192.168.1.119:5000/") ?: ""
        val deviceName = sharedPreferences.getString("device_name", "") ?: ""

        binding.apiUrlEditText.setText(baseUrl)
        binding.editTextDeviceName.setText(deviceName)

        if (deviceName.isNotBlank()) {
            binding.editTextDeviceName.apply {
                isEnabled = false
                isFocusable = false
                isCursorVisible = false
            }
        }
    }

    private fun saveSettings() {
        val selectedPosition = binding.syncIntervalSpinner.selectedItemPosition
        val intervalMinutes = listOf(15, 30, 60, 120, 360)[selectedPosition]
        val apiUrl = binding.apiUrlEditText.text.toString().trim()
        val deviceNameInput = binding.editTextDeviceName.text.toString().trim()

        if (apiUrl.isEmpty()) {
            Snackbar.make(binding.root, "Please enter a valid API URL", Snackbar.LENGTH_SHORT).show()
            return
        }

        // Use the newly typed URL directly for health check
        val apiService = RetrofitClient.getApiService(apiUrl)

        lifecycleScope.launch {
            try {
                val response = apiService.checkHealth()
                if (response.isSuccessful) {
                    // Health check passed — save settings
                    with(sharedPreferences.edit()) {
                        putString("api_url", apiUrl)
                        putInt("sync_interval", intervalMinutes)

                        if (sharedPreferences.getString("device_name", "").isNullOrEmpty()
                            && deviceNameInput.isNotEmpty()
                        ) {
                            viewModel.initApi(apiUrl)
                            viewModel.registerDevice(deviceId, deviceNameInput)
                            putString("device_name", deviceNameInput)
                            binding.editTextDeviceName.apply {
                                isEnabled = false
                                isFocusable = false
                                isCursorVisible = false
                            }
                        }
                        apply()
                    }

                    scheduleWorker(intervalMinutes.toLong())
                    hideKeyboard(binding.apiUrlEditText)
                    Snackbar.make(binding.root, "Settings saved", Snackbar.LENGTH_LONG).show()
                } else {
                    Snackbar.make(binding.root, "Server error: ${response.code()}", Snackbar.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Snackbar.make(binding.root, "Unable to reach server: ${e.localizedMessage}", Snackbar.LENGTH_LONG).show()
            }
        }
    }



    private fun scheduleWorker(syncInterval: Long) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<TrackTableCheckWorker>()
            .setConstraints(constraints)
            .setInitialDelay(
                syncInterval,
                TimeUnit.MINUTES
            ) // fixed: use MINUTES instead of SECONDS
            .build()

        WorkManager.getInstance(requireContext()).enqueueUniqueWork(
            "TrackTableCheckWorker",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupKeyboardHiding(view: View) {
        view.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                val focusedView = requireActivity().currentFocus
                if (focusedView is EditText) {
                    focusedView.clearFocus()
                    hideKeyboard(focusedView)
                }
            }
            false
        }
    }

    private fun hideKeyboard(view: View) {
        val imm =
            requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupKeyboardHiding(view)
        viewModel = ViewModelProvider(this)[SettingsViewModel::class.java]
        viewModel.registrationResult.observe(viewLifecycleOwner) { message ->
            Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
