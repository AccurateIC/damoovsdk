package com.example.accuratedamoov.ui.register

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Patterns
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.accuratedamoov.MainActivity
import com.example.accuratedamoov.R
import com.example.accuratedamoov.databinding.ActivityRegisterBinding
import com.google.android.material.snackbar.Snackbar
import androidx.core.content.edit
import com.example.accuratedamoov.ui.login.LoginActivity


class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var viewModel: RegisterViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[RegisterViewModel::class.java]

        binding.registerBtn.setOnClickListener {
            val email = binding.emailEdt.text.toString().trim()
            val password = binding.pwdEdt.text.toString().trim()
            val name = binding.nameEdt.text.toString().trim().ifEmpty { null }

            // Validate inputs
            var isValid = true

            if (email.isEmpty()) {
                binding.emailEdt.error = "Email is required"
                isValid = false
            } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.emailEdt.error = "Invalid email format"
                isValid = false
            }

            if (password.isEmpty()) {
                binding.pwdEdt.error = "Password is required"
                isValid = false
            } else if (password.length < 6) {
                binding.pwdEdt.error = "Password must be at least 6 characters"
                isValid = false
            }

            if (!isValid) return@setOnClickListener

            // Call ViewModel if valid
            viewModel.registerUser(email, password, name)
        }

        viewModel.registerResult.observe(this) { result ->
            result
                .onSuccess {
                    val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
                    prefs.edit {
                        putBoolean("is_registered", true)
                            .putBoolean("is_logged_in", true)
                            .putInt("user_id", it.user_id?.toInt() ?: 0)
                    }

                    Snackbar.make(binding.root, "Registered! ID: ${it.user_id}", Snackbar.LENGTH_LONG).show()

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

    override fun onBackPressed() {
        super.onBackPressed()
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }
}

