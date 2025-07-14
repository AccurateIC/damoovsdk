package com.example.accuratedamoov.ui.register

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.accuratedamoov.R


class RegisterActivity : AppCompatActivity() {

    private val viewModel: RegisterViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register) // Reuse same layout
    }
}
