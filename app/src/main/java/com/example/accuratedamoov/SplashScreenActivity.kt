package com.example.accuratedamoov

import android.R.attr.password
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Bundle
import android.provider.Settings.Secure.putString
import androidx.appcompat.app.AppCompatActivity
import com.example.accuratedamoov.ui.login.LoginActivity
import com.example.accuratedamoov.ui.setting.SetttingsActivity
import androidx.core.content.edit
import org.json.JSONObject
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import androidx.lifecycle.lifecycleScope
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.accuratedamoov.data.model.LoginRequest
import com.example.accuratedamoov.data.network.RetrofitClient
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import kotlin.properties.Delegates
import com.example.accuratedamoov.R

class SplashScreenActivity : AppCompatActivity() {

    private lateinit var enprefs: SharedPreferences
    private lateinit var api_url: String
    private var email: String? = null
    private var password: String? = null
    private var isLoggedIn by Delegates.notNull<Boolean>()
    private lateinit var prefs: SharedPreferences
    private lateinit var progressBar: ProgressBar
    private var networkSnackbar: Snackbar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        progressBar = findViewById(R.id.progressBar)

        prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        isLoggedIn = prefs.getBoolean("is_logged_in", false)
        api_url = prefs.getString("api_url", "") ?: ""

        val masterKey = MasterKey.Builder(this)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        enprefs = EncryptedSharedPreferences.create(
            this,
            "user_creds",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        email = enprefs.getString("user_email", null)
        password = enprefs.getString("user_password", null)

        observeNetwork()

        if (!isNetworkAvailable()) {
            networkSnackbar = Snackbar.make(
                findViewById(android.R.id.content),
                "No internet connection. Waiting to reconnect...",
                Snackbar.LENGTH_INDEFINITE
            )
            networkSnackbar?.show()
        }

        // Instead of calling API, just check is_logged_in
        handleNextScreen()
    }

    private fun observeNetwork() {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkRequest = NetworkRequest.Builder().build()

        connectivityManager.registerNetworkCallback(
            networkRequest,
            object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    runOnUiThread { networkSnackbar?.dismiss() }
                }

                override fun onLost(network: Network) {
                    runOnUiThread {
                        networkSnackbar = Snackbar.make(
                            findViewById(android.R.id.content),
                            "No internet connection. Waiting to reconnect...",
                            Snackbar.LENGTH_INDEFINITE
                        )
                        networkSnackbar?.show()
                    }
                }
            }
        )
    }

    private fun handleNextScreen() {
        progressBar.visibility = View.GONE

        val nextIntent = if (isLoggedIn) {
            // User already logged in, go to MainActivity
            Intent(this, MainActivity::class.java)
        } else {
            // Not logged in, go to Login or Settings
            if (api_url.isEmpty()) {
                Intent(this, SetttingsActivity::class.java)
            } else {
                Intent(this, LoginActivity::class.java)
            }
        }.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        startActivity(nextIntent)
    }

    // -------------------------------
    // COMMENTED OUT: API login code
    /*
    private fun attemptAutoLogin() {
        // Previously attempted auto-login via API
    }

    private fun callLoginApi(email: String, password: String) {
        // Previously called Retrofit login API
    }

    private fun showLoginFallback() {
        // Previously showed fallback Snackbar
    }
    */

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}

