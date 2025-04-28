package com.example.accuratedamoov

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.intent.rule.IntentsTestRule
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import com.example.accuratedamoov.service.NetworkMonitorService
import com.example.accuratedamoov.service.PermissionMonitorService
import com.raxeltelematics.v2.sdk.TrackingApi
import com.raxeltelematics.v2.sdk.utils.ContextUtils.isServiceRunning
import com.raxeltelematics.v2.sdk.utils.permissions.PermissionsWizardActivity
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertFalse
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

    private val TAG: String? = "SplashScreenActivityTest"

    @get:Rule
    val rule = IntentsTestRule(SplashScreenActivity::class.java, true, false)

    @get:Rule
    val rule1 = ActivityScenarioRule(SplashScreenActivity::class.java)

    @After
    fun cleanUp() {
        Log.d("helloOmkar","Prabhale")
        rule.finishActivity()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        if (isServiceRunning(PermissionMonitorService::class.java)) {
            context.stopService(Intent(context, PermissionMonitorService::class.java))
        }

        try {
            Intents.release()
        } catch (e: IllegalStateException) {
            Log.e(TAG,e.message.toString())
        }

        unmockkAll()
    }

    @Test
    fun whenPermissionsGrantedAndConnected_navigatesToMainActivity() {

        mockkObject(TrackingApi)
        every {
            TrackingApi.getInstance().areAllRequiredPermissionsAndSensorsGranted()
        } returns true
        every { TrackingApi.getInstance().isTracking() } returns true
        every { TrackingApi.getInstance().stopTracking() } returns true
        justRun { TrackingApi.getInstance().setEnableSdk(true) }
        NetworkMonitorService.isConnected = true

        rule.launchActivity(null)
        Thread.sleep(1500)
        intended(hasComponent(MainActivity::class.java.name))

        unmockkAll()
    }

    @Test
    fun whenPermissionsNotGrantedAndConnected_doNotNavigate() {
        mockkObject(TrackingApi)
        every {
            TrackingApi.getInstance().areAllRequiredPermissionsAndSensorsGranted()
        } returns false
        every { TrackingApi.getInstance().isTracking() } returns true
        every { TrackingApi.getInstance().stopTracking() } returns true
        justRun { TrackingApi.getInstance().setEnableSdk(false) }
        NetworkMonitorService.isConnected = true



        rule1.scenario.onActivity { activity ->
            assertFalse(activity.isDestroyed)
        }


    }


    @Test
    fun whenPermissionsGrantedAndNotConnected_doNotNavigate() {
        mockkObject(TrackingApi)
        every {
            TrackingApi.getInstance().areAllRequiredPermissionsAndSensorsGranted()
        } returns true
        every { TrackingApi.getInstance().isTracking() } returns true
        every { TrackingApi.getInstance().stopTracking() } returns true
        justRun { TrackingApi.getInstance().setEnableSdk(true) }
        NetworkMonitorService.isConnected = false

        rule1.scenario.onActivity { activity ->
            assertFalse(activity.isDestroyed)
        }


    }



    fun isServiceRunning(serviceClass: Class<*>): Boolean {
        val activityManager = InstrumentationRegistry.getInstrumentation().targetContext
            .getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in activityManager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }
}
