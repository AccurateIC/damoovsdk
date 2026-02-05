package com.example.accuratedamoov.ui.login

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.util.Patterns
import android.view.ViewTreeObserver
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.accuratedamoov.MainActivity
import com.example.accuratedamoov.R
import androidx.core.view.WindowCompat
import androidx.core.widget.addTextChangedListener
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.accuratedamoov.data.model.RegisterModel
import com.example.accuratedamoov.data.model.RegisterResponse
import com.example.accuratedamoov.data.network.ApiService
import com.example.accuratedamoov.data.network.OTPApiClient
import com.example.accuratedamoov.data.network.RetrofitClient
import com.example.accuratedamoov.databinding.ActivityLoginBinding
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.ActionCodeSettings
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.UUID
import androidx.core.content.edit
import com.example.accuratedamoov.ui.register.RegisterActivity


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


//2
/*class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var viewModel: LoginViewModel
    private lateinit var smsReceiver: BroadcastReceiver
    private lateinit var firebaseAuth: FirebaseAuth
    private val prefs by lazy {
        getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    }
    private val smsConsentLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val message = result.data?.getStringExtra(SmsRetriever.EXTRA_SMS_MESSAGE)
            val otp = parseOtpFromMessage(message)
            if (otp.isNotEmpty()) autoFillOtp(otp)
        }

    private fun parseOtpFromMessage(message: String?): String {
        return message?.let { Regex("\\b\\d{4,6}\\b").find(it)?.value } ?: ""
    }

    private fun autoFillOtp(otp: String) {
        val otpEdt = binding.loginContainer.findViewById<TextInputEditText>(R.id.otpEdt)
        otpEdt?.setText(otp)
        otpEdt?.setSelection(otp.length)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[LoginViewModel::class.java]
        firebaseAuth = FirebaseAuth.getInstance()

        registerSmsReceiver()
        loadUnifiedLogin()

        binding.reg2tv.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun registerSmsReceiver() {
        smsReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (SmsRetriever.SMS_RETRIEVED_ACTION == intent?.action) {
                    val status = intent.extras?.get(SmsRetriever.EXTRA_STATUS) as? Status
                    if (status?.statusCode == CommonStatusCodes.SUCCESS) {
                        val consentIntent = intent.extras?.getParcelable<Intent>(SmsRetriever.EXTRA_CONSENT_INTENT)
                        consentIntent?.let { smsConsentLauncher.launch(it) }
                    }
                }
            }
        }
        registerReceiver(smsReceiver, IntentFilter(SmsRetriever.SMS_RETRIEVED_ACTION), RECEIVER_EXPORTED)
    }

    private fun loadUnifiedLogin() {
        val view = layoutInflater.inflate(R.layout.layout_phone_login, binding.loginContainer, false)
        binding.loginContainer.removeAllViews()
        binding.loginContainer.addView(view)

        val inputEdt = view.findViewById<TextInputEditText>(R.id.identifierEdt)
        val sendOtpBtn = view.findViewById<MaterialButton>(R.id.sendOtpBtn)

        sendOtpBtn.setOnClickListener {
            val input = inputEdt.text.toString().trim()

            if (input.isEmpty()) {
                Toast.makeText(this, "Enter phone number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // allow only 10-digit Indian mobile number
            if (input.length == 10 && input.all { it.isDigit() }) {
                sendPhoneOtp("+91$input")
            } else {
                Toast.makeText(this, "Enter valid 10-digit mobile number", Toast.LENGTH_SHORT).show()
            }
        }
    }


    // -----------------------------
    // PHONE OTP
    // -----------------------------
    private fun sendPhoneOtp(phone: String) {
        startSmsUserConsent()
        lifecycleScope.launch {
            try {
                val response = OTPApiClient.instance.sendOTP(mapOf("phone" to phone))
                if (response.isSuccessful && response.body()?.success == true) showOtpInput(phone)
                else Toast.makeText(this@LoginActivity, response.body()?.error ?: "Failed to send OTP", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(this@LoginActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun startSmsUserConsent() {
        SmsRetriever.getClient(this).startSmsUserConsent(null)
    }

    // -----------------------------
    // EMAIL OTP using Firebase
    // -----------------------------
    private fun sendEmailOtp(email: String) {
        val actionCodeSettings = ActionCodeSettings.newBuilder()
            .setUrl("https://accuratedamoov-85d6e.firebaseapp.com/verify?email=$email")
            .setHandleCodeInApp(true)
            .setAndroidPackageName(packageName, true, null)
            .build()

        firebaseAuth.sendSignInLinkToEmail(email, actionCodeSettings)
            .addOnSuccessListener {
                Toast.makeText(this, "OTP sent to $email", Toast.LENGTH_SHORT).show()
                showOtpInput(email)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to send email OTP: ${it.message}", Toast.LENGTH_LONG).show()
            }
    }

    // -----------------------------
    // OTP Verification
    // -----------------------------
    private fun verifyOtp(input: String, otp: String) {
        if (Patterns.EMAIL_ADDRESS.matcher(input).matches()) verifyEmailOtp(input, otp)
        else verifyPhoneOtp(input, otp)
    }

    *//*private fun verifyPhoneOtp(phone: String, otp: String) {
        lifecycleScope.launch {
            try {
                val response = OTPApiClient.instance.verifyOTP(mapOf("phone" to phone, "code" to otp))
                if (response.isSuccessful && response.body()?.success == true) {
                    Toast.makeText(this@LoginActivity, "Login successful!", Toast.LENGTH_SHORT).show()
                    registerUserApi(phone,otp,"")
                } else Toast.makeText(this@LoginActivity, "Invalid OTP", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@LoginActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }*//*

    private fun verifyEmailOtp(email: String, otp: String) {
        val intent = intent
        if (firebaseAuth.isSignInWithEmailLink(intent.data.toString())) {
            firebaseAuth.signInWithEmailLink(email, intent.data.toString())
                .addOnSuccessListener {
                    Toast.makeText(this, "Email login successful!", Toast.LENGTH_SHORT).show()
                    registerUserApi(email) // <-- Register after email OTP verification
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Invalid email OTP: ${it.message}", Toast.LENGTH_LONG).show()
                }
        }
    }

    // -----------------------------
    // Show OTP input screen
    // -----------------------------
    private fun showOtpInput(input: String) {
        val otpLayout = layoutInflater.inflate(R.layout.layout_otp_input, binding.loginContainer, false)
        binding.loginContainer.removeAllViews()
        binding.loginContainer.addView(otpLayout)

        val otpEdt = otpLayout.findViewById<TextInputEditText>(R.id.otpEdt)
        val verifyOtpBtn = otpLayout.findViewById<MaterialButton>(R.id.verifyOtpBtn)
        val backBtn = otpLayout.findViewById<TextView>(R.id.backToPhone)

        verifyOtpBtn.setOnClickListener {
            val otp = otpEdt.text.toString().trim()
            if (otp.isNotEmpty()) verifyOtp(input, otp) else Toast.makeText(this, "Enter OTP", Toast.LENGTH_SHORT).show()
        }

        backBtn.setOnClickListener { loadUnifiedLogin() }
    }

    // -----------------------------
    // REGISTER USER API CALL
    // -----------------------------
    private fun verifyPhoneOtp(phone: String, otp: String) {
        lifecycleScope.launch {
            try {
                val response = OTPApiClient.instance.verifyOTP(
                    mapOf("phone" to phone, "code" to otp)
                )
                if (response.isSuccessful && response.body()?.success == true) {
                    Toast.makeText(this@LoginActivity, "OTP verified!", Toast.LENGTH_SHORT).show()

                    // OTP verified, now register user without password
                    registerUserApi(phone, otp, null)
                } else {
                    Toast.makeText(this@LoginActivity, "Invalid OTP", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@LoginActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun registerUserApi(input: String, otp: String? = null, name: String? = null) {
        val androidId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        val deviceId = UUID.nameUUIDFromBytes(androidId.toByteArray()).toString()

        val registerModel = if (Patterns.EMAIL_ADDRESS.matcher(input).matches()) {
            RegisterModel(email = input, otp = otp, name = name, device_id = deviceId, device_name = Build.MODEL)
        } else {
            RegisterModel(phone = input, otp = otp, name = name, device_id = deviceId, device_name = Build.MODEL)
        }

        lifecycleScope.launch {
            try {
                val api = RetrofitClient.getApiService(applicationContext)
                val res = api.registerUserWithDevice(registerModel)

                if (res.success) {
                    val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
                    prefs.edit().apply {
                        putBoolean("is_registered", true)
                        putBoolean("is_logged_in", true)
                        putInt("user_id", res.user_id?.toInt() ?: 0)
                        apply()
                    }
                    Toast.makeText(this@LoginActivity, res.message ?: "Registered successfully", Toast.LENGTH_SHORT).show()

                    startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this@LoginActivity, res.error ?: "Registration failed", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@LoginActivity, "Registration error: ${e.message}", Toast.LENGTH_LONG).show()
                Log.e("RegisterUserApi Failed", "Error: ${e.message}")
            }
        }
    }






    override fun onDestroy() {
        super.onDestroy()
        try { unregisterReceiver(smsReceiver) } catch (e: Exception) {}
    }
}*/


//3

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var viewModel: LoginViewModel
    private lateinit var smsReceiver: BroadcastReceiver
    private lateinit var firebaseAuth: FirebaseAuth

    private val prefs by lazy {
        getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    }

    private val smsConsentLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val message = result.data?.getStringExtra(SmsRetriever.EXTRA_SMS_MESSAGE)
            val otp = parseOtpFromMessage(message)
            if (otp.isNotEmpty()) autoFillOtp(otp)
        }

    private fun parseOtpFromMessage(message: String?): String {
        return message?.let { Regex("\\b\\d{4,6}\\b").find(it)?.value } ?: ""
    }

    private fun autoFillOtp(otp: String) {
        val otpEdt = binding.loginContainer.findViewById<TextInputEditText>(R.id.otpEdt)
        otpEdt?.setText(otp)
        otpEdt?.setSelection(otp.length)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[LoginViewModel::class.java]
        firebaseAuth = FirebaseAuth.getInstance()

        registerSmsReceiver()
       // loadPhoneLogin()
        loadEmailLogin()

        binding.reg2tv.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
            finish()
        }

        binding.reg1tv.setOnClickListener {
            loadEmailLogin()
        }

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

    // ----------------------------------------------------------------------
    // SMS USER CONSENT RECEIVER
    // ----------------------------------------------------------------------

    @RequiresApi(Build.VERSION_CODES.O)
    private fun registerSmsReceiver() {
        smsReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (SmsRetriever.SMS_RETRIEVED_ACTION == intent?.action) {
                    val status = intent.extras?.get(SmsRetriever.EXTRA_STATUS) as? Status
                    if (status?.statusCode == CommonStatusCodes.SUCCESS) {
                        val consentIntent =
                            intent.extras?.getParcelable<Intent>(SmsRetriever.EXTRA_CONSENT_INTENT)
                        consentIntent?.let { smsConsentLauncher.launch(it) }
                    }
                }
            }
        }

        registerReceiver(
            smsReceiver,
            IntentFilter(SmsRetriever.SMS_RETRIEVED_ACTION),
            RECEIVER_EXPORTED
        )
    }

    // ----------------------------------------------------------------------
    // PHONE LOGIN SCREEN
    // ----------------------------------------------------------------------

    private fun loadPhoneLogin() {
        val view = layoutInflater.inflate(R.layout.layout_phone_login, binding.loginContainer, false)
        binding.loginContainer.removeAllViews()
        binding.loginContainer.addView(view)

        val inputEdt = view.findViewById<TextInputEditText>(R.id.identifierEdt)
        val sendOtpBtn = view.findViewById<MaterialButton>(R.id.sendOtpBtn)
        val loginWithEmail = view.findViewById<TextView>(R.id.loginWithEmailActionTv)

        loginWithEmail.setOnClickListener { loadEmailLogin() }

        sendOtpBtn.setOnClickListener {
            val input = inputEdt.text.toString().trim()

            if (input.isEmpty()) {
                Toast.makeText(this, "Enter phone number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (input.length == 10 && input.all { it.isDigit() }) {
                sendPhoneOtp("+91$input")
            } else {
                Toast.makeText(this, "Enter valid 10-digit mobile number", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ----------------------------------------------------------------------
    // EMAIL LOGIN SCREEN
    // ----------------------------------------------------------------------

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
            val androidId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
            val deviceId = UUID.nameUUIDFromBytes(androidId.toByteArray()).toString()
            if (Patterns.EMAIL_ADDRESS.matcher(email).matches() && pwd.length >= 6) {
                viewModel.loginUser(email, pwd,deviceId)
            } else {
                Toast.makeText(this, "Enter valid email and password", Toast.LENGTH_SHORT).show()
            }
        }

        backToPhone.setOnClickListener {
            Snackbar.make(binding.root, "Phone login is temporary disabled, Try login using email", Snackbar.LENGTH_SHORT).show()
            //loadPhoneLogin()
        }

        fun validateFields(showErrors: Boolean = false) {
            val email = emailEdt.text.toString().trim()
            val pwd = pwdEdt.text.toString().trim()

            if (!showErrors) {
                emailLayout.error = null
                pwdLayout.error = null
            }

            if (showErrors) {
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

            val isEmailValid = Patterns.EMAIL_ADDRESS.matcher(email).matches()
            val isPwdValid = pwd.length >= 6
            val valid = isEmailValid && isPwdValid

            loginBtn.isEnabled = valid
            loginBtn.alpha = if (valid) 1f else 0.5f
        }

        emailEdt.doOnTextChanged { _, _, _, _ ->
            emailLayout.error = null
            validateFields()
        }

        pwdEdt.doOnTextChanged { _, _, _, _ ->
            pwdLayout.error = null
            validateFields()
        }

        emailEdt.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val email = emailEdt.text.toString().trim()
                emailLayout.error = when {
                    email.isEmpty() -> "Email required"
                    !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> "Invalid email"
                    else -> null
                }
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
            }
        }
    }

    // ----------------------------------------------------------------------
    // OTP SENDER
    // ----------------------------------------------------------------------

    private fun sendPhoneOtp(phone: String) {
        startSmsUserConsent()

        lifecycleScope.launch {
            try {
                val response = OTPApiClient.instance.sendOTP(mapOf("phone" to phone))
                if (response.isSuccessful && response.body()?.success == true) {
                    showOtpInput(phone)
                } else {
                    Toast.makeText(
                        this@LoginActivity,
                        response.body()?.error ?: "Failed to send OTP",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@LoginActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun startSmsUserConsent() {
        SmsRetriever.getClient(this).startSmsUserConsent(null)
    }

    // ----------------------------------------------------------------------
    // OTP INPUT SCREEN
    // ----------------------------------------------------------------------

    private fun showOtpInput(input: String) {
        val otpLayout =
            layoutInflater.inflate(R.layout.layout_otp_input, binding.loginContainer, false)

        binding.loginContainer.removeAllViews()
        binding.loginContainer.addView(otpLayout)

        val otpEdt = otpLayout.findViewById<TextInputEditText>(R.id.otpEdt)
        val verifyOtpBtn = otpLayout.findViewById<MaterialButton>(R.id.verifyOtpBtn)
        val backBtn = otpLayout.findViewById<TextView>(R.id.backToPhone)

        verifyOtpBtn.setOnClickListener {
            val otp = otpEdt.text.toString().trim()
            if (otp.isNotEmpty()) verifyOtp(input, otp)
            else Toast.makeText(this, "Enter OTP", Toast.LENGTH_SHORT).show()
        }

        backBtn.setOnClickListener { loadPhoneLogin() }
    }

    private fun verifyOtp(input: String, otp: String) {
        if (Patterns.EMAIL_ADDRESS.matcher(input).matches()) {
            verifyEmailOtp(input, otp)
        } else {
            verifyPhoneOtp(input, otp)
        }
    }

    private fun verifyPhoneOtp(phone: String, otp: String) {
        lifecycleScope.launch {
            try {
                val response = OTPApiClient.instance.verifyOTP(
                    mapOf("phone" to phone, "code" to otp)
                )
                if (response.isSuccessful && response.body()?.success == true) {
                    registerUserApi(phone, otp, null)
                } else {
                    Toast.makeText(this@LoginActivity, "Invalid OTP", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@LoginActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun verifyEmailOtp(email: String, otp: String) {
        val intent = intent
        if (firebaseAuth.isSignInWithEmailLink(intent.data.toString())) {
            firebaseAuth.signInWithEmailLink(email, intent.data.toString())
                .addOnSuccessListener {
                    registerUserApi(email)
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Invalid email OTP: ${it.message}", Toast.LENGTH_LONG)
                        .show()
                }
        }
    }

    // ----------------------------------------------------------------------
    // REGISTER USER API
    // ----------------------------------------------------------------------

    private fun registerUserApi(input: String, otp: String? = null, name: String? = null) {
        val androidId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        val deviceId = UUID.nameUUIDFromBytes(androidId.toByteArray()).toString()

        val registerModel = if (Patterns.EMAIL_ADDRESS.matcher(input).matches()) {
            RegisterModel(
                email = input,
                otp = otp,
                name = name,
                device_id = deviceId,
                device_name = Build.MODEL
            )
        } else {
            RegisterModel(
                phone = input,
                otp = otp,
                name = name,
                device_id = deviceId,
                device_name = Build.MODEL
            )
        }

        lifecycleScope.launch {
            try {
                val api = RetrofitClient.getApiService(applicationContext)
                val res = api.registerUserWithDevice(registerModel)

                if (res.success) {
                    prefs.edit().apply {
                        putBoolean("is_registered", true)
                        putBoolean("is_logged_in", true)
                        putInt("user_id", res.user_id?.toInt() ?: 0)
                        apply()
                    }

                    startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                    finish()

                } else {
                    Toast.makeText(
                        this@LoginActivity,
                        res.error ?: "Registration failed",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@LoginActivity,
                    "Registration error: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(smsReceiver)
        } catch (_: Exception) {
        }
    }
}
