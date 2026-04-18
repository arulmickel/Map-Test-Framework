package com.maptest.domain.model

// =============================================================================
// DOMAIN MODEL: SavedLocation
// =============================================================================
// WHY SEPARATE DOMAIN MODELS FROM DATABASE ENTITIES:
// 
// This is Clean Architecture. The domain model is what the rest of the app
// uses. The database entity (LocationEntity) is what Room uses. They might
// look similar now, but they serve different purposes:
//
// - SavedLocation: UI and business logic use this. No Room annotations.
// - LocationEntity: Has @Entity, @PrimaryKey — tied to database schema.
//
// WHY THIS MATTERS FOR TESTING:
// In unit tests, you create SavedLocation objects freely — no database needed.
// In instrumented tests, you can test the mapping between Entity ↔ Domain.
//
// INTERVIEW QUESTION: "Why not use one class for everything?"
// ANSWER: "Separation of concerns. If the API response format changes or the
// database schema changes, I only update one layer, not the whole app."
//
// DSA CONNECTION:
// This class implements Comparable — so we can sort locations by distance
// using built-in sort algorithms. This comes up in interviews:
// "Given a list of locations, find the K nearest to the user."
// → Sort by distance, take first K. Or use a min-heap for O(n log k).
// =============================================================================

data class SavedLocation(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val address: String = "",
    val isFavorite: Boolean = false,
    val savedTimestamp: Long = System.currentTimeMillis(),
    val category: LocationCategory = LocationCategory.OTHER
) : Comparable<SavedLocation> {

    // =========================================================================
    // Calculate distance to another location using Haversine formula
    // 
    // DSA RELEVANCE: This is the "distance between two points" problem.
    // In interviews, you might be asked to find nearest neighbors,
    // cluster locations, or validate route distances.
    //
    // Haversine formula calculates great-circle distance between two points
    // on a sphere given their latitudes and longitudes.
    // =========================================================================
    fun distanceTo(otherLat: Double, otherLng: Double): Double {
        val earthRadius = 6371.0 // km

        val dLat = Math.toRadians(otherLat - latitude)
        val dLng = Math.toRadians(otherLng - longitude)

        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(latitude)) *
                Math.cos(Math.toRadians(otherLat)) *
                Math.sin(dLng / 2) * Math.sin(dLng / 2)

        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))

        return earthRadius * c
    }

    // Comparable implementation — sort by name alphabetically by default
    override fun compareTo(other: SavedLocation): Int = name.compareTo(other.name)
}

// =============================================================================
// Location categories — used for filtering and test scenarios
// =============================================================================
enum class LocationCategory {
    RESTAURANT,
    GAS_STATION,
    PARKING,
    HOSPITAL,
    HOTEL,
    SHOPPING,
    OTHER
}
