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
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.accuratedamoov.databinding.FragmentSettingsBinding
import com.example.accuratedamoov.worker.TrackTableCheckWorker
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
            Toast.makeText(requireContext(), "Please enter a valid API URL", Toast.LENGTH_SHORT).show()
            return
        }

        // Store settings (SharedPreferences or Database)
        val sharedPreferences = requireContext().getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        sharedPreferences.edit()
            .putString("api_url", apiUrl)
            .putInt("sync_interval", intervalMinutes)
            .apply()

        scheduleWorker(intervalMinutes)
    }

    private fun scheduleWorker(intervalMinutes: Int) {
        val workRequest = PeriodicWorkRequestBuilder<TrackTableCheckWorker>(
            intervalMinutes.toLong(), TimeUnit.MINUTES
        ).build()

        WorkManager.getInstance(requireContext()).enqueueUniquePeriodicWork(
            "TrackTableCheckWorker",
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )

        Toast.makeText(requireContext(), "Worker set to run every $intervalMinutes minutes", Toast.LENGTH_SHORT).show()
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
