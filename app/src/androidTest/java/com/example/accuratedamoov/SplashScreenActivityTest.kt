package com.example.accuratedamoov


import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4

import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed


@RunWith(AndroidJUnit4::class)
class MockSplashScreenActivityTest {

    // Grant permissions rule

    @Test
    fun testPermissionsGrantedNavigatesToMainActivity() {
        // Launch SplashScreenActivity
        ActivityScenario.launch(MockSplashScreenActivity::class.java)

        // Wait for splash screen delay + transition
        Thread.sleep(3000)

        // Check splash is shown
        onView(withId(R.id.splashscreenll)).check(matches(isDisplayed()))

        // Check MainActivity view
        onView(withId(R.id.container)).check(matches(isDisplayed()))
    }



}
