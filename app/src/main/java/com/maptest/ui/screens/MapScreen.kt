package com.maptest.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.maptest.BuildConfig
import com.maptest.domain.model.SavedLocation
import com.maptest.ui.TestTags
import com.maptest.ui.components.LocationCard
import com.maptest.ui.components.SearchBar
import com.maptest.ui.viewmodel.MapUiState
import com.maptest.util.PermissionStatus

// Stateless map screen: state in via parameters, events out via lambdas.
// Every assertable element carries a testTag so tests can target it without
// matching by text. The map rendering itself is Google's responsibility;
// tests cover behaviour around it (markers for the right locations, search
// updates the visible set, permission rationales surface as expected).

@Composable
fun MapScreen(
    uiState: MapUiState,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    onLocationSelected: (SavedLocation) -> Unit,
    onToggleFavorite: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag(TestTags.MAP_CONTAINER)
    ) {
        // =====================================================================
        // GOOGLE MAP
        // =====================================================================
        val cameraPositionState = rememberCameraPositionState {
            position = CameraPosition.fromLatLngZoom(
                LatLng(uiState.cameraPosition.latitude, uiState.cameraPosition.longitude),
                uiState.cameraPosition.zoom
            )
        }

        // When MAPS_API_KEY is empty (developer hasn't configured one in
        // local.properties), the GoogleMap composable would still render but
        // show a blank/error tile. We swap in a fallback surface that lists
        // the markers we *would* have shown, so the rest of the app — search,
        // favorites, permission flows, offline banner — stays exercisable on
        // any emulator without API-key setup.
        if (BuildConfig.MAPS_API_KEY.isBlank()) {
            MapFallbackSurface(
                savedLocations = uiState.savedLocations,
                searchResults = uiState.searchResults,
                onLocationSelected = onLocationSelected
            )
        } else {
            GoogleMap(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag(TestTags.MAP_VIEW),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(
                    isMyLocationEnabled = false
                ),
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = true,
                    myLocationButtonEnabled = false
                )
            ) {
                uiState.savedLocations.forEach { location ->
                    Marker(
                        state = MarkerState(
                            position = LatLng(location.latitude, location.longitude)
                        ),
                        title = location.name,
                        snippet = location.address,
                        tag = location.id,
                        onClick = {
                            onLocationSelected(location)
                            true
                        }
                    )
                }

                uiState.searchResults.forEach { location ->
                    Marker(
                        state = MarkerState(
                            position = LatLng(location.latitude, location.longitude)
                        ),
                        title = location.name,
                        snippet = location.address,
                        tag = location.id
                    )
                }
            }
        }

        // =====================================================================
        // SEARCH BAR OVERLAY (on top of map)
        // =====================================================================
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            SearchBar(
                query = searchQuery,
                onQueryChange = onSearchQueryChanged,
                isSearching = uiState.isSearching
            )

            // Search results list
            if (uiState.searchResults.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                        .padding(horizontal = 8.dp)
                        .testTag(TestTags.SEARCH_RESULTS_LIST),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    LazyColumn {
                        itemsIndexed(uiState.searchResults) { index, location ->
                            LocationCard(
                                location = location,
                                onClick = { onLocationSelected(location) },
                                onFavoriteClick = { onToggleFavorite(location.id) },
                                index = index
                            )
                        }
                    }
                }
            }

            // Empty state for search
            if (searchQuery.isNotBlank() &&
                uiState.searchResults.isEmpty() &&
                !uiState.isSearching
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .testTag(TestTags.SEARCH_EMPTY_STATE)
                ) {
                    Text(
                        text = "No results found for \"$searchQuery\"",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        // =====================================================================
        // ERROR BANNER
        // =====================================================================
        uiState.error?.let { error ->
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .testTag(TestTags.ERROR_BANNER)
            ) {
                Text(text = error)
            }
        }

        // =====================================================================
        // PERMISSION RATIONALES
        // =====================================================================
        // Three independent surfaces, any of which can appear based on which
        // permission is in which state. Every state has a uniquely tagged,
        // independently-assertable UI element.
        if (uiState.shouldShowLocationRationale) {
            PermissionRationaleCard(
                testTag = TestTags.PERMISSION_LOCATION_RATIONALE,
                message = "We need your location to show you on the map.",
                modifier = Modifier.align(Alignment.Center)
            )
        } else if (uiState.locationDenied) {
            PermissionRationaleCard(
                testTag = TestTags.PERMISSION_LOCATION_DENIED_CARD,
                message = "Location is off. Turn it on in Settings to see your position.",
                modifier = Modifier.align(Alignment.Center),
                showOpenSettings = true
            )
        }

        if (uiState.postNotificationsPermission == PermissionStatus.SHOULD_SHOW_RATIONALE) {
            PermissionRationaleCard(
                testTag = TestTags.PERMISSION_NOTIFICATIONS_RATIONALE,
                message = "Enable notifications to get navigation and arrival alerts.",
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        // =====================================================================
        // OFFLINE BANNER
        // =====================================================================
        // Driven by `uiState.isOnline`, which the ViewModel sources from
        // NetworkMonitor. Tests toggle a FakeNetworkMonitor to flip this.
        if (!uiState.isOnline) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .testTag(TestTags.OFFLINE_BANNER),
                color = MaterialTheme.colorScheme.errorContainer
            ) {
                Text(
                    text = "You're offline — showing cached results",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

// =============================================================================
// PERMISSION RATIONALE CARD
// =============================================================================
// Kept in this file (not components/) because it's only used here and the
// testTag routing is screen-specific. Every element the tests assert on has
// its own tag — the outer card uses the caller-supplied tag so multiple
// rationales on the same screen stay independently addressable.
// =============================================================================

@Composable
private fun MapFallbackSurface(
    savedLocations: List<SavedLocation>,
    searchResults: List<SavedLocation>,
    onLocationSelected: (SavedLocation) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag(TestTags.MAP_VIEW),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Map preview unavailable",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Set MAPS_API_KEY in local.properties to enable Google Maps. " +
                        "Showing ${savedLocations.size + searchResults.size} location(s) below.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
            )
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                itemsIndexed(savedLocations + searchResults) { _, location ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .testTag(TestTags.markerTag(location.id))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(text = location.name, style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = "${location.latitude}, ${location.longitude}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            TextButton(onClick = { onLocationSelected(location) }) {
                                Text("Select")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionRationaleCard(
    testTag: String,
    message: String,
    modifier: Modifier = Modifier,
    showOpenSettings: Boolean = false
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .testTag(testTag),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium
            )
            Row(modifier = Modifier.padding(top = 12.dp)) {
                TextButton(
                    onClick = { /* Activity wires real system prompt */ },
                    modifier = Modifier.testTag(TestTags.PERMISSION_ALLOW_BUTTON)
                ) {
                    Text("Allow")
                }
                TextButton(
                    onClick = { /* Dismiss — ViewModel tracks denied state */ },
                    modifier = Modifier.testTag(TestTags.PERMISSION_DENY_BUTTON)
                ) {
                    Text("Not now")
                }
                if (showOpenSettings) {
                    TextButton(
                        onClick = { /* Activity opens app settings */ },
                        modifier = Modifier.testTag(TestTags.PERMISSION_OPEN_SETTINGS_BUTTON)
                    ) {
                        Text("Open Settings")
                    }
                }
            }
        }
    }
}
