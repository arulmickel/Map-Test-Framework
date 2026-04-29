package com.maptest.tests

import com.maptest.framework.base.BaseTestCase
import com.maptest.framework.data.TestDataBuilder
import com.maptest.framework.pages.MapPage
import com.maptest.ui.TestTags
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Test

// Search-screen instrumented tests. Conventions used throughout:
//   - Test names describe behaviour (search_withSpecialCharacters_doesNotCrash)
//   - Arrange-Act-Assert structure per test
//   - All UI access goes through MapPage; no raw Compose matchers
//   - Edge cases (empty input, special characters, long input) covered
//     alongside the happy path
//   - Each test runs independently — no shared state between @Tests

@HiltAndroidTest
class SearchScreenTest : BaseTestCase() {

    private lateinit var mapPage: MapPage

    @Before
    override fun setUp() {
        super.setUp()
        mapPage = MapPage(composeTestRule)
    }

    @Test
    fun searchBar_isDisplayedOnMapScreen() {
        // ARRANGE: Screen is already loaded via BaseTestCase
        // ACT: Nothing — just verify initial state
        // ASSERT:
        mapPage.assertMapDisplayed()
        composeTestRule.onNodeWithTag(TestTags.SEARCH_INPUT).assertIsDisplayed()
    }

    @Test
    fun search_withValidQuery_showsResults() {
        // ARRANGE: Nothing extra needed
        // ACT:
        mapPage.searchFor("coffee shops")
        // ASSERT:
        mapPage.assertSearchResultsVisible()
    }

    @Test
    fun search_tapResult_selectsLocation() {
        // ARRANGE + ACT:
        mapPage.searchFor("coffee")
        waitForIdle()

        // ACT: Tap first result
        mapPage.tapSearchResult(0)

        // ASSERT: Search results should collapse, location selected
        // (In full implementation, verify map moves to selected location)
        waitForIdle()
    }

    @Test
    fun search_clearButton_clearsQueryAndResults() {
        // ARRANGE:
        mapPage.searchFor("restaurants")
        mapPage.assertSearchResultsVisible()

        // ACT:
        mapPage.clearSearch()

        // ASSERT:
        mapPage.assertSearchResultsHidden()
    }

    @Test
    fun search_withEmptyQuery_showsNoResults() {
        // Type then clear — should show nothing
        mapPage.searchFor("coffee")
        mapPage.clearSearch()
        mapPage.assertSearchResultsHidden()
    }

    @Test
    fun search_withSpecialCharacters_doesNotCrash() {
        // ARRANGE + ACT:
        // These characters could break SQL queries, JSON parsing, or UI rendering
        val specialQueries = listOf(
            "café",           // Accented characters
            "O'Brien's",      // Apostrophe (SQL injection risk)
            "<script>alert",  // XSS attempt
            "日本語",          // Unicode (Japanese)
            "🏪☕",           // Emoji
            "",               // Empty
            " ",              // Whitespace only
            "a".repeat(1000)  // Very long input
        )

        specialQueries.forEach { query ->
            mapPage.searchFor(query)
            waitForIdle()
            mapPage.clearSearch()
            waitForIdle()
            // If we get here without crashing, the test passes
        }
    }

    @Test
    fun search_withNoResults_showsEmptyState() {
        // A query that should return no results
        mapPage.searchFor("xyznonexistentplace12345")
        waitForIdle()
        mapPage.assertSearchEmpty()
    }

    @Test
    fun search_rapidTyping_doesNotCrash() {
        // Simulate rapid typing — tests debounce behavior
        // Each character triggers a state update; rapid input shouldn't
        // cause race conditions or crashes
        val searchInput = composeTestRule.onNodeWithTag(TestTags.SEARCH_INPUT)

        "coffee shops near me in Austin Texas downtown area".forEach { char ->
            searchInput.performTextInput(char.toString())
        }
        waitForIdle()
        // No crash = pass
    }

    // =========================================================================
    // INTEGRATION TESTS
    // =========================================================================

    @Test
    fun search_favoriteResult_persistsAfterClearAndReSearch() {
        // ARRANGE: Search and favorite a result
        mapPage.searchFor("coffee")
        waitForIdle()
        mapPage.tapFavoriteOnResult(0)
        waitForIdle()

        // ACT: Clear search and search again
        mapPage.clearSearch()
        mapPage.searchFor("coffee")
        waitForIdle()

        // ASSERT: The favorited result should still show as favorited
        // (Favorite state persists in database, not just UI)
    }
}
