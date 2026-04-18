package com.maptest.framework.helpers

import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.SystemClock
import androidx.test.platform.app.InstrumentationRegistry

// =============================================================================
// LOCATION MOCK HELPER
// =============================================================================
// ⭐ CRITICAL FOR MAPKIT SDET INTERVIEWS
//
// Maps apps need GPS coordinates. In tests, we can't rely on real GPS:
// - Tests run on emulators (no real GPS)
// - Tests need deterministic coordinates (same location every time)
// - Tests need to simulate movement (walking, driving)
//
// This helper injects fake GPS coordinates so the app thinks the user
// is at a specific location.
//
// HOW IT WORKS:
// Android has a "mock location provider" API. We register as a test
// provider, then push fake Location objects. The app's FusedLocationProvider
// receives them as if they were real GPS readings.
//
// INTERVIEW QUESTION: "How do you test location-based features?"
// ANSWER: "I use Android's mock location provider to inject deterministic
// coordinates. My LocationMockHelper can simulate being at any location,
// moving between locations, and even GPS signal loss. This lets me test
// geofencing triggers, location permission flows, and map camera updates
// without any real GPS hardware."
// =============================================================================

class LocationMockHelper {

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE)
        as LocationManager

    private val providerName = LocationManager.GPS_PROVIDER

    // =========================================================================
    // SETUP / TEARDOWN
    // =========================================================================

    /**
     * Register this helper as a mock location provider.
     * Call in @Before or test setup.
     */
    fun enableMockLocation() {
        try {
            locationManager.addTestProvider(
                providerName,
                false, // requiresNetwork
                false, // requiresSatellite
                false, // requiresCell
                false, // hasMonetaryCost
                true,  // supportsAltitude
                true,  // supportsSpeed
                true,  // supportsBearing
                android.location.Criteria.POWER_LOW,
                android.location.Criteria.ACCURACY_FINE
            )
            locationManager.setTestProviderEnabled(providerName, true)
        } catch (e: SecurityException) {
            // Mock location permission not granted — skip silently in CI
            // or throw for local debugging
        }
    }

    /**
     * Unregister mock provider. Call in @After or test teardown.
     */
    fun disableMockLocation() {
        try {
            locationManager.setTestProviderEnabled(providerName, false)
            locationManager.removeTestProvider(providerName)
        } catch (e: Exception) {
            // Provider might not exist if setup failed
        }
    }

    // =========================================================================
    // MOCK LOCATION INJECTION
    // =========================================================================

    /**
     * Set the device's location to the given coordinates.
     *
     * EXAMPLE:
     *   locationMock.setLocation(30.2672, -97.7431) // Austin, TX
     *   // App now thinks user is in Austin
     */
    fun setLocation(
        latitude: Double,
        longitude: Double,
        accuracy: Float = 3.0f,   // 3 meters — very accurate
        altitude: Double = 0.0,
        speed: Float = 0.0f
    ) {
        val location = createMockLocation(latitude, longitude, accuracy, altitude, speed)
        try {
            locationManager.setTestProviderLocation(providerName, location)
        } catch (e: SecurityException) {
            // Handle gracefully
        }
    }

    /**
     * Simulate movement along a path of coordinates.
     * Useful for testing:
     * - Route tracking
     * - Geofence enter/exit
     * - Map camera follow
     *
     * EXAMPLE:
     *   locationMock.simulateRoute(
     *       points = listOf(
     *           30.2672 to -97.7431,  // Start: Downtown Austin
     *           30.2747 to -97.7404,  // Mid: Capitol
     *           30.2862 to -97.7394   // End: UT Campus
     *       ),
     *       intervalMs = 1000 // 1 second between points
     *   )
     *
     * DSA CONNECTION: This iterates through an array of coordinate pairs.
     * In interviews, you might be asked to calculate total distance of a route
     * (sum of distances between consecutive points — simple array traversal).
     */
    fun simulateRoute(
        points: List<Pair<Double, Double>>,
        intervalMs: Long = 1000
    ) {
        points.forEachIndexed { index, (lat, lng) ->
            val speed = if (index > 0) {
                val prevLat = points[index - 1].first
                val prevLng = points[index - 1].second
                // Calculate speed based on distance and interval
                val distanceMeters = calculateDistance(prevLat, prevLng, lat, lng) * 1000
                (distanceMeters / (intervalMs / 1000f))
            } else {
                0f
            }

            setLocation(lat, lng, speed = speed)
            Thread.sleep(intervalMs)
        }
    }

    /**
     * Simulate GPS signal loss.
     * The app should handle this gracefully (show last known location,
     * display "GPS unavailable" message, etc.)
     */
    fun simulateGpsLoss() {
        try {
            locationManager.setTestProviderEnabled(providerName, false)
        } catch (e: Exception) {
            // Handle
        }
    }

    /**
     * Restore GPS signal after simulated loss.
     */
    fun restoreGps() {
        try {
            locationManager.setTestProviderEnabled(providerName, true)
        } catch (e: Exception) {
            // Handle
        }
    }

    // =========================================================================
    // PREDEFINED LOCATIONS (for convenience)
    // =========================================================================

    fun setLocationToAustin() = setLocation(30.2672, -97.7431)
    fun setLocationToChicago() = setLocation(41.8781, -87.6298)
    fun setLocationToSanFrancisco() = setLocation(37.7749, -122.4194)
    fun setLocationToNullIsland() = setLocation(0.0, 0.0) // Edge case!

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    private fun createMockLocation(
        latitude: Double,
        longitude: Double,
        accuracy: Float,
        altitude: Double,
        speed: Float
    ): Location {
        return Location(providerName).apply {
            this.latitude = latitude
            this.longitude = longitude
            this.accuracy = accuracy
            this.altitude = altitude
            this.speed = speed
            this.time = System.currentTimeMillis()
            this.elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                this.bearingAccuracyDegrees = 0.1f
                this.verticalAccuracyMeters = accuracy
                this.speedAccuracyMetersPerSecond = 0.01f
            }
        }
    }

    private fun calculateDistance(
        lat1: Double, lng1: Double,
        lat2: Double, lng2: Double
    ): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLng / 2) * Math.sin(dLng / 2)
        return r * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    }
}
