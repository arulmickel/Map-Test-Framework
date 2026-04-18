package com.maptest.framework.base

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.maptest.ui.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith

// =============================================================================
// BASE TEST CASE
// =============================================================================
// ⭐ This is the FOUNDATION of the test framework. Every instrumented test
// extends this class to get common setup for free.
//
// WHY A BASE TEST CLASS:
// Without it, every test file would repeat:
//   - HiltAndroidRule setup
//   - ComposeTestRule setup
//   - Common page object instantiation
//   - Common helper initialization
//
// With it, a new test class is just:
//   class MyNewTest : BaseTestCase() {
//       @Test fun myTest() { ... }
//   }
//
// RULE ORDER MATTERS:
// @Rule(order = 0) HiltAndroidRule → Sets up DI FIRST
// @Rule(order = 1) ComposeTestRule → Launches Activity AFTER DI is ready
//
// If you reverse the order, the Activity launches before Hilt is ready
// and the app crashes. This is a common interview question:
// "Why does your test crash with 'Hilt component not available'?"
//
// INTERVIEW QUESTION: "Walk me through your test framework architecture."
// ANSWER: "I have a BaseTestCase that sets up Hilt DI and Compose test rules.
// Page Objects encapsulate UI interactions — one per screen. Helpers provide
// utilities for location mocking, network simulation, and permissions.
// TestDataBuilder creates consistent test data with sensible defaults.
// Each test class extends BaseTestCase and uses Page Objects to interact
// with the UI in a readable, maintainable way."
// =============================================================================

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
