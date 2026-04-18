package com.maptest.framework.rules

import com.maptest.framework.helpers.LocationMockHelper
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

// =============================================================================
// MOCK LOCATION RULE
// =============================================================================
// A JUnit Rule that automatically sets up and tears down mock location
// for every test that uses it.
//
// WHY A RULE (not manual @Before/@After):
// - Rules are reusable — add to any test class with one line
// - Rules guarantee teardown even if the test crashes
// - Rules can be composed (stack multiple rules)
//
// USAGE:
//   @get:Rule
//   val mockLocation = MockLocationRule(30.2672, -97.7431) // Austin
//
//   @Test fun testMapShowsUserLocation() {
//       // Location is already set to Austin when this runs
//       mapPage.assertMapCenteredOn(30.2672, -97.7431)
//   }
//
// INTERVIEW QUESTION: "How do you handle test setup that's shared across tests?"
// ANSWER: "I use custom JUnit Rules for cross-cutting concerns like mock
// location, permission grants, and network simulation. Rules guarantee
// cleanup even on test failure and can be composed."
// =============================================================================

class MockLocationRule(
    private val latitude: Double = 30.2672,
    private val longitude: Double = -97.7431
) : TestRule {

    val locationHelper = LocationMockHelper()

    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                // SETUP: Enable mock location before test
                locationHelper.enableMockLocation()
                locationHelper.setLocation(latitude, longitude)

                try {
                    // RUN: Execute the actual test
                    base.evaluate()
                } finally {
                    // TEARDOWN: Disable mock location after test
                    // This runs even if the test fails/crashes
                    locationHelper.disableMockLocation()
                }
            }
        }
    }
}
