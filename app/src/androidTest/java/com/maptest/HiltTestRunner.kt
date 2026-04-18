package com.maptest

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication

// =============================================================================
// HILT TEST RUNNER
// =============================================================================
// This is referenced in build.gradle.kts:
//   testInstrumentationRunner = "com.maptest.HiltTestRunner"
//
// WHY CUSTOM RUNNER:
// The default AndroidJUnitRunner creates the real Application class.
// We need HiltTestApplication instead, which sets up Hilt's test DI container.
// This lets us swap real dependencies with fakes in test classes.
//
// WITHOUT THIS: @Inject in test classes would fail because Hilt isn't set up.
// WITH THIS: @Inject works, and we can use @UninstallModules + @BindValue
// to replace real implementations with test doubles.
// =============================================================================

class HiltTestRunner : AndroidJUnitRunner() {
    override fun newApplication(
        cl: ClassLoader?,
        className: String?,
        context: Context?
    ): Application {
        return super.newApplication(cl, HiltTestApplication::class.java.name, context)
    }
}
