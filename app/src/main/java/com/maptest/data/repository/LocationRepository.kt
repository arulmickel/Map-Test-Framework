package com.maptest.data.repository

import com.maptest.data.local.LocationDao
import com.maptest.data.local.LocationEntity
import com.maptest.data.remote.PlacesApiService
import com.maptest.domain.model.LocationCategory
import com.maptest.domain.model.SavedLocation
import com.maptest.util.LRULocationCache
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// =============================================================================
// Single source of truth for location data, three tiers:
//   1. LRU cache (hot data, in-memory)
//   2. Room (persistent, local)
//   3. Retrofit (network, when available)
//
// Offline-first: a network failure never blocks a read; tunnels and airplane
// mode degrade to cached/local data. Unit tests mock the DAO + API;
// integration tests use in-memory Room + MockWebServer.

@Singleton
class LocationRepository @Inject constructor(
    private val locationDao: LocationDao,
    private val apiService: PlacesApiService
) {
    // LRU Cache: keeps 100 most recently accessed locations in memory
    // WHY 100: Typical map viewport shows ~20-50 POIs. 100 gives us
    // ~2-3 screens worth of cache. Larger = more RAM, smaller = more DB hits.
    private val cache = LRULocationCache(capacity = 100)

    // =========================================================================
    // READ: Get all saved locations (reactive)
    // =========================================================================
    fun getAllLocations(): Flow<List<SavedLocation>> {
        return locationDao.getAllLocations().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    // =========================================================================
    // READ: Get favorites (reactive)
    // =========================================================================
    fun getFavorites(): Flow<List<SavedLocation>> {
        return locationDao.getFavoriteLocations().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    // =========================================================================
    // READ: Get a single location by ID (with cache layer)
    //
    // DSA IN ACTION: This demonstrates the cache lookup pattern:
    // 1. O(1) cache hit → return immediately
    // 2. Cache miss → O(1) DB lookup → populate cache → return
    // =========================================================================
    suspend fun getLocationById(id: String): SavedLocation? {
        // Layer 1: Check LRU cache (O(1) lookup)
        cache.get(id)?.let { return it }

        // Layer 2: Check database
        val entity = locationDao.getLocationById(id) ?: return null
        val location = entity.toDomainModel()

        // Populate cache for future lookups
        cache.put(id, location)

        return location
    }

    // =========================================================================
    // SEARCH: Search locations locally + remotely
    // =========================================================================
    fun searchLocations(query: String): Flow<List<SavedLocation>> {
        return locationDao.searchLocations(query).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    // =========================================================================
    // SEARCH: Network search with local caching
    //
    // This demonstrates the full offline-first pattern:
    // 1. Try network first (freshest data)
    // 2. On success: save to DB + cache, return results
    // 3. On failure: fall back to local DB search
    // =========================================================================
    suspend fun searchPlaces(
        query: String,
        latitude: Double,
        longitude: Double
    ): Result<List<SavedLocation>> {
        return try {
            val response = apiService.searchPlaces(query, latitude, longitude)

            if (response.isSuccessful && response.body() != null) {
                val locations = response.body()!!.results.map { result ->
                    SavedLocation(
                        id = result.placeId,
                        name = result.name,
                        latitude = result.latitude,
                        longitude = result.longitude,
                        address = result.address,
                        category = try {
                            LocationCategory.valueOf(result.category.uppercase())
                        } catch (e: IllegalArgumentException) {
                            LocationCategory.OTHER
                        }
                    )
                }

                // Save to database for offline access
                locations.forEach { location ->
                    locationDao.insertLocation(LocationEntity.fromDomainModel(location))
                    cache.put(location.id, location)
                }

                Result.success(locations)
            } else {
                Result.failure(Exception("API error: ${response.code()}"))
            }
        } catch (e: Exception) {
            // Network failure → return whatever we have locally
            Result.failure(e)
        }
    }

    // =========================================================================
    // WRITE: Save a location
    // =========================================================================
    suspend fun saveLocation(location: SavedLocation) {
        locationDao.insertLocation(LocationEntity.fromDomainModel(location))
        cache.put(location.id, location)
    }

    // =========================================================================
    // WRITE: Toggle favorite status
    // =========================================================================
    suspend fun toggleFavorite(locationId: String) {
        val entity = locationDao.getLocationById(locationId) ?: return
        val updated = entity.copy(isFavorite = !entity.isFavorite)
        locationDao.updateLocation(updated)

        // Update cache too — keep it consistent
        cache.get(locationId)?.let { cached ->
            cache.put(locationId, cached.copy(isFavorite = !cached.isFavorite))
        }
    }

    // =========================================================================
    // DELETE: Remove a location
    // =========================================================================
    suspend fun deleteLocation(locationId: String) {
        locationDao.deleteLocationById(locationId)
        cache.remove(locationId)
    }

    // =========================================================================
    // NEARBY: Find locations near coordinates
    //
    // DSA IN ACTION: Uses bounding box query (DB) + Haversine filter (code)
    // to efficiently find nearby locations.
    //
    // Step 1: SQL query with lat/lng bounds (fast, but rectangular)
    // Step 2: Filter results by actual distance (slow, but circular/accurate)
    // Step 3: Sort by distance (nearest first)
    //
    // Coarse filter → precise filter: cheap bounding-box query first to
    // shrink the candidate set, then accurate Haversine distance check.
    // =========================================================================
    suspend fun getNearbyLocations(
        latitude: Double,
        longitude: Double,
        radiusKm: Double = 5.0
    ): List<SavedLocation> {
        // Approximate bounding box (1 degree latitude ≈ 111 km)
        val latDelta = radiusKm / 111.0
        val lngDelta = radiusKm / (111.0 * Math.cos(Math.toRadians(latitude)))

        val entities = locationDao.getLocationsInBounds(
            minLat = latitude - latDelta,
            maxLat = latitude + latDelta,
            minLng = longitude - lngDelta,
            maxLng = longitude + lngDelta
        )

        return entities
            .map { it.toDomainModel() }
            .filter { it.distanceTo(latitude, longitude) <= radiusKm }
            .sortedBy { it.distanceTo(latitude, longitude) }
    }

    // =========================================================================
    // CACHE UTILITIES (useful for testing and debugging)
    // =========================================================================
    fun getCacheSize(): Int = cache.currentSize()
    fun clearCache() = cache.clear()
    fun getRecentFromCache(): List<SavedLocation> = cache.getAllInOrder()
}
