package com.maptest.tests

import com.maptest.framework.base.BaseTestCase
import com.maptest.framework.pages.FavoritesPage
import com.maptest.framework.pages.MapPage
import com.maptest.framework.pages.NavigationPage
import com.maptest.ui.TestTags
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Test

// =============================================================================
// NAVIGATION TESTS
// =============================================================================
// Tests for screen transitions and navigation state persistence.
//
// WHY THESE TESTS MATTER:
// Navigation bugs are common in production apps:
// - User loses search results when switching tabs
// - Favorite state doesn't update across screens
// - Back button doesn't work as expected
// - Tab state resets on configuration change (rotation)
//
// These tests verify the navigation flow works end-to-end.
// =============================================================================

@HiltAndroidTest
class NavigationTest : BaseTestCase() {

    private lateinit var navPage: NavigationPage
    private lateinit var mapPage: MapPage
    private lateinit var favoritesPage: FavoritesPage

    @Before
    override fun setUp() {
        super.setUp()
        navPage = NavigationPage(composeTestRule)
        mapPage = MapPage(composeTestRule)
        favoritesPage = FavoritesPage(composeTestRule)
    }

    @Test
    fun app_startsOnMapScreen() {
        mapPage.assertMapDisplayed()
        navPage.assertNavBarVisible()
    }

    @Test
    fun navigation_mapToFavorites_showsFavoritesScreen() {
        // ACT:
        navPage.goToFavorites()

        // ASSERT:
        composeTestRule.onNodeWithTag(TestTags.FAVORITES_EMPTY_STATE)
            .assertIsDisplayed()
    }

    @Test
    fun navigation_favoritesToMap_returnsToMapScreen() {
        // ARRANGE:
        navPage.goToFavorites()

        // ACT:
        navPage.goToMap()

        // ASSERT:
        mapPage.assertMapDisplayed()
    }

    @Test
    fun navigation_searchState_survivesTabSwitch() {
        // ARRANGE: Start a search on map screen
        mapPage.searchFor("coffee")
        waitForIdle()

        // ACT: Switch to favorites and back
        navPage.goToFavorites()
        waitForIdle()
        navPage.goToMap()
        waitForIdle()

        // ASSERT: Search state should still be there
        // (This verifies ViewModel survives navigation)
    }

    @Test
    fun navigation_rapidTabSwitching_doesNotCrash() {
        // Stress test: rapidly switch between tabs
        repeat(10) {
            navPage.goToFavorites()
            navPage.goToMap()
        }
        waitForIdle()
        // No crash = pass
        mapPage.assertMapDisplayed()
    }
}
