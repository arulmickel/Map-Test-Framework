package com.maptest.viewmodel

import com.maptest.data.repository.LocationRepository
import com.maptest.domain.model.SavedLocation
import com.maptest.ui.viewmodel.MapViewModel
import com.maptest.util.FakeNetworkMonitor
import com.maptest.util.FakePermissionChecker
import com.maptest.util.PermissionStatus
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import com.google.common.truth.Truth.assertThat

// =============================================================================
// MAP VIEW MODEL TESTS
// =============================================================================
// Tests the ViewModel's state management logic in isolation.
//
// KEY TESTING CONCEPTS:
// 1. Mock the Repository — ViewModel is tested alone
// 2. Use TestDispatcher — controls coroutine execution
// 3. Test state transitions — verify Loading → Success/Error
//
// INTERVIEW QUESTION: "How do you test ViewModels with coroutines?"
// ANSWER: "I replace the Main dispatcher with a TestDispatcher using
// Dispatchers.setMain(). I mock the Repository with MockK to return
// controlled data. Then I collect StateFlow emissions and assert the
// state transitions match expected behavior."
// =============================================================================

@OptIn(ExperimentalCoroutinesApi::class)
class MapViewModelTest {

    private lateinit var viewModel: MapViewModel
    private lateinit var repository: LocationRepository
    private lateinit var networkMonitor: FakeNetworkMonitor
    private lateinit var permissionChecker: FakePermissionChecker
    private val testDispatcher = UnconfinedTestDispatcher()

    // Test data
    private val testLocations = listOf(
        SavedLocation(
            id = "1", name = "Coffee Shop",
            latitude = 30.27, longitude = -97.74,
            address = "123 Main St"
        ),
        SavedLocation(
            id = "2", name = "Park",
            latitude = 30.28, longitude = -97.75,
            address = "456 Oak Ave",
            isFavorite = true
        )
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        repository = mockk(relaxed = true)

        // Default mock behavior — return test data
        every { repository.getAllLocations() } returns flowOf(testLocations)
        every { repository.getFavorites() } returns flowOf(
            testLocations.filter { it.isFavorite }
        )

        networkMonitor = FakeNetworkMonitor(initialOnline = true)
        permissionChecker = FakePermissionChecker()
        viewModel = MapViewModel(repository, networkMonitor, permissionChecker)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // =========================================================================
    // INITIAL STATE
    // =========================================================================

    @Test
    fun `initial state - loads saved locations`() = runTest {
        val state = viewModel.uiState.value
        assertThat(state.savedLocations).hasSize(2)
    }

    @Test
    fun `initial state - loads favorites`() = runTest {
        val state = viewModel.uiState.value
        assertThat(state.favoriteLocations).hasSize(1)
        assertThat(state.favoriteLocations[0].name).isEqualTo("Park")
    }

    @Test
    fun `initial state - isSearching is false`() {
        assertThat(viewModel.uiState.value.isSearching).isFalse()
    }

    @Test
    fun `initial state - no error`() {
        assertThat(viewModel.uiState.value.error).isNull()
    }

    // =========================================================================
    // SEARCH
    // =========================================================================

    @Test
    fun `onSearchQueryChanged - updates search query`() {
        viewModel.onSearchQueryChanged("coffee")
        assertThat(viewModel.searchQuery.value).isEqualTo("coffee")
    }

    @Test
    fun `onSearchQueryChanged - blank query clears results`() {
        viewModel.onSearchQueryChanged("")
        val state = viewModel.uiState.value
        assertThat(state.searchResults).isEmpty()
        assertThat(state.isSearching).isFalse()
    }

    // =========================================================================
    // LOCATION SELECTION
    // =========================================================================

    @Test
    fun `onLocationSelected - updates selectedLocation`() {
        val location = testLocations[0]

        viewModel.onLocationSelected(location)

        assertThat(viewModel.uiState.value.selectedLocation).isEqualTo(location)
    }

    @Test
    fun `onLocationSelected - updates camera position`() {
        val location = testLocations[0]

        viewModel.onLocationSelected(location)

        val camera = viewModel.uiState.value.cameraPosition
        assertThat(camera.latitude).isEqualTo(location.latitude)
        assertThat(camera.longitude).isEqualTo(location.longitude)
    }

    // =========================================================================
    // FAVORITES
    // =========================================================================

    @Test
    fun `onToggleFavorite - calls repository`() = runTest {
        viewModel.onToggleFavorite("1")
        coVerify { repository.toggleFavorite("1") }
    }

    // =========================================================================
    // SAVE / DELETE
    // =========================================================================

    @Test
    fun `onSaveLocation - calls repository and shows message`() = runTest {
        val location = testLocations[0]

        viewModel.onSaveLocation(location)

        coVerify { repository.saveLocation(location) }
        assertThat(viewModel.uiState.value.userMessage).isEqualTo("Location saved!")
    }

    @Test
    fun `onDeleteLocation - calls repository and clears selection`() = runTest {
        // First select a location
        viewModel.onLocationSelected(testLocations[0])
        assertThat(viewModel.uiState.value.selectedLocation).isNotNull()

        // Then delete it
        viewModel.onDeleteLocation("1")

        coVerify { repository.deleteLocation("1") }
        assertThat(viewModel.uiState.value.selectedLocation).isNull()
    }

    // =========================================================================
    // MESSAGE HANDLING
    // =========================================================================

    @Test
    fun `onMessageShown - clears user message`() = runTest {
        viewModel.onSaveLocation(testLocations[0])
        assertThat(viewModel.uiState.value.userMessage).isNotNull()

        viewModel.onMessageShown()
        assertThat(viewModel.uiState.value.userMessage).isNull()
    }

    @Test
    fun `onErrorDismissed - clears error`() {
        viewModel.onErrorDismissed()
        assertThat(viewModel.uiState.value.error).isNull()
    }

    // =========================================================================
    // NETWORK STATE (wired via FakeNetworkMonitor)
    // =========================================================================
    // These prove the ViewModel actually observes the NetworkMonitor StateFlow.
    // If someone deletes the collector in `init`, these tests fail loudly.

    @Test
    fun `initial state - isOnline mirrors network monitor`() {
        assertThat(viewModel.uiState.value.isOnline).isTrue()
    }

    @Test
    fun `network goes offline - uiState reflects it`() = runTest {
        networkMonitor.setOnline(false)
        assertThat(viewModel.uiState.value.isOnline).isFalse()
    }

    @Test
    fun `network toggles back online - uiState reflects it`() = runTest {
        networkMonitor.setOnline(false)
        networkMonitor.setOnline(true)
        assertThat(viewModel.uiState.value.isOnline).isTrue()
    }

    // =========================================================================
    // PERMISSIONS (wired via FakePermissionChecker)
    // =========================================================================

    @Test
    fun `initial state - permissions default to granted`() {
        val state = viewModel.uiState.value
        assertThat(state.fineLocationPermission).isEqualTo(PermissionStatus.GRANTED)
        assertThat(state.postNotificationsPermission).isEqualTo(PermissionStatus.GRANTED)
        assertThat(state.shouldShowLocationRationale).isFalse()
        assertThat(state.locationDenied).isFalse()
    }

    @Test
    fun `fine location denied - locationDenied flag flips`() = runTest {
        permissionChecker.setFineLocation(PermissionStatus.DENIED)
        assertThat(viewModel.uiState.value.locationDenied).isTrue()
        assertThat(viewModel.uiState.value.shouldShowLocationRationale).isFalse()
    }

    @Test
    fun `fine location rationale - shouldShowRationale flag flips`() = runTest {
        permissionChecker.setFineLocation(PermissionStatus.SHOULD_SHOW_RATIONALE)
        assertThat(viewModel.uiState.value.shouldShowLocationRationale).isTrue()
        assertThat(viewModel.uiState.value.locationDenied).isFalse()
    }

    @Test
    fun `notifications not applicable - notificationsDenied stays false`() = runTest {
        // Simulates API < 33, where POST_NOTIFICATIONS does not exist.
        permissionChecker.setPostNotifications(PermissionStatus.NOT_APPLICABLE)
        assertThat(viewModel.uiState.value.notificationsDenied).isFalse()
    }

    @Test
    fun `flipping one permission does not affect the others`() = runTest {
        permissionChecker.setBackgroundLocation(PermissionStatus.DENIED)

        val state = viewModel.uiState.value
        assertThat(state.backgroundLocationPermission).isEqualTo(PermissionStatus.DENIED)
        assertThat(state.fineLocationPermission).isEqualTo(PermissionStatus.GRANTED)
        assertThat(state.postNotificationsPermission).isEqualTo(PermissionStatus.GRANTED)
    }
}
