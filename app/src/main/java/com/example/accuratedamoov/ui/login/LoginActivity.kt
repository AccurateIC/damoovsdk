package com.example.accuratedamoov.ui.login

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.accuratedamoov.MainActivity
import com.example.accuratedamoov.R
import androidx.core.content.edit
import com.example.accuratedamoov.databinding.ActivityLoginBinding
import com.example.accuratedamoov.ui.register.RegisterActivity


class LoginActivity : AppCompatActivity() {


    private lateinit var activityLoginBinding: ActivityLoginBinding



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityLoginBinding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(activityLoginBinding.root)

        activityLoginBinding.rgstrBtn.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        activityLoginBinding.signInBtn.setOnClickListener {
            val username = activityLoginBinding.usrEdt.text.toString().trim()
            val password = activityLoginBinding.pwdEdt.text.toString().trim()

            if (username.isNotEmpty() && password.isNotEmpty()) {
                if (username == "admin" && password == "admin") {
                    val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
                    prefs.edit { putBoolean("is_registered", true) }

                    Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show()

                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this, "Invalid credentials", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Please enter both fields", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
