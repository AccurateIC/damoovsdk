package com.example.accuratedamoov.ui.register

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import android.view.View
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.example.accuratedamoov.MainActivity
import com.example.accuratedamoov.R
import com.example.accuratedamoov.databinding.ActivityRegisterBinding
import com.google.android.material.snackbar.Snackbar
import androidx.core.content.edit
import androidx.core.view.WindowCompat
import androidx.core.widget.addTextChangedListener
import com.example.accuratedamoov.ui.login.LoginActivity


class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var viewModel: RegisterViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)

        // Ensures layout resizes smoothly when keyboard opens
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContentView(binding.root)

        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        viewModel = ViewModelProvider(this)[RegisterViewModel::class.java]

        val gray = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.gray))

        binding.regBtn.isEnabled = false
        binding.regBtn.backgroundTintList = gray
        binding.regBtn.alpha = 0.5f

        // Add text listeners to enable/disable button dynamically
        binding.emailEdt.addTextChangedListener(textWatcher)
        binding.nameEdt.addTextChangedListener(textWatcher)
        binding.pwdEdt.addTextChangedListener(textWatcher)
        binding.repwdEdt.addTextChangedListener(textWatcher)

        // Focus listeners to show/hide labels
        setupFocusListeners()
        setupErrorClearing()
        attachTypingErrorClear()

        // Register button click
        binding.regBtn.setOnClickListener {
            clearErrors()
            validateAndRegister()
        }

        // Observe registration result
        viewModel.registerResult.observe(this) { result ->
            result
                .onSuccess {
                    val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
                    prefs.edit().apply {
                        putBoolean("is_registered", true)
                        putBoolean("is_logged_in", true)
                        putInt("user_id", it.user_id?.toInt() ?: 0)
                        apply()
                    }

                    //Snackbar.make(binding.root, "Welcome ${it.user_id ?: "User"}!", Snackbar.LENGTH_LONG).show()

                    Handler(Looper.getMainLooper()).postDelayed({
                        val intent = Intent(this, MainActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    }, 1000)
                }
                .onFailure {
                    Snackbar.make(binding.root, "Error: ${it.message}", Snackbar.LENGTH_LONG).show()
                }
        }
    }

    private val textWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            val email = binding.emailEdt.text.toString().trim()
            val name = binding.nameEdt.text.toString().trim()
            val password = binding.pwdEdt.text.toString().trim()
            val confirmPwd = binding.repwdEdt.text.toString().trim()

            val enableButton = email.isNotEmpty() && name.isNotEmpty() &&
                    password.isNotEmpty() && confirmPwd.isNotEmpty()

            binding.regBtn.isEnabled = enableButton

            val color = if (enableButton) R.color.colorPrimary else R.color.gray
            val tint = ColorStateList.valueOf(ContextCompat.getColor(binding.root.context, color))

            binding.regBtn.isEnabled = enableButton
            binding.regBtn.backgroundTintList = tint
            binding.regBtn.alpha = if (enableButton) 1f else 0.5f


            // Update label transparency
            binding.emailTv.alpha = if (email.isNotEmpty() || binding.emailEdt.hasFocus()) 1f else 0.4f
            binding.nameTv.alpha = if (name.isNotEmpty() || binding.nameEdt.hasFocus()) 1f else 0.4f
            binding.pwdTv.alpha = if (password.isNotEmpty() || binding.pwdEdt.hasFocus()) 1f else 0.4f
            binding.repwdTv.alpha = if (confirmPwd.isNotEmpty() || binding.repwdEdt.hasFocus()) 1f else 0.4f
        }

        override fun afterTextChanged(s: Editable?) {}
    }

    private fun setupFocusListeners() {
        binding.emailEdt.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus || binding.emailEdt.text!!.isNotEmpty()) {
                binding.emailTv.visibility = View.VISIBLE
                binding.emailTv.setTextColor(getColor(R.color.gray))
            }
        }

        binding.nameEdt.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus || binding.nameEdt.text!!.isNotEmpty()) {
                binding.nameTv.visibility = View.VISIBLE
                binding.nameTv.setTextColor(getColor(R.color.gray))
            }
        }

        binding.pwdEdt.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus || binding.pwdEdt.text!!.isNotEmpty()) {
                binding.pwdTv.visibility = View.VISIBLE
                binding.pwdTv.setTextColor(getColor(R.color.gray))
            }
        }

        binding.repwdEdt.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus || binding.repwdEdt.text!!.isNotEmpty()) {
                binding.repwdTv.visibility = View.VISIBLE
                binding.repwdTv.setTextColor(getColor(R.color.gray))
            }
        }
    }

    private fun clearErrors() {
        binding.emailEdtLayout.error = null
        binding.pwdEdtLayout.error = null
        binding.repwdEdtLayout.error = null
    }

    private fun validateAndRegister() {
        val email = binding.emailEdt.text.toString().trim()
        val password = binding.pwdEdt.text.toString().trim()
        val confirmPassword = binding.repwdEdt.text.toString().trim()
        val name = binding.nameEdt.text.toString().trim()
        val phone = binding.phoneEdt.text.toString().trim()

        var isValid = true
        if (phone.isEmpty()) {
            binding.phoneEdtLayout.error = "Phone number required"
            isValid = false
        } else if (!phone.matches(Regex("^[6-9][0-9]{9}$"))) {
            binding.phoneEdtLayout.error = "Enter valid 10-digit phone number"
            isValid = false
        }


        if (email.isEmpty()) {
            binding.emailEdtLayout.error = "Email required"
            isValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailEdtLayout.error = "Invalid email format"
            isValid = false
        }

        if (name.isEmpty()) {
            binding.nameEdtLayout.error = "Name required"
            isValid = false
        }

        if (password.isEmpty()) {
            binding.pwdEdtLayout.error = "Password required"
            isValid = false
        } else if (password.length < 6) {
            binding.pwdEdtLayout.error = "At least 6 characters"
            isValid = false
        }

        if (confirmPassword != password) {
            binding.repwdEdtLayout.error = "Passwords do not match"
            isValid = false
        }

        if (!isValid) return

        viewModel.registerUser(email, password, name, phone)

    }

    override fun onBackPressed() {
        super.onBackPressed()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    private fun setupErrorClearing() {
        binding.emailEdt.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) binding.emailEdtLayout.error = null
        }

        binding.nameEdt.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) binding.nameEdtLayout.error = null
        }

        binding.pwdEdt.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) binding.pwdEdtLayout.error = null
        }

        binding.repwdEdt.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) binding.repwdEdtLayout.error = null
        }

        binding.phoneEdt.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) binding.phoneEdtLayout.error = null
        }
    }

    private fun attachTypingErrorClear() {
        binding.emailEdt.addTextChangedListener { binding.emailEdtLayout.error = null }
        binding.nameEdt.addTextChangedListener { binding.nameEdtLayout.error = null }
        binding.pwdEdt.addTextChangedListener { binding.pwdEdtLayout.error = null }
        binding.repwdEdt.addTextChangedListener { binding.repwdEdtLayout.error = null }
        binding.phoneEdt.addTextChangedListener { binding.phoneEdtLayout.error = null }
    }


}


