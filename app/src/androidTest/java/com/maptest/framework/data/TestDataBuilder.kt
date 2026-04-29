package com.maptest.framework.data

import com.maptest.data.local.LocationEntity
import com.maptest.data.remote.PlaceResult
import com.maptest.data.remote.PlacesSearchResponse
import com.maptest.domain.model.LocationCategory
import com.maptest.domain.model.SavedLocation

// Builder for valid test objects with sensible defaults. Each test
// overrides only the fields it actually exercises:
//
//   val favorited = TestDataBuilder.location(isFavorite = true)
//   val nearby    = TestDataBuilder.location(latitude = 30.0, longitude = -97.0)
//
// Adding a new field to SavedLocation means updating one default here,
// not 50 tests.

object TestDataBuilder {

    fun location(
        id: String = "test_location_${System.nanoTime()}",
        name: String = "Test Location",
        latitude: Double = 30.2672,    // Austin, TX
        longitude: Double = -97.7431,
        address: String = "123 Test St, Austin, TX 78701",
        isFavorite: Boolean = false,
        savedTimestamp: Long = System.currentTimeMillis(),
        category: LocationCategory = LocationCategory.OTHER
    ) = SavedLocation(
        id = id,
        name = name,
        latitude = latitude,
        longitude = longitude,
        address = address,
        isFavorite = isFavorite,
        savedTimestamp = savedTimestamp,
        category = category
    )

    // =========================================================================
    // LOCATION ENTITY BUILDER (for database tests)
    // =========================================================================
    fun locationEntity(
        id: String = "test_entity_${System.nanoTime()}",
        name: String = "Test Entity",
        latitude: Double = 30.2672,
        longitude: Double = -97.7431,
        address: String = "123 Test St, Austin, TX 78701",
        isFavorite: Boolean = false,
        savedTimestamp: Long = System.currentTimeMillis(),
        category: String = LocationCategory.OTHER.name
    ) = LocationEntity(
        id = id,
        name = name,
        latitude = latitude,
        longitude = longitude,
        address = address,
        isFavorite = isFavorite,
        savedTimestamp = savedTimestamp,
        category = category
    )

    // =========================================================================
    // API RESPONSE BUILDERS (for MockWebServer tests)
    // =========================================================================
    fun placeResult(
        placeId: String = "place_${System.nanoTime()}",
        name: String = "Test Place",
        latitude: Double = 30.2672,
        longitude: Double = -97.7431,
        address: String = "456 API St, Austin, TX 78701",
        category: String = "RESTAURANT",
        rating: Double? = 4.5,
        isOpen: Boolean? = true
    ) = PlaceResult(
        placeId = placeId,
        name = name,
        latitude = latitude,
        longitude = longitude,
        address = address,
        category = category,
        rating = rating,
        isOpen = isOpen
    )

    fun searchResponse(
        results: List<PlaceResult> = listOf(placeResult()),
        status: String = "OK",
        totalResults: Int = results.size
    ) = PlacesSearchResponse(
        results = results,
        status = status,
        totalResults = totalResults
    )

    // =========================================================================
    // PREDEFINED TEST DATASETS
    // =========================================================================
    // Common datasets used across multiple tests.
    // Named descriptively so tests read like documentation.
    // =========================================================================

    /** Austin, TX landmarks for map display tests */
    fun austinLandmarks() = listOf(
        location(
            id = "capitol",
            name = "Texas State Capitol",
            latitude = 30.2747,
            longitude = -97.7404,
            address = "1100 Congress Ave, Austin, TX 78701",
            category = LocationCategory.OTHER
        ),
        location(
            id = "zilker",
            name = "Zilker Park",
            latitude = 30.2669,
            longitude = -97.7729,
            address = "2100 Barton Springs Rd, Austin, TX 78704",
            category = LocationCategory.OTHER
        ),
        location(
            id = "ut_tower",
            name = "UT Tower",
            latitude = 30.2862,
            longitude = -97.7394,
            address = "110 Inner Campus Drive, Austin, TX 78705",
            category = LocationCategory.OTHER
        )
    )

    /** Locations spread across different distances for nearby search tests */
    fun locationsAtVaryingDistances(
        centerLat: Double = 30.2672,
        centerLng: Double = -97.7431
    ) = listOf(
        location(id = "very_close", name = "Very Close (0.1km)",
            latitude = centerLat + 0.001, longitude = centerLng + 0.001),
        location(id = "close", name = "Close (1km)",
            latitude = centerLat + 0.009, longitude = centerLng + 0.009),
        location(id = "medium", name = "Medium (5km)",
            latitude = centerLat + 0.045, longitude = centerLng + 0.045),
        location(id = "far", name = "Far (15km)",
            latitude = centerLat + 0.135, longitude = centerLng + 0.135),
        location(id = "very_far", name = "Very Far (50km)",
            latitude = centerLat + 0.450, longitude = centerLng + 0.450)
    )

    /** Mix of favorite and non-favorite for favorites screen tests */
    fun mixedFavorites() = listOf(
        location(id = "fav_1", name = "Favorite Coffee Shop", isFavorite = true,
            category = LocationCategory.RESTAURANT),
        location(id = "fav_2", name = "Favorite Park", isFavorite = true,
            category = LocationCategory.OTHER),
        location(id = "not_fav_1", name = "Random Gas Station", isFavorite = false,
            category = LocationCategory.GAS_STATION),
        location(id = "fav_3", name = "Favorite Hospital", isFavorite = true,
            category = LocationCategory.HOSPITAL),
        location(id = "not_fav_2", name = "Random Hotel", isFavorite = false,
            category = LocationCategory.HOTEL)
    )

    /** Edge case data for boundary testing */
    fun edgeCaseLocations() = listOf(
        location(id = "null_island", name = "Null Island",
            latitude = 0.0, longitude = 0.0),
        location(id = "north_pole", name = "North Pole",
            latitude = 90.0, longitude = 0.0),
        location(id = "south_pole", name = "South Pole",
            latitude = -90.0, longitude = 0.0),
        location(id = "date_line", name = "International Date Line",
            latitude = 0.0, longitude = 180.0),
        location(id = "empty_name", name = "",
            latitude = 30.0, longitude = -97.0),
        location(id = "special_chars", name = "Café & Résumé — L'Artiste",
            latitude = 30.0, longitude = -97.0),
        location(id = "very_long_name",
            name = "A".repeat(500),
            latitude = 30.0, longitude = -97.0)
    )

    // =========================================================================
    // JSON BUILDERS (for MockWebServer responses)
    // =========================================================================
    fun successJsonResponse(places: List<PlaceResult> = listOf(placeResult())): String {
        val resultsJson = places.joinToString(",") { place ->
            """
            {
                "placeId": "${place.placeId}",
                "name": "${place.name}",
                "latitude": ${place.latitude},
                "longitude": ${place.longitude},
                "address": "${place.address}",
                "category": "${place.category}",
                "rating": ${place.rating ?: "null"},
                "isOpen": ${place.isOpen ?: "null"}
            }
            """.trimIndent()
        }

        return """
        {
            "results": [$resultsJson],
            "status": "OK",
            "totalResults": ${places.size}
        }
        """.trimIndent()
    }

    fun errorJsonResponse(
        status: String = "ERROR",
        message: String = "Something went wrong"
    ): String = """
    {
        "results": [],
        "status": "$status",
        "totalResults": 0,
        "error": "$message"
    }
    """.trimIndent()
}
