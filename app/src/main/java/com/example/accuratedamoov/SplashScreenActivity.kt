package com.example.accuratedamoov

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.accuratedamoov.ui.login.LoginActivity
import com.example.accuratedamoov.ui.setting.SetttingsActivity

class SplashScreenActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val isLoggedIn = prefs.getBoolean("is_logged_in", false)
        val api_url = prefs.getString("api_url", "")

        if (isLoggedIn) {
            startActivity(Intent(this, MainActivity::class.java))
        } else {
            if (api_url.isNullOrEmpty()) {
                startActivity(Intent(this, SetttingsActivity::class.java))
            } else {
                startActivity(Intent(this, LoginActivity::class.java))

            }

        }
        finish()
    }
}