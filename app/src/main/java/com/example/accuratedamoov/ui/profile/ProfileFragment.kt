package com.example.accuratedamoov.ui.profile

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import com.example.accuratedamoov.R
import java.io.File
import androidx.core.net.toUri
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.accuratedamoov.SplashScreenActivity
import com.example.accuratedamoov.ui.setting.SetttingsActivity
import com.telematicssdk.tracking.TrackingApi
import androidx.core.content.edit

class ProfileFragment : Fragment() {

    private lateinit var tvTrips: TextView
    private val viewModel: ProfileViewModel by viewModels()
    private lateinit var profileImage: ImageView

    private val imagePicker =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                viewModel.saveProfileImage(requireContext(), uri)
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        profileImage = view.findViewById(R.id.imgProfile)

        val editBtn = view.findViewById<ImageView>(R.id.imgEdit)
        editBtn.setOnClickListener {
            imagePicker.launch("image/*")
        }

        val dialog = view.findViewById<LinearLayout>(R.id.rowLogout)
        dialog.setOnClickListener {
            showLogoutDialog()
        }
        observeViewModel()

        val rowAppSettings = view.findViewById<LinearLayout>(R.id.rowAppSettings)

        rowAppSettings.setOnClickListener {
            val intent = Intent(requireContext(), SetttingsActivity::class.java)
            startActivity(intent)
        }
        val tvName = view.findViewById<TextView>(R.id.tvName)
        val tvEmail = view.findViewById<TextView>(R.id.tvEmail)
        tvTrips = view.findViewById<TextView>(R.id.tvTrips)
        setUserInfo(tvName, tvEmail)

        return view
    }

    private fun setUserInfo(tvName: TextView, tvEmail: TextView) {

        val prefs = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

        val tripCount = prefs.getInt("trip_count", 0)
        val name = prefs.getString("name", null)
        val email = prefs.getString("email", null)
        val phone = prefs.getString("phone", null)

        // Name Priority: name → phone → "User"
        val finalName = when {
            !name.isNullOrEmpty() -> name
            !phone.isNullOrEmpty() -> phone
            else -> "User"
        }

        // Email Priority: email → phone → "Not available"
        val finalEmail = when {
            !email.isNullOrEmpty() -> email
            !phone.isNullOrEmpty() -> phone
            else -> "Not available"
        }

        tvName.text = finalName
        tvEmail.text = finalEmail
        tvTrips.text = tripCount.toString()
    }


    private fun observeViewModel() {
        viewModel.profileImagePath.observe(viewLifecycleOwner) { path ->
            if (path != null) {

                val file = File(path)

                // Force reload by appending timestamp query
                val uri = (file.toURI().toString() + "?t=" + System.currentTimeMillis()).toUri()

                profileImage.setImageURI(uri)
                profileImage.invalidate()

            } else {
                profileImage.setImageResource(R.drawable.ic_profile_placeholder)
            }
        }
    }


    private fun showLogoutDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_logout_confirm, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val cancelBtn = dialogView.findViewById<TextView>(R.id.btnCancel)
        val logoutBtn = dialogView.findViewById<TextView>(R.id.btnLogout)

        cancelBtn.setOnClickListener { dialog.dismiss() }

        logoutBtn.setOnClickListener {
            dialog.dismiss()
            logoutUser()
        }

        dialog.show()
    }

    private fun logoutUser() {
        clearEverything()
    }


    private fun clearAllSharedPrefs(context: Context) {
        val prefsDir = File(context.applicationInfo.dataDir, "shared_prefs")
        if (prefsDir.exists() && prefsDir.isDirectory) {
            prefsDir.listFiles()?.forEach { it.delete() }
        }
    }

    private fun clearAppCache(context: Context) {
        context.cacheDir?.deleteRecursively()
        context.codeCacheDir?.deleteRecursively()
        context.externalCacheDirs?.forEach { it?.deleteRecursively() }
    }


    private fun clearDatabase(context: Context) {
        context.deleteDatabase("raxel_tracker_db.db")
    }


    private fun clearEverything() {
        if (TrackingApi.getInstance().isSdkEnabled()) {
            TrackingApi.getInstance().logout()
        }

        // Clear both SharedPreferences cleanly
        clearAllPrefs()

        clearAppCache(requireContext())

        Toast.makeText(requireContext(), "Successfully logged out", Toast.LENGTH_SHORT).show()

        val intent = Intent(requireContext(), SplashScreenActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
    }
    private fun clearAllPrefs() {
        // Normal SharedPrefs
        val prefs = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()

        // Encrypted SharedPrefs
        val masterKey = MasterKey.Builder(requireContext())
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val enPrefs = EncryptedSharedPreferences.create(
            requireContext(),
            "user_creds",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        enPrefs.edit { clear() }
    }



}

