package com.example.accuratedamoov.ui.profile

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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import com.example.accuratedamoov.R
import java.io.File
import androidx.core.net.toUri

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
        // your logout logic
    }

}

