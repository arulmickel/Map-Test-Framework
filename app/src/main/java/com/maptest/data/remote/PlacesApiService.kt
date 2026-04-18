package com.maptest.data.remote

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

// =============================================================================
// PLACES API SERVICE (Retrofit)
// =============================================================================
// This defines the network API for searching places/locations.
// In a real app, this would call Google Places API or a custom backend.
//
// WHY RETROFIT:
// - Type-safe HTTP client — define API as a Kotlin interface
// - Automatic JSON parsing (Gson/Moshi)
// - Works with coroutines (suspend functions)
// - Easy to mock for testing (just implement the interface)
//
// TESTING APPROACH:
// 1. Unit tests: Mock this interface with MockK → return fake responses
// 2. Instrumented tests: Use MockWebServer → intercept HTTP calls and
//    return predefined JSON responses. No real network calls in tests.
//
// INTERVIEW QUESTION: "How do you test API integrations?"
// ANSWER: "Unit tests mock the Retrofit interface. Integration tests use
// OkHttp MockWebServer to simulate the real server — I can test success,
// error codes (404, 500), timeouts, and malformed JSON without hitting
// any real server."
// =============================================================================

interface PlacesApiService {

    @GET("places/search")
    suspend fun searchPlaces(
        @Query("query") query: String,
        @Query("lat") latitude: Double,
        @Query("lng") longitude: Double,
        @Query("radius") radiusKm: Double = 10.0
    ): Response<PlacesSearchResponse>

    @GET("places/details")
    suspend fun getPlaceDetails(
        @Query("place_id") placeId: String
    ): Response<PlaceDetailsResponse>

    @GET("places/nearby")
    suspend fun getNearbyPlaces(
        @Query("lat") latitude: Double,
        @Query("lng") longitude: Double,
        @Query("category") category: String? = null,
        @Query("radius") radiusKm: Double = 5.0
    ): Response<PlacesSearchResponse>
}

// =============================================================================
// API RESPONSE MODELS
// =============================================================================
// These model the JSON structure returned by the API.
// Keep them separate from domain models — API format can change independently.
// =============================================================================

data class PlacesSearchResponse(
    val results: List<PlaceResult>,
    val status: String,
    val totalResults: Int
)

data class PlaceResult(
    val placeId: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val category: String,
    val rating: Double? = null,
    val isOpen: Boolean? = null
)

data class PlaceDetailsResponse(
    val placeId: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val phoneNumber: String? = null,
    val website: String? = null,
    val openingHours: List<String>? = null,
    val photos: List<String>? = null
)
