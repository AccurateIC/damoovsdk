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
import com.example.accuratedamoov.databinding.ActivityLoginBinding
import com.example.accuratedamoov.ui.register.RegisterActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout


// LoginActivity.kt
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var viewModel: LoginViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        setContentView(binding.root)
        viewModel = ViewModelProvider(this)[LoginViewModel::class.java]

        loadPhoneLogin()
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

    pwdEdt.setOnFocusChangeListener{ _, hasFocus ->
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




}

