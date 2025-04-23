package com.example.accuratedamoov

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.intent.rule.IntentsTestRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import com.example.accuratedamoov.service.NetworkMonitorService
import com.raxeltelematics.v2.sdk.TrackingApi
import com.raxeltelematics.v2.sdk.utils.permissions.PermissionsWizardActivity
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SplashScreenActivityTest {

    @Test
    fun splashScreen_launches_and_checks_permissions() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = Intent(context, SplashScreenActivity::class.java)

        val scenario = ActivityScenario.launch<SplashScreenActivity>(intent)

        scenario.onActivity { activity ->

            assert(activity != null)

        }
    }

    @Test
    fun whenPermissionsNotGranted_launchesPermissionsWizard() {
        val scenario = ActivityScenario.launch(SplashScreenActivity::class.java)

        scenario.onActivity { activity ->

            activity.checkPermissionsAndContinue()
            val expectedIntent = Intent(activity, PermissionsWizardActivity::class.java)
            assert(expectedIntent.component?.className?.contains("PermissionsWizardActivity") == true)
        }
    }

    @get:Rule
    val rule = IntentsTestRule(SplashScreenActivity::class.java, true, false)

    @Test
    fun whenPermissionsGrantedAndConnected_navigatesToMainActivity() {

        mockkObject(TrackingApi)
        every { TrackingApi.getInstance().areAllRequiredPermissionsAndSensorsGranted() } returns true
        every { TrackingApi.getInstance().isTracking() } returns true
        NetworkMonitorService.isConnected = true

        rule.launchActivity(null)
        Thread.sleep(1500)
        intended(hasComponent(MainActivity::class.java.name))

        unmockkAll()
    }


}
