package com.maptest.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maptest.data.repository.LocationRepository
import com.maptest.domain.model.SavedLocation
import com.maptest.util.NetworkMonitor
import com.maptest.util.PermissionChecker
import com.maptest.util.PermissionStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// =============================================================================
// MAP VIEW MODEL
// =============================================================================
// This ViewModel manages ALL state for the map screens using
// Unidirectional Data Flow (UDF).
//
// WHY UDF:
// State flows in ONE direction: ViewModel → UI (via StateFlow)
// Events flow in ONE direction: UI → ViewModel (via function calls)
// This makes state predictable, testable, and debuggable.
//
//   ┌──────────┐    StateFlow    ┌──────┐
//   │ ViewModel │ ──────────────→│  UI  │
//   │           │←───────────────│      │
//   └──────────┘    Events       └──────┘
//
// WHY StateFlow (not LiveData):
// - Works with coroutines (consistent async model)
// - Has an initial value (no null states to handle)
// - Can be tested with Turbine library
// - Not lifecycle-aware (ViewModel handles lifecycle via viewModelScope)
//
// TESTING APPROACH:
// 1. Create ViewModel with a FakeRepository
// 2. Call a function (e.g., searchPlaces("coffee"))
// 3. Collect StateFlow emissions with Turbine
// 4. Assert state transitions: Loading → Success(results)
//
// INTERVIEW QUESTION: "How do you test ViewModels?"
// ANSWER: "I inject a FakeRepository, call ViewModel functions, and use
// Turbine to assert on StateFlow emissions. I test the full state machine:
// initial state → loading → success/error."
// =============================================================================

@HiltViewModel
class MapViewModel @Inject constructor(
    private val repository: LocationRepository,
    private val networkMonitor: NetworkMonitor,
    private val permissionChecker: PermissionChecker
) : ViewModel() {

    // =========================================================================
    // UI STATE
    // =========================================================================
    // Single state object that represents everything the UI needs to display.
    // The UI simply observes this and renders accordingly.
    // =========================================================================
    private val _uiState = MutableStateFlow(
        // Seed with the monitor's current value so the UI renders the correct
        // banner on the very first frame — no flash of "online" before the
        // StateFlow collector below delivers the real value.
        MapUiState(
            isOnline = networkMonitor.isCurrentlyOnline(),
            fineLocationPermission = permissionChecker.fineLocation.value,
            backgroundLocationPermission = permissionChecker.backgroundLocation.value,
            postNotificationsPermission = permissionChecker.postNotifications.value
        )
    )
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    // Search query — separate flow for debouncing
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    init {
        // Observe network state — drives the offline banner in the UI.
        // StateFlow is hot, so this collector will receive the current value
        // immediately and then every subsequent change.
        viewModelScope.launch {
            networkMonitor.isOnline.collect { online ->
                _uiState.update { it.copy(isOnline = online) }
            }
        }

        // Observe each runtime permission independently. Three collectors
        // instead of a single `combine` so tests can verify that toggling
        // one permission does not clobber the state of the others.
        viewModelScope.launch {
            permissionChecker.fineLocation.collect { status ->
                _uiState.update { it.copy(fineLocationPermission = status) }
            }
        }
        viewModelScope.launch {
            permissionChecker.backgroundLocation.collect { status ->
                _uiState.update { it.copy(backgroundLocationPermission = status) }
            }
        }
        viewModelScope.launch {
            permissionChecker.postNotifications.collect { status ->
                _uiState.update { it.copy(postNotificationsPermission = status) }
            }
        }

        // Observe all saved locations
        viewModelScope.launch {
            repository.getAllLocations().collect { locations ->
                _uiState.update { it.copy(savedLocations = locations) }
            }
        }

        // Observe favorites
        viewModelScope.launch {
            repository.getFavorites().collect { favorites ->
                _uiState.update { it.copy(favoriteLocations = favorites) }
            }
        }

        // Debounced search — waits 300ms after user stops typing before searching
        // DSA: This is a real-world application of the "debounce" pattern.
        // Without it, we'd search on every keystroke = wasteful.
        viewModelScope.launch {
            _searchQuery
                .debounce(300)
                .filter { it.isNotBlank() }
                .collectLatest { query ->
                    performSearch(query)
                }
        }
    }

    // =========================================================================
    // USER ACTIONS (Events from UI → ViewModel)
    // =========================================================================

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            _uiState.update { it.copy(searchResults = emptyList(), isSearching = false) }
        }
    }

    fun onLocationSelected(location: SavedLocation) {
        _uiState.update {
            it.copy(
                selectedLocation = location,
                cameraPosition = CameraPosition(location.latitude, location.longitude, 15f)
            )
        }
    }

    fun onToggleFavorite(locationId: String) {
        viewModelScope.launch {
            repository.toggleFavorite(locationId)
        }
    }

    fun onSaveLocation(location: SavedLocation) {
        viewModelScope.launch {
            repository.saveLocation(location)
            _uiState.update {
                it.copy(userMessage = "Location saved!")
            }
        }
    }

    fun onDeleteLocation(locationId: String) {
        viewModelScope.launch {
            repository.deleteLocation(locationId)
            _uiState.update {
                it.copy(
                    selectedLocation = null,
                    userMessage = "Location deleted"
                )
            }
        }
    }

    fun onMapMoved(latitude: Double, longitude: Double, zoom: Float) {
        _uiState.update {
            it.copy(cameraPosition = CameraPosition(latitude, longitude, zoom))
        }
    }

    fun onMessageShown() {
        _uiState.update { it.copy(userMessage = null) }
    }

    fun onErrorDismissed() {
        _uiState.update { it.copy(error = null) }
    }

    // =========================================================================
    // PERMISSIONS
    // =========================================================================
    // Called by the Activity after returning from a system permission dialog
    // or the app-settings screen. The Real checker re-reads ContextCompat;
    // the Fake is a no-op (tests drive state via setFineLocation etc.).
    fun onPermissionResultReturned() {
        permissionChecker.refresh()
    }

    // =========================================================================
    // PRIVATE: Search implementation
    // =========================================================================
    private suspend fun performSearch(query: String) {
        _uiState.update { it.copy(isSearching = true, error = null) }

        val currentPosition = _uiState.value.cameraPosition
        val result = repository.searchPlaces(
            query = query,
            latitude = currentPosition.latitude,
            longitude = currentPosition.longitude
        )

        result.onSuccess { locations ->
            _uiState.update {
                it.copy(searchResults = locations, isSearching = false)
            }
        }.onFailure { error ->
            // On network failure, try local search
            _uiState.update {
                it.copy(
                    isSearching = false,
                    error = "Search failed: ${error.message}. Showing cached results."
                )
            }

            // Fall back to local search
            repository.searchLocations(query).collect { localResults ->
                _uiState.update { it.copy(searchResults = localResults) }
            }
        }
    }
}

// =============================================================================
// UI STATE DATA CLASS
// =============================================================================
// Everything the UI needs in one immutable object.
// When anything changes, we create a NEW state (copy()).
// This makes state changes trackable and testable.
// =============================================================================

data class MapUiState(
    val savedLocations: List<SavedLocation> = emptyList(),
    val favoriteLocations: List<SavedLocation> = emptyList(),
    val searchResults: List<SavedLocation> = emptyList(),
    val selectedLocation: SavedLocation? = null,
    val cameraPosition: CameraPosition = CameraPosition(),
    val isSearching: Boolean = false,
    val isOnline: Boolean = true,
    val fineLocationPermission: PermissionStatus = PermissionStatus.GRANTED,
    val backgroundLocationPermission: PermissionStatus = PermissionStatus.GRANTED,
    val postNotificationsPermission: PermissionStatus = PermissionStatus.GRANTED,
    val error: String? = null,
    val userMessage: String? = null
) {
    /** True when the user needs to see an in-app rationale for fine location. */
    val shouldShowLocationRationale: Boolean
        get() = fineLocationPermission == PermissionStatus.SHOULD_SHOW_RATIONALE

    /** True when fine location has been denied outright (not rationale, not granted). */
    val locationDenied: Boolean
        get() = fineLocationPermission == PermissionStatus.DENIED

    /** True when notifications have been denied on API 33+. NOT_APPLICABLE → false. */
    val notificationsDenied: Boolean
        get() = postNotificationsPermission == PermissionStatus.DENIED ||
            postNotificationsPermission == PermissionStatus.SHOULD_SHOW_RATIONALE
}

data class CameraPosition(
    val latitude: Double = 30.2672,   // Austin, TX (where the job is!)
    val longitude: Double = -97.7431,
    val zoom: Float = 12f
)
