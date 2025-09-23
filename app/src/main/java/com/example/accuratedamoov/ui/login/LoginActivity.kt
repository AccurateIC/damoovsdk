package com.example.accuratedamoov.ui.login

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.accuratedamoov.MainActivity
import com.example.accuratedamoov.R
import androidx.core.content.edit
import androidx.lifecycle.ViewModelProvider
import com.example.accuratedamoov.databinding.ActivityLoginBinding
import com.example.accuratedamoov.ui.register.RegisterActivity
import com.google.android.material.snackbar.Snackbar


// LoginActivity.kt
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var viewModel: LoginViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[LoginViewModel::class.java]

        binding.loginBtn.setOnClickListener {
            validateAndLogin()
        }

        viewModel.loginResult.observe(this) { result ->
            result
                .onSuccess {
                    val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
                    prefs.edit().apply {
                        putBoolean("is_registered", true)
                        putBoolean("is_logged_in", true)
                        putInt("user_id", it.user_id)
                        apply()
                    }
                    Snackbar.make(binding.root, "Welcome ${it.name}", Snackbar.LENGTH_LONG).show()

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

        binding.registerll.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun validateAndLogin() {
        val email = binding.emailEdt.text.toString().trim()
        val password = binding.pwdEdt.text.toString().trim()
        var isValid = true

        if (email.isEmpty()) {
            binding.emailEdt.error = "Email required"
            isValid = false
        }
        if (password.isEmpty()) {
            binding.pwdEdt.error = "Password required"
            isValid = false
        }
        if (isValid) {
            viewModel.loginUser(email, password)
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
    }

}
