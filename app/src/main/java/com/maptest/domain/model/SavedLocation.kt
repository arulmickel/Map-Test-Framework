package com.maptest.domain.model

// Domain model used by UI and business logic. Kept separate from
// LocationEntity (Room) and the API DTOs so schema/JSON changes don't
// propagate across layers. Implements Comparable so callers can sort by
// distance with the standard collection API.

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

    /** Great-circle distance to another point in km, via Haversine. */
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
