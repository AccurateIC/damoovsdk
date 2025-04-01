package com.example.accuratedamoov.ui.settings

import android.R
import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.accuratedamoov.databinding.FragmentSettingsBinding
import com.example.accuratedamoov.worker.TrackTableCheckWorker
import com.google.android.material.snackbar.Snackbar
import java.util.concurrent.TimeUnit

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val settingsViewModel = ViewModelProvider(this).get(SettingsViewModel::class.java)
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        setupSpinner()
        val sharedPreferences = context?.getSharedPreferences("appSettings", Context.MODE_PRIVATE)
        val baseUrl:String = sharedPreferences?.getString("api_url", "http://192.168.1.119:5000/") ?: "http://192.168.1.119:5000/"
        binding.apiUrlEditText.setText(baseUrl)
        binding.saveButton.setOnClickListener { saveSettings() }

        return root
    }

    private fun setupSpinner() {
        val intervals = listOf("15 min", "30 min", "1 hour", "2 hours", "6 hours")
        val intervalValues = listOf(15, 30, 60, 120, 360) // Corresponding minutes

        val adapter = ArrayAdapter(requireContext(), R.layout.simple_spinner_dropdown_item, intervals)
        binding.syncIntervalSpinner.adapter = adapter
    }

    private fun saveSettings() {
        val selectedPosition = binding.syncIntervalSpinner.selectedItemPosition
        val intervalMinutes = listOf(15, 30, 60, 120, 360)[selectedPosition]
        val apiUrl = binding.apiUrlEditText.text.toString().trim()

        if (apiUrl.isEmpty()) {
            Snackbar.make(binding.root, "Please enter a valid API URL", Toast.LENGTH_SHORT).show()
            return
        }

        // Store settings (SharedPreferences or Database)
        val sharedPreferences = requireContext().getSharedPreferences("appSettings", Context.MODE_PRIVATE)
        sharedPreferences.edit()
            .putString("api_url", apiUrl)
            .putInt("sync_interval", intervalMinutes)
            .apply()

        scheduleWorker(intervalMinutes.toLong())

        hideKeyboard(binding.apiUrlEditText)
        Snackbar.make(
            binding.root,
            "saved",
            Snackbar.LENGTH_LONG
        ).show()


    }

    private fun scheduleWorker(syncInterval: Long) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<TrackTableCheckWorker>()
            .setConstraints(constraints)
            .setInitialDelay(syncInterval, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(requireContext()).enqueueUniqueWork(
            "TrackTableCheckWorker",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )


    }




    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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
        val inputMethodManager = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupKeyboardHiding(view)
    }

}
