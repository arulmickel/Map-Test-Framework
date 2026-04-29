package com.maptest.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// Reads return Flow so the UI updates reactively when rows change. Writes
// are suspend (one-shot). Tests use Room.inMemoryDatabaseBuilder + Turbine
// to assert on emissions.

@Dao
interface LocationDao {

    @Query("SELECT * FROM saved_locations ORDER BY savedTimestamp DESC")
    fun getAllLocations(): Flow<List<LocationEntity>>

    @Query("SELECT * FROM saved_locations WHERE isFavorite = 1 ORDER BY name ASC")
    fun getFavoriteLocations(): Flow<List<LocationEntity>>

    @Query("SELECT * FROM saved_locations WHERE id = :locationId")
    suspend fun getLocationById(locationId: String): LocationEntity?

    // SQL LIKE substring match. The Trie-backed autocomplete in
    // util/LocationTrie handles prefix queries faster.
    @Query(
        """
        SELECT * FROM saved_locations 
        WHERE name LIKE '%' || :query || '%' 
        OR address LIKE '%' || :query || '%'
        ORDER BY name ASC
        """
    )
    fun searchLocations(query: String): Flow<List<LocationEntity>>

    // SQL has no native distance function, so this is a bounding-box query.
    // Repository layer follows up with Haversine to filter to a true radius.
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
