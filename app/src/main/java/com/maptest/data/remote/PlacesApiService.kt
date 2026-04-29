package com.maptest.data.remote

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

// Retrofit interface for the places search backend. Unit tests mock this
// interface with MockK; integration tests use MockWebServer so success,
// 4xx/5xx, timeouts, and malformed JSON can be exercised without a real
// server.

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

// API DTOs. Kept separate from domain models so JSON shape changes don't
// ripple into the rest of the app.

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
