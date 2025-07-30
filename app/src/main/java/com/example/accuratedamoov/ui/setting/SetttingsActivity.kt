package com.example.accuratedamoov.ui.setting

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.accuratedamoov.MainActivity
import com.example.accuratedamoov.R
import com.example.accuratedamoov.data.network.RetrofitClient
import com.example.accuratedamoov.databinding.ActivitySetttingsBinding
import com.example.accuratedamoov.ui.login.LoginActivity
import com.example.accuratedamoov.ui.register.RegisterActivity
import com.example.accuratedamoov.ui.settings.SettingsViewModel
import com.example.accuratedamoov.worker.TrackTableCheckWorker
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.TimeUnit

class SetttingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySetttingsBinding
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var viewModel: SettingsViewModel
    private lateinit var androidId: String
    private lateinit var deviceId: String

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySetttingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPreferences = getSharedPreferences("appSettings", Context.MODE_PRIVATE)

        setupSpinner()
        loadSettings()

        binding.saveButton.setOnClickListener { saveSettings() }

        androidId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        deviceId = UUID.nameUUIDFromBytes(androidId.toByteArray()).toString()

        setupKeyboardHiding(binding.root)

        viewModel = ViewModelProvider(this)[SettingsViewModel::class.java]
    }

    private fun setupSpinner() {
        val intervals = listOf("15 min", "30 min", "1 hour", "2 hours", "6 hours")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, intervals)
        binding.syncIntervalSpinner.adapter = adapter
    }

    private fun loadSettings() {
        val baseUrl = sharedPreferences.getString("api_url", "http://192.168.1.119:5000/") ?: ""
        binding.apiUrlEditText.setText(baseUrl)
    }

    private fun saveSettings() {
        val selectedPosition = binding.syncIntervalSpinner.selectedItemPosition
        val intervalMinutes = listOf(15, 30, 60, 120, 360)[selectedPosition]
        val apiUrl = binding.apiUrlEditText.text.toString().trim()

        if (apiUrl.isEmpty()) {
            Snackbar.make(binding.root, "Please enter a valid API URL", Snackbar.LENGTH_SHORT).show()
            return
        }

        val apiService = RetrofitClient.getApiService(apiUrl)

        lifecycleScope.launch {
            try {
                val response = apiService.checkHealth()
                if (response.isSuccessful) {
                    with(sharedPreferences.edit()) {
                        putString("api_url", apiUrl)
                        putInt("sync_interval", intervalMinutes)
                        apply()
                    }

                    scheduleWorker(intervalMinutes.toLong())
                    hideKeyboard(binding.apiUrlEditText)

                    // Navigate based on registration & login status
                    val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
                    val isLoggedIn = prefs.getBoolean("is_logged_in", false)

                    val nextIntent = when {
                        !isLoggedIn -> Intent(
                            this@SetttingsActivity,
                            LoginActivity::class.java
                        )
                        else -> Intent(this@SetttingsActivity, MainActivity::class.java)
                    }

                    startActivity(nextIntent)
                    finish()

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
            .setInitialDelay(syncInterval, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(this).enqueueUniqueWork(
            "TrackTableCheckWorker",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    private fun setupKeyboardHiding(view: View) {
        view.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                val focusedView = currentFocus
                if (focusedView is EditText) {
                    focusedView.clearFocus()
                    hideKeyboard(focusedView)
                }
            }
            false
        }
    }

    private fun hideKeyboard(view: View) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }
}
