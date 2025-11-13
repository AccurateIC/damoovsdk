package com.example.accuratedamoov.ui.login

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.TextWatcher
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.util.Patterns
import android.view.View
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.accuratedamoov.MainActivity
import com.example.accuratedamoov.R
import androidx.core.content.edit
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.addTextChangedListener
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.accuratedamoov.data.network.OTPApiClient
import com.example.accuratedamoov.data.network.OTPApiService
import com.example.accuratedamoov.databinding.ActivityLoginBinding
import com.example.accuratedamoov.ui.register.RegisterActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit


// LoginActivity.kt
/*class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var viewModel: LoginViewModel
    private lateinit var auth: FirebaseAuth
    private var verificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        setContentView(binding.root)
        viewModel = ViewModelProvider(this)[LoginViewModel::class.java]

        loadPhoneLogin()

        viewModel.loginResult.observe(this) { result ->
            result.onSuccess { response ->
                // Login success
                Snackbar.make(binding.root, "Welcome ${response.name}", Snackbar.LENGTH_SHORT)
                    .show()
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
            result.onFailure { error ->
                Snackbar.make(binding.root, error.message ?: "Login failed", Snackbar.LENGTH_LONG)
                    .show()
            }
        }

    }

    // ----------------------------------------------------
    // Load PHONE LOGIN UI
    // ----------------------------------------------------
    private fun loadPhoneLogin() {
        val phoneView =
            layoutInflater.inflate(R.layout.layout_phone_login, binding.loginContainer, false)
        binding.loginContainer.removeAllViews()
        binding.loginContainer.addView(phoneView)

        val loginWithEmail = phoneView.findViewById<TextView>(R.id.loginWithEmail)
        highlightEmail(loginWithEmail)
        val phoneEdt = phoneView.findViewById<TextInputEditText>(R.id.phoneEdt)
        val sendOtpBtn = phoneView.findViewById<MaterialButton>(R.id.sendOtpBtn)

        auth = FirebaseAuth.getInstance() // Initialize Firebase Auth
        if (BuildConfig.DEBUG) {
            auth.firebaseAuthSettings.setAppVerificationDisabledForTesting(true)
        }
        // Send OTP on button click
        sendOtpBtn.setOnClickListener {
            val phone = phoneEdt.text.toString().trim()
            if (phone.isEmpty() || phone.length < 10) {
                Toast.makeText(this, "Enter valid phone number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val options = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber("+1$phone") // Change country code if needed
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                        // Auto-verification: OTP detected automatically
                        signInWithCredential(credential)
                    }

                    override fun onVerificationFailed(e: FirebaseException) {
                        Log.d("LoginActivity", "Verification failed: ${e.message}")
                        Toast.makeText(this@LoginActivity, e.message, Toast.LENGTH_LONG).show()
                    }

                    override fun onCodeSent(
                        verifId: String,
                        token: PhoneAuthProvider.ForceResendingToken
                    ) {
                        verificationId = verifId
                        resendToken = token

                        // Show OTP input dynamically
                        val otpLayout = layoutInflater.inflate(
                            R.layout.layout_otp_input,
                            binding.loginContainer,
                            false
                        )
                        binding.loginContainer.removeAllViews()
                        binding.loginContainer.addView(otpLayout)

                        val otpEdt = otpLayout.findViewById<TextInputEditText>(R.id.otpEdt)
                        val verifyOtpBtn = otpLayout.findViewById<MaterialButton>(R.id.verifyOtpBtn)
                        val backToPhone = otpLayout.findViewById<TextView>(R.id.backToPhone)

                        // Verify OTP
                        verifyOtpBtn.setOnClickListener {
                            val otp = otpEdt.text.toString().trim()
                            if (otp.isEmpty()) {
                                Toast.makeText(this@LoginActivity, "Enter OTP", Toast.LENGTH_SHORT)
                                    .show()
                                return@setOnClickListener
                            }
                            verificationId?.let {
                                val credential = PhoneAuthProvider.getCredential(it, otp)
                                signInWithCredential(credential)
                            }
                        }

                        // Back button to enter phone again
                        backToPhone.setOnClickListener {
                            loadPhoneLogin()
                        }
                    }
                })
                .build()

            PhoneAuthProvider.verifyPhoneNumber(options)
        }

        loginWithEmail.setOnClickListener {
            loadEmailLogin()
        }
    }

    // ----------------------------------------------------
    // Highlight only "Email"
    // ----------------------------------------------------
    private fun highlightEmail(textView: TextView) {
        val text = "Else login with Email"
        val span = SpannableString(text)
        val start = text.indexOf("Email")
        val end = start + 5

        span.setSpan(
            ForegroundColorSpan(ContextCompat.getColor(this, R.color.colorPrimary)),
            start, end,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        textView.text = span
    }

    // ----------------------------------------------------
    // Load EMAIL LOGIN UI
    // ----------------------------------------------------
    private fun loadEmailLogin() {
        val emailView =
            layoutInflater.inflate(R.layout.layout_email_login, binding.loginContainer, false)
        binding.loginContainer.removeAllViews()
        binding.loginContainer.addView(emailView)

        val backToPhone = emailView.findViewById<TextView>(R.id.backToPhone)
        val emailEdt = emailView.findViewById<TextInputEditText>(R.id.emailEdt)
        val pwdEdt = emailView.findViewById<TextInputEditText>(R.id.passwordEdt)
        val emailLayout = emailView.findViewById<TextInputLayout>(R.id.emailLayout)
        val pwdLayout = emailView.findViewById<TextInputLayout>(R.id.passwordLayout)
        val loginBtn = emailView.findViewById<MaterialButton>(R.id.emailLoginBtn)

        loginBtn.isEnabled = false
        loginBtn.alpha = 0.5f
        loginBtn.setOnClickListener {
            val email = emailEdt.text.toString().trim()
            val pwd = pwdEdt.text.toString().trim()

            // Only attempt login if valid
            if (Patterns.EMAIL_ADDRESS.matcher(email).matches() && pwd.length >= 6) {
                viewModel.loginUser(email, pwd)
            } else {
                // Optional: show error if somehow clicked while invalid
                Toast.makeText(this, "Enter valid email and password", Toast.LENGTH_SHORT).show()
            }
        }
        backToPhone.setOnClickListener { loadPhoneLogin() }

        fun validateFields(showErrors: Boolean = false) {
            val email = emailEdt.text.toString().trim()
            val pwd = pwdEdt.text.toString().trim()


            // Clear errors immediately when user types
            if (!showErrors) {
                emailLayout.error = null
                pwdLayout.error = null
            }

            if (showErrors) {
                // Show errors only when focus lost
                emailLayout.error = when {
                    email.isEmpty() -> "Email required"
                    !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> "Invalid email"
                    else -> null
                }

                pwdLayout.error = when {
                    pwd.isEmpty() -> "Password required"
                    pwd.length < 6 -> "Min 6 characters"
                    else -> null
                }
            }

            // Enable button dynamically
            val isEmailValid = Patterns.EMAIL_ADDRESS.matcher(email).matches()
            val isPwdValid = pwd.length >= 6
            val valid = isEmailValid && isPwdValid

            loginBtn.isEnabled = valid
            loginBtn.alpha = if (valid) 1f else 0.5f

        }

        // Email text change
        emailEdt.doOnTextChanged { text, _, _, _ ->
            emailLayout.error = null  // clear error immediately on typing
            validateFields()          // update login button state
        }

// Password text change
        pwdEdt.doOnTextChanged { text, _, _, _ ->
            pwdLayout.error = null    // clear error immediately on typing
            validateFields()          // update login button state
        }

        // Validate on focus lost to show errors
        emailEdt.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val email = emailEdt.text.toString().trim()
                emailLayout.error = when {
                    email.isEmpty() -> "Email required"
                    !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> "Invalid email"
                    else -> null
                }
            } else {
                emailEdt.viewTreeObserver.addOnGlobalLayoutListener(object :
                    ViewTreeObserver.OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        binding.scrollView.smoothScrollTo(0, emailLayout.bottom)
                        emailEdt.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    }
                })
            }
        }

        pwdEdt.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val pwd = pwdEdt.text.toString().trim()
                pwdLayout.error = when {
                    pwd.isEmpty() -> "Password required"
                    pwd.length < 6 -> "Min 6 characters"
                    else -> null
                }
            } else {

                pwdEdt.viewTreeObserver.addOnGlobalLayoutListener(object :
                    ViewTreeObserver.OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        binding.scrollView.smoothScrollTo(0, pwdLayout.bottom)
                        pwdEdt.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    }
                })
            }
        }


    }


    private fun signInWithCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = task.result?.user
                    Snackbar.make(
                        binding.root,
                        "Welcome ${user?.phoneNumber}",
                        Snackbar.LENGTH_SHORT
                    ).show()
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this, "Invalid OTP", Toast.LENGTH_SHORT).show()
                }
            }
    }
}*/

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var viewModel: LoginViewModel
    private val serverUrl = "http://YOUR_SERVER_IP:3000" // change to your backend

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        setContentView(binding.root)
        viewModel = ViewModelProvider(this)[LoginViewModel::class.java]
        loadPhoneLogin()
    }

    // ----------------------------------------------------
    // Load PHONE LOGIN UI (Twilio backend)
    // ----------------------------------------------------
    private fun loadPhoneLogin() {
        val phoneView = layoutInflater.inflate(R.layout.layout_phone_login, binding.loginContainer, false)
        binding.loginContainer.removeAllViews()
        binding.loginContainer.addView(phoneView)

        val loginWithEmail = phoneView.findViewById<TextView>(R.id.loginWithEmail)
        highlightEmail(loginWithEmail)
        val phoneEdt = phoneView.findViewById<TextInputEditText>(R.id.phoneEdt)
        val sendOtpBtn = phoneView.findViewById<MaterialButton>(R.id.sendOtpBtn)

        sendOtpBtn.setOnClickListener {
            val phone = phoneEdt.text.toString().trim()
            if (phone.isEmpty() || phone.length < 10) {
                Toast.makeText(this, "Enter valid phone number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            sendOtp("+91$phone") // or your country code
        }

        loginWithEmail.setOnClickListener {
            //loadEmailLogin()
        }
    }

    private fun sendOtp(phone: String) {
        lifecycleScope.launch {
            try {
                val response = OTPApiClient.instance.sendOTP(mapOf("phone" to phone))
                if (response.isSuccessful && response.body()?.success == true) {
                    val msg = response.body()?.message ?: "OTP sent successfully"
                    Toast.makeText(this@LoginActivity, msg, Toast.LENGTH_SHORT).show()
                    showOtpInput(phone)
                } else {
                    val errorMsg = response.body()?.error ?: "Failed to send OTP"
                    Toast.makeText(this@LoginActivity, errorMsg, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@LoginActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun verifyOtp(phone: String, otp: String) {
        lifecycleScope.launch {
            try {
                val response = OTPApiClient.instance.verifyOTP(mapOf("phone" to phone, "code" to otp))
                if (response.isSuccessful && response.body()?.success == true) {
                    val msg = response.body()?.message ?: "Login successful!"
                    Toast.makeText(this@LoginActivity, msg, Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                    finish()
                } else {
                    val errorMsg = response.body()?.error ?: "Invalid OTP"
                    Toast.makeText(this@LoginActivity, errorMsg, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@LoginActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }


    private fun showOtpInput(phone: String) {
        val otpLayout = layoutInflater.inflate(R.layout.layout_otp_input, binding.loginContainer, false)
        binding.loginContainer.removeAllViews()
        binding.loginContainer.addView(otpLayout)

        val otpEdt = otpLayout.findViewById<TextInputEditText>(R.id.otpEdt)
        val verifyOtpBtn = otpLayout.findViewById<MaterialButton>(R.id.verifyOtpBtn)
        val backToPhone = otpLayout.findViewById<TextView>(R.id.backToPhone)

        verifyOtpBtn.setOnClickListener {
            val otp = otpEdt.text.toString().trim()
            if (otp.isEmpty()) {
                Toast.makeText(this, "Enter OTP", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            verifyOtp(phone, otp)
        }

        backToPhone.setOnClickListener { loadPhoneLogin() }
    }


    // Keep highlightEmail() and loadEmailLogin() as in your original code
    private fun highlightEmail(textView: TextView) {
        val text = "Else login with Email"
        val span = SpannableString(text)
        val start = text.indexOf("Email")
        val end = start + 5
        span.setSpan(
            ForegroundColorSpan(ContextCompat.getColor(this, R.color.colorPrimary)),
            start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        textView.text = span
    }
}
