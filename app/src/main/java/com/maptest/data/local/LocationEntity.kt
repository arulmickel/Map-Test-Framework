package com.maptest.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.maptest.domain.model.LocationCategory
import com.maptest.domain.model.SavedLocation

// =============================================================================
// ROOM ENTITY: LocationEntity
// =============================================================================
// This is the database representation of a location. Room uses this class
// to create and manage the SQLite table.
//
// WHY @Entity ANNOTATIONS:
// Room generates SQL at compile time from these annotations. If you make a
// mistake (e.g., wrong column type), it fails at BUILD time, not runtime.
// This is a huge advantage over raw SQLite.
//
// TESTING RELEVANCE:
// We test LocationDao using an in-memory database. Each test gets a fresh
// database → no state leaks between tests → deterministic results.
//
// INTERVIEW QUESTION: "How do you ensure test isolation with databases?"
// ANSWER: "I use Room's inMemoryDatabaseBuilder in @Before, and close it
// in @After. Each test starts with an empty database."
// =============================================================================

@Entity(tableName = "saved_locations")
data class LocationEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val address: String = "",
    val isFavorite: Boolean = false,
    val savedTimestamp: Long = System.currentTimeMillis(),
    val category: String = LocationCategory.OTHER.name
) {
    // =========================================================================
    // Mapper: Entity → Domain Model
    // =========================================================================
    // WHY MAPPER FUNCTIONS:
    // The database layer should not leak into the UI layer.
    // This function converts Room's entity into the domain model.
    // In tests, we verify this mapping is correct.
    // =========================================================================
    fun toDomainModel(): SavedLocation = SavedLocation(
        id = id,
        name = name,
        latitude = latitude,
        longitude = longitude,
        address = address,
        isFavorite = isFavorite,
        savedTimestamp = savedTimestamp,
        category = try {
            LocationCategory.valueOf(category)
        } catch (e: IllegalArgumentException) {
            LocationCategory.OTHER
        }
    )

    companion object {
        // =====================================================================
        // Mapper: Domain Model → Entity
        // =====================================================================
        fun fromDomainModel(location: SavedLocation): LocationEntity = LocationEntity(
            id = location.id,
            name = location.name,
            latitude = location.latitude,
            longitude = location.longitude,
            address = location.address,
            isFavorite = location.isFavorite,
            savedTimestamp = location.savedTimestamp,
            category = location.category.name
        )
    }
}
