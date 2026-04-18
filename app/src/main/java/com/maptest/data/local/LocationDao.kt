package com.maptest.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// =============================================================================
// DATA ACCESS OBJECT (DAO): LocationDao
// =============================================================================
// The DAO defines all database operations. Room generates the implementation
// at compile time from these abstract methods.
//
// WHY Flow RETURN TYPES (not LiveData, not suspend):
// - Flow is reactive: UI automatically updates when data changes
// - Flow works with coroutines (consistent async pattern)
// - Flow can be tested with Turbine library (clean assertions)
// - LiveData is lifecycle-aware but harder to test and transform
//
// INTERVIEW QUESTION: "What's the difference between a suspend function
// and a Flow-returning function in a DAO?"
// ANSWER: "Suspend functions are one-shot — call once, get one result.
// Flow functions are reactive — they emit a new value every time the
// underlying data changes. Use suspend for writes (insert/delete),
// Flow for reads that the UI observes."
//
// TESTING APPROACH:
// 1. Create in-memory database
// 2. Insert test data using suspend functions
// 3. Collect Flow emissions using Turbine
// 4. Assert values match expected
// 5. Close database in @After
// =============================================================================

@Dao
interface LocationDao {

    // =========================================================================
    // READ OPERATIONS (return Flow for reactive updates)
    // =========================================================================

    @Query("SELECT * FROM saved_locations ORDER BY savedTimestamp DESC")
    fun getAllLocations(): Flow<List<LocationEntity>>

    @Query("SELECT * FROM saved_locations WHERE isFavorite = 1 ORDER BY name ASC")
    fun getFavoriteLocations(): Flow<List<LocationEntity>>

    @Query("SELECT * FROM saved_locations WHERE id = :locationId")
    suspend fun getLocationById(locationId: String): LocationEntity?

    // =========================================================================
    // SEARCH: Uses SQL LIKE for pattern matching
    // 
    // DSA CONNECTION: This is a basic string search. In interviews, you might
    // be asked to implement more efficient search (Trie for autocomplete,
    // or fuzzy matching for typo tolerance).
    // =========================================================================
    @Query(
        """
        SELECT * FROM saved_locations 
        WHERE name LIKE '%' || :query || '%' 
        OR address LIKE '%' || :query || '%'
        ORDER BY name ASC
        """
    )
    fun searchLocations(query: String): Flow<List<LocationEntity>>

    // =========================================================================
    // NEARBY SEARCH: Find locations within a bounding box
    //
    // WHY BOUNDING BOX (not radius):
    // SQL doesn't have a built-in "distance" function. We approximate
    // "nearby" by checking if lat/lng fall within a box. The actual distance
    // filtering happens in the Repository layer using Haversine formula.
    //
    // DSA CONNECTION: This is a 2D range query. In interviews, this maps to
    // "find all points within a rectangle" — solvable with sorted arrays +
    // binary search, or with a KD-Tree for optimal performance.
    // =========================================================================
    @Query(
        """
        SELECT * FROM saved_locations
        WHERE latitude BETWEEN :minLat AND :maxLat
        AND longitude BETWEEN :minLng AND :maxLng
        """
    )
    suspend fun getLocationsInBounds(
        minLat: Double,
        maxLat: Double,
        minLng: Double,
        maxLng: Double
    ): List<LocationEntity>

    // =========================================================================
    // WRITE OPERATIONS (suspend — one-shot, not reactive)
    // =========================================================================

    // OnConflictStrategy.REPLACE: If a location with the same ID exists,
    // update it instead of failing. This is idempotent — safe to call multiple
    // times with the same data.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocation(location: LocationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocations(locations: List<LocationEntity>)

    @Update
    suspend fun updateLocation(location: LocationEntity)

    @Delete
    suspend fun deleteLocation(location: LocationEntity)

    @Query("DELETE FROM saved_locations WHERE id = :locationId")
    suspend fun deleteLocationById(locationId: String)

    @Query("DELETE FROM saved_locations")
    suspend fun deleteAllLocations()

    // =========================================================================
    // AGGREGATE QUERIES
    // =========================================================================
    @Query("SELECT COUNT(*) FROM saved_locations")
    suspend fun getLocationCount(): Int

    @Query("SELECT COUNT(*) FROM saved_locations WHERE isFavorite = 1")
    suspend fun getFavoriteCount(): Int
}
