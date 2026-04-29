package com.maptest.framework.base

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.maptest.ui.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith

// Foundation for instrumented tests. Subclasses inherit Hilt + Compose
// rule wiring.
//
// Rule ordering is load-bearing: HiltAndroidRule must run first (order = 0)
// so DI is ready before the Activity launches via ComposeTestRule
// (order = 1). Reversing the order produces "Hilt component not available".

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
abstract class BaseTestCase {

    // =========================================================================
    // RULES
    // =========================================================================
    // Rules are JUnit's way of adding behavior before/after each test.
    // Think of them as automated setup/teardown.
    // =========================================================================

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    // =========================================================================
    // COMMON SETUP
    // =========================================================================
    @Before
    open fun setUp() {
        // Inject Hilt dependencies into this test class
        hiltRule.inject()
    }

    // =========================================================================
    // COMMON UTILITY METHODS
    // =========================================================================

    /**
     * Wait for all animations and async operations to complete.
     * Call this after actions that trigger state changes.
     */
    protected fun waitForIdle() {
        composeTestRule.waitForIdle()
    }

    /**
     * Wait until a condition is true, with timeout.
     * Useful for waiting on async operations like network calls or DB writes.
     *
     * EXAMPLE:
     *   waitUntil(timeoutMillis = 5000) {
     *       composeTestRule.onAllNodesWithTag(TestTags.SEARCH_RESULT_ITEM)
     *           .fetchSemanticsNodes().isNotEmpty()
     *   }
     */
    protected fun waitUntil(
        timeoutMillis: Long = 3000,
        condition: () -> Boolean
    ) {
        composeTestRule.waitUntil(timeoutMillis = timeoutMillis, condition = condition)
    }
}
