package com.maptest.framework.rules

import com.maptest.framework.helpers.LocationMockHelper
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

// JUnit Rule that wires mock location setup/teardown into every test that
// declares it. Rules guarantee teardown even on test failure and stack
// cleanly with other rules.
//
//   @get:Rule
//   val mockLocation = MockLocationRule(30.2672, -97.7431)

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
