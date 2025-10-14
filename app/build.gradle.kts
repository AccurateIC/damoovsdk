import org.ajoberstar.grgit.Grgit
import org.gradle.kotlin.dsl.implementation


plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.android.libraries.mapsplatform.secrets.gradle.plugin)
    id("org.ajoberstar.grgit") version "5.1.0"

    id("org.jetbrains.kotlin.kapt")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}


android {
    namespace = "com.example.accuratedamoov"
    compileSdk = 35
    val git: Grgit? = try {
        Grgit.open(mapOf("currentDir" to rootDir))
    } catch (e: Exception) {
        null
    }
    val gitBranch = git?.branch?.current()?.name ?: "N/A"
    val gitCommit = git?.head()?.abbreviatedId ?: "N/A"
    val gitDirty = if (git?.status()?.isClean == false) "-dirty" else ""
    val gitVersionString = "$gitBranch/$gitCommit$gitDirty"
    defaultConfig {
        applicationId = "com.example.accuratedamoov"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        // versionName: manually set here, can be automated to increment with each commit/push using CI or Grgit
        versionName = "1.0.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "APP_VERSION_NAME", "\"$versionName\"")
        buildConfigField("String", "GIT_VERSION", "\"$gitVersionString\"")
        buildConfigField("String", "GIT_BRANCH", "\"$gitBranch\"")
        buildConfigField("String", "GIT_COMMIT", "\"$gitCommit\"")
        buildConfigField("Boolean", "GIT_DIRTY", "${gitDirty.isNotEmpty()}")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        buildConfig = true
        viewBinding = true
    }

    packaging {
        resources {
            excludes += setOf(
                "META-INF/INDEX.LIST",
                "META-INF/io.netty.versions.properties",
                "META-INF/LICENSE.md",
                "META-INF/LICENSE-notice.md",
                "META-INF/DEPENDENCIES",
                "META-INF/NOTICE",
                "META-INF/LICENSE"
            )
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.play.services.maps)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.androidx.espresso.intents)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.play.services.location)
    implementation(libs.core)
    implementation(libs.androidx.legacy.support.v4)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation(libs.tracking)
    implementation(libs.rxjava)
    implementation(libs.rxandroid)
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.logging.interceptor)
    implementation(libs.androidx.lifecycle.viewmodel.ktx.v262)
    implementation(libs.androidx.fragment.ktx)
    androidTestImplementation(libs.androidx.junit.v115)
    androidTestImplementation(libs.androidx.espresso.core.v351)
    testImplementation(libs.mockito.core)
    testImplementation(libs.junit)
    testImplementation(libs.mockito.core.v5110)
    testImplementation(libs.mockito.kotlin)
    androidTestImplementation(libs.mockito.android)
    testImplementation(libs.mockk.v1133)
    androidTestImplementation(libs.mockk.android)

    // Maps and UI
    implementation(libs.mapsforge.core)
    implementation(libs.mapsforge.map)
    implementation(libs.mapsforge.map.reader)
    implementation(libs.mapsforge.themes)
    implementation(libs.mapsforge.poi)
    implementation(libs.mapsforge.map.android)
    implementation(libs.androidsvg)
    implementation(libs.osmdroid.android)
    implementation(libs.guava)
    implementation("com.facebook.shimmer:shimmer:0.5.0")
    implementation("com.airbnb.android:lottie:6.0.0")

    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.room.runtime)
    kapt(libs.androidx.room.compiler)

    implementation(platform("com.google.firebase:firebase-bom:34.1.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-crashlytics")
    implementation("androidx.security:security-crypto:1.1.0-alpha04")
}
