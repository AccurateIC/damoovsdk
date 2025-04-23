plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.android.libraries.mapsplatform.secrets.gradle.plugin)
}

android {
    namespace = "com.example.accuratedamoov"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.accuratedamoov"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation (libs.tracking)
    implementation(libs.rxjava)
    implementation(libs.rxandroid)
    implementation (libs.retrofit)
    implementation (libs.converter.gson)
    implementation (libs.logging.interceptor)
    implementation (libs.androidx.lifecycle.viewmodel.ktx.v262)
    implementation (libs.androidx.fragment.ktx)
    androidTestImplementation (libs.androidx.junit.v115)
    androidTestImplementation (libs.androidx.espresso.core.v351)
    testImplementation (libs.mockito.core)
    testImplementation(libs.junit)
    testImplementation(libs.mockito.core.v5110)

    // For Kotlin-friendly API
    testImplementation(libs.mockito.kotlin)

    // Instrumented tests (if needed)
    androidTestImplementation(libs.mockito.android)

    // JUnit (if you're not using it already)
    testImplementation(libs.junit)

    testImplementation ("io.mockk:mockk:1.13.3")

// For instrumentation tests (Android)
    androidTestImplementation ("io.mockk:mockk-android:1.13.3")



    ///maps
    implementation ("com.github.mapsforge.mapsforge:mapsforge-core:0.25.0")
    implementation ("com.github.mapsforge.mapsforge:mapsforge-map:0.25.0")
    implementation( "com.github.mapsforge.mapsforge:mapsforge-map-reader:0.25.0")
    implementation ("com.github.mapsforge.mapsforge:mapsforge-themes:0.25.0")
    implementation ("com.github.mapsforge.mapsforge:mapsforge-core:0.25.0")
    implementation ("com.github.mapsforge.mapsforge:mapsforge-poi:0.25.0")
    implementation ("com.github.mapsforge.mapsforge:mapsforge-map-android:0.25.0")
    implementation ("com.caverock:androidsvg:1.4")

    implementation ("org.osmdroid:osmdroid-android:6.1.16")
    implementation("com.google.guava:guava:31.1-android")
}