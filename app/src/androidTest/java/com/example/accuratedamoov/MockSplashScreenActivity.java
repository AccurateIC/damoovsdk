package com.example.accuratedamoov;

import android.annotation.SuppressLint;

@SuppressLint("CustomSplashScreen")
public class MockSplashScreenActivity extends SplashScreenActivity {

    @Override
    public boolean allPermissionGranted() {
        return true;
    }
}
