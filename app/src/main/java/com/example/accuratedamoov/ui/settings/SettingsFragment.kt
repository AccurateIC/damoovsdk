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
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.work.*
import com.example.accuratedamoov.MainActivity
import com.example.accuratedamoov.data.network.ApiService
import com.example.accuratedamoov.data.network.RetrofitClient
import com.example.accuratedamoov.databinding.FragmentSettingsBinding
import com.example.accuratedamoov.worker.TrackTableCheckWorker
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.TimeUnit
import com.example.accuratedamoov.R;
import com.google.android.material.bottomnavigation.BottomNavigationView

class SettingsFragment : Fragment() {

    private val viewModel: SettingsViewModel by viewModels()

    private lateinit var apiUrlEditText: TextInputEditText
    private lateinit var scoreUrlEditText: TextInputEditText
    private lateinit var saveButton: View

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root = inflater.inflate(R.layout.fragment_settings, container, false)

        apiUrlEditText = root.findViewById(R.id.apiUrlEditText)
        scoreUrlEditText = root.findViewById(R.id.scoreUrlEditText)
        saveButton = root.findViewById(R.id.saveButton)

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Populate initial values
        apiUrlEditText.setText(viewModel.cloudUrl.value ?: "")
        scoreUrlEditText.setText(viewModel.scoreUrl.value ?: "")

        viewModel.cloudUrl.observe(viewLifecycleOwner) { url ->
            apiUrlEditText.setText(url)
        }
        viewModel.scoreUrl.observe(viewLifecycleOwner) { url ->
            scoreUrlEditText.setText(url)
        }

        // Show messages
        viewModel.message.observe(viewLifecycleOwner, Observer { msg ->
            if (!msg.isNullOrBlank()) {
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
            }

            if (msg.contains("saved", ignoreCase = true)) { // success condition
                (requireActivity().findViewById<BottomNavigationView>(R.id.nav_view))
                    .selectedItemId = R.id.navigation_home
            }
        })

        // Handle save button click
        saveButton.setOnClickListener {
            val apiUrl = apiUrlEditText.text?.toString()?.trim() ?: ""
            val scoreUrl = scoreUrlEditText.text?.toString()?.trim() ?: ""

            val mainActivity = activity as? MainActivity
            if (mainActivity != null && mainActivity.isNetworkAvailable()) {
                viewModel.saveCloudUrl(apiUrl)

            }
            viewModel.saveScoreUrl(scoreUrl)
        }
    }
}
