package com.example.accuratedamoov

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.accuratedamoov.databinding.ActivityMainBinding
import com.raxeltelematics.v2.sdk.utils.permissions.PermissionsWizardActivity

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    val TAG:String = this::class.java.simpleName
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        startActivityForResult(
            PermissionsWizardActivity
                .getStartWizardIntent(
                    context = this,
                    enableAggressivePermissionsWizard  = false,
                    enableAggressivePermissionsWizardPage  = false
                ),
            PermissionsWizardActivity.WIZARD_PERMISSIONS_CODE
        )

        val navView: BottomNavigationView = binding.navView

        val navController = findNavController(R.id.nav_host_fragment_activity_main)

        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home, R.id.navigation_feed, R.id.navigation_notifications
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
    }



    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PermissionsWizardActivity.WIZARD_PERMISSIONS_CODE) {
            when (resultCode) {
                PermissionsWizardActivity.WIZARD_RESULT_ALL_GRANTED -> {
                    Log.d(TAG, "onActivityResult: WIZARD_RESULT_ALL_GRANTED")
                    Toast.makeText(this, "All permissions was granted", Toast.LENGTH_SHORT).show()
                }
                PermissionsWizardActivity.WIZARD_RESULT_NOT_ALL_GRANTED -> {
                    Log.d(TAG, "onActivityResult: WIZARD_RESULT_NOT_ALL_GRANTED")
                    Toast.makeText(this, "All permissions was not granted", Toast.LENGTH_SHORT)
                        .show()
                }
                PermissionsWizardActivity.WIZARD_RESULT_CANCELED -> {
                    Log.d(TAG, "onActivityResult: WIZARD_RESULT_CANCELED")
                    Toast.makeText(this, "Wizard cancelled", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}