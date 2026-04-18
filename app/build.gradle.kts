// =============================================================================
// APP-LEVEL build.gradle.kts
// =============================================================================
// This is where ALL the real configuration lives — dependencies, SDK versions,
// build types, test configurations.
//
// INTERVIEW TIP: Be ready to explain why each dependency exists and what
// alternatives you considered. "I chose X because..." shows engineering judgment.
// =============================================================================

import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
}

// =============================================================================
// LOAD local.properties (for sdk.dir + MAPS_API_KEY)
// =============================================================================
// Gradle does NOT auto-expose local.properties values to build scripts —
// only the Android Gradle Plugin reads sdk.dir from it. We load the file
// manually here so MAPS_API_KEY can flow into both BuildConfig and the
// AndroidManifest placeholder.
//
// Empty key is treated as "not set" — the MapScreen renders a fallback UI
// so the app still launches without a real Google Maps API key.
// =============================================================================
val localProperties = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
val mapsApiKey: String = localProperties.getProperty("MAPS_API_KEY", "")

android {
    namespace = "com.maptest"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.maptest"
        minSdk = 26        // Android 8.0 — covers 95%+ of devices
        targetSdk = 34     // Android 14 — latest stable
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "com.maptest.HiltTestRunner"

        // Manifest placeholder injects the key into AndroidManifest meta-data.
        // Empty string is fine — Maps SDK simply renders a blank tile, and
        // the Compose UI checks BuildConfig.MAPS_API_KEY to show a fallback.
        manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey

        // Surface the key to runtime code via BuildConfig so the UI layer
        // can decide whether to render the real GoogleMap or a fallback.
        buildConfigField("String", "MAPS_API_KEY", "\"$mapsApiKey\"")
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

    // =========================================================================
    // COMPOSE CONFIGURATION
    // =========================================================================
    // Jetpack Compose needs explicit opt-in because it uses a custom Kotlin
    // compiler plugin to transform @Composable functions.
    // =========================================================================
    buildFeatures {
        compose = true
        // AGP 8 made BuildConfig generation opt-in. We need it on so the UI
        // can read MAPS_API_KEY at runtime.
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    // =========================================================================
    // TEST OPTIONS
    // =========================================================================
    // animationsDisabled = true → Espresso best practice.
    // Animations cause flaky tests because Espresso waits for the UI to be
    // idle, but animations keep the UI "busy." Disabling them makes tests
    // deterministic.
    //
    // INTERVIEW QUESTION: "How do you reduce flaky tests?"
    // ANSWER: "First thing — disable animations in test builds. Then use
    // idling resources for async operations, deterministic test data, and
    // stable selectors like resource IDs and test tags instead of text."
    // =========================================================================
    testOptions {
        animationsDisabled = true
    }
}

dependencies {
    // =========================================================================
    // CORE ANDROID
    // =========================================================================
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")

    // =========================================================================
    // JETPACK COMPOSE
    // =========================================================================
    // WHY COMPOSE (not XML):
    // 1. Apple's JD explicitly mentions "Compose UI testing"
    // 2. Compose has built-in test support via Semantics tree
    // 3. Test tags in Compose are first-class citizens, not afterthoughts
    // 4. Modern Android development has moved to Compose
    //
    // INTERVIEW TIP: Know the difference between testing Compose vs XML:
    // - Compose: useUnmergedTree, semantics matchers, testTag
    // - XML: Espresso view matchers, resource IDs, view hierarchy
    // =========================================================================
    val composeBom = platform("androidx.compose:compose-bom:2024.01.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.navigation:navigation-compose:2.7.6")

    // =========================================================================
    // GOOGLE MAPS
    // =========================================================================
    // WHY GOOGLE MAPS SDK (not Mapbox, etc.):
    // Google Maps Compose library provides a Compose-native way to embed maps.
    // Apple MapKit on Android uses a similar pattern internally.
    // This is the closest public equivalent to what you'd test at Apple.
    // =========================================================================
    implementation("com.google.maps.android:maps-compose:4.3.0")
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.google.android.gms:play-services-location:21.1.0")

    // =========================================================================
    // ROOM (Local Database)
    // =========================================================================
    // WHY ROOM:
    // Maps apps MUST work offline. Room provides:
    // - Compile-time SQL verification (catches bugs before runtime)
    // - Flow/LiveData integration (reactive UI updates when DB changes)
    // - Easy migration support (when schema changes between versions)
    //
    // INTERVIEW QUESTION: "How do you test database operations?"
    // ANSWER: "I use Room's in-memory database for tests — it's fast, isolated,
    // and automatically cleaned up. Each test gets a fresh database."
    // =========================================================================
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1") // Coroutine support
    ksp("androidx.room:room-compiler:2.6.1")

    // =========================================================================
    // HILT (Dependency Injection)
    // =========================================================================
    implementation("com.google.dagger:hilt-android:2.50")
    ksp("com.google.dagger:hilt-compiler:2.50")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")

    // =========================================================================
    // NETWORKING
    // =========================================================================
    // WHY RETROFIT + OKHTTP:
    // Industry standard. Retrofit handles the API interface, OkHttp handles
    // the actual HTTP. Together they provide:
    // - Type-safe API definitions
    // - Interceptors for logging/auth/retry
    // - Easy mocking for tests (swap OkHttp client with MockWebServer)
    // =========================================================================
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // =========================================================================
    // UNIT TEST DEPENDENCIES (run on JVM, no device needed)
    // =========================================================================
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("io.mockk:mockk:1.13.9")
    testImplementation("app.cash.turbine:turbine:1.0.0") // Flow testing
    testImplementation("com.google.truth:truth:1.4.0")   // Readable assertions

    // =========================================================================
    // INSTRUMENTED TEST DEPENDENCIES (run on device/emulator)
    // =========================================================================
    // WHY SEPARATE test vs androidTest:
    // - test/ = Unit tests, run on your computer's JVM, fast (milliseconds)
    // - androidTest/ = Instrumented tests, run on device/emulator, slower
    //   but can test real UI, permissions, sensors, etc.
    //
    // For SDET work, androidTest/ is where most of your value lives.
    // =========================================================================
    androidTestImplementation(composeBom)
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.test.espresso:espresso-intents:3.5.1")
    androidTestImplementation("androidx.test.espresso:espresso-contrib:3.5.1")
    androidTestImplementation("androidx.test:rules:1.5.0")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("com.google.dagger:hilt-android-testing:2.50")
    kspAndroidTest("com.google.dagger:hilt-compiler:2.50")
    androidTestImplementation("io.mockk:mockk-android:1.13.9")
    androidTestImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    androidTestImplementation("com.google.truth:truth:1.4.0")

    // Debug-only: needed for Compose UI test tooling
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
