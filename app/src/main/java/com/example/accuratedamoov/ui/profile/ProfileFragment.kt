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
import com.example.accuratedamoov.SplashScreenActivity
import com.example.accuratedamoov.ui.setting.SetttingsActivity
import com.telematicssdk.tracking.TrackingApi

class ProfileFragment : Fragment() {

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

        return view
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
        try {
            context.cacheDir.deleteRecursively()
            context.externalCacheDir?.deleteRecursively()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun clearDatabase(context: Context) {
        context.deleteDatabase("raxel_tracker_db.db")
    }


    private fun clearEverything() {
        if(TrackingApi.getInstance().isSdkEnabled()){
            TrackingApi.getInstance().logout()
        }
        clearAllSharedPrefs(requireContext())
        clearAppCache(requireContext())
        Toast.makeText(requireContext(), "successfully logout", Toast.LENGTH_SHORT).show()
        val intent = Intent(requireContext(), SplashScreenActivity::class.java)
        startActivity(intent)
        requireActivity().finish()

    }

}

