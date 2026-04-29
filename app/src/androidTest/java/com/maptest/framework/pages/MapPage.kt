package com.maptest.framework.pages

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.maptest.ui.MainActivity
import com.maptest.ui.TestTags

// Page Object for the map screen. Tests call intent-revealing methods like
// searchFor("coffee") / assertSearchResultsVisible() instead of raw Compose
// matchers, so a testTag rename is a one-place change.

class MapPage(
    private val rule: AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>
) {
    // =========================================================================
    // ELEMENT LOCATORS
    // =========================================================================
    // Private — external code should use action methods, not raw locators.
    // This encapsulation is the whole point of POM.
    // =========================================================================
    private val mapContainer get() = rule.onNodeWithTag(TestTags.MAP_CONTAINER)
    private val mapView get() = rule.onNodeWithTag(TestTags.MAP_VIEW)
    private val searchInput get() = rule.onNodeWithTag(TestTags.SEARCH_INPUT)
    private val searchClearButton get() = rule.onNodeWithTag(TestTags.SEARCH_CLEAR_BUTTON)
    private val searchResultsList get() = rule.onNodeWithTag(TestTags.SEARCH_RESULTS_LIST)
    private val searchLoading get() = rule.onNodeWithTag(TestTags.SEARCH_LOADING)
    private val searchEmptyState get() = rule.onNodeWithTag(TestTags.SEARCH_EMPTY_STATE)
    private val errorBanner get() = rule.onNodeWithTag(TestTags.ERROR_BANNER)
    private val offlineBanner get() = rule.onNodeWithTag(TestTags.OFFLINE_BANNER)

    // =========================================================================
    // ACTIONS: What the user can DO on this screen
    // =========================================================================

    /** Type a search query into the search bar */
    fun searchFor(query: String): MapPage {
        searchInput.performTextInput(query)
        rule.waitForIdle()
        return this // Return self for method chaining
    }

    /** Clear the search bar */
    fun clearSearch(): MapPage {
        searchClearButton.performClick()
        rule.waitForIdle()
        return this
    }

    /** Tap a search result by its index */
    fun tapSearchResult(index: Int): MapPage {
        rule.onNodeWithTag(TestTags.searchResultTag(index)).performClick()
        rule.waitForIdle()
        return this
    }

    /** Tap the favorite button on a search result */
    fun tapFavoriteOnResult(index: Int): MapPage {
        rule.onNodeWithTag(TestTags.searchResultTag(index))
            .onChildren()
            .filter(hasTestTag(TestTags.LOCATION_FAVORITE_BUTTON))
            .onFirst()
            .performClick()
        rule.waitForIdle()
        return this
    }

    // =========================================================================
    // ASSERTIONS: What we can VERIFY on this screen
    // =========================================================================

    /** Assert the map screen is displayed */
    fun assertMapDisplayed(): MapPage {
        mapContainer.assertIsDisplayed()
        return this
    }

    /** Assert the map view is visible */
    fun assertMapViewVisible(): MapPage {
        mapView.assertIsDisplayed()
        return this
    }

    /** Assert search results are showing */
    fun assertSearchResultsVisible(): MapPage {
        searchResultsList.assertIsDisplayed()
        return this
    }

    /** Assert search results are NOT showing */
    fun assertSearchResultsHidden(): MapPage {
        searchResultsList.assertDoesNotExist()
        return this
    }

    /** Assert a specific number of search results */
    fun assertSearchResultCount(count: Int): MapPage {
        // Check that result at index count-1 exists but index count doesn't
        if (count > 0) {
            rule.onNodeWithTag(TestTags.searchResultTag(count - 1))
                .assertExists()
        }
        rule.onNodeWithTag(TestTags.searchResultTag(count))
            .assertDoesNotExist()
        return this
    }

    /** Assert the search loading indicator is visible */
    fun assertSearchLoading(): MapPage {
        searchLoading.assertIsDisplayed()
        return this
    }

    /** Assert the "no results" empty state is visible */
    fun assertSearchEmpty(): MapPage {
        searchEmptyState.assertIsDisplayed()
        return this
    }

    /** Assert the error banner is displayed */
    fun assertErrorDisplayed(): MapPage {
        errorBanner.assertIsDisplayed()
        return this
    }

    /** Assert the error banner is not displayed */
    fun assertNoError(): MapPage {
        errorBanner.assertDoesNotExist()
        return this
    }

    /** Assert the offline banner is displayed */
    fun assertOfflineBannerVisible(): MapPage {
        offlineBanner.assertIsDisplayed()
        return this
    }

    /** Assert the offline banner is NOT in the tree */
    fun assertOfflineBannerHidden(): MapPage {
        offlineBanner.assertDoesNotExist()
        return this
    }

    /** Assert the search input has specific text */
    fun assertSearchText(text: String): MapPage {
        searchInput.assertTextEquals(text)
        return this
    }

    /** Assert the search bar is empty */
    fun assertSearchEmpty(isInputEmpty: Boolean = true): MapPage {
        if (isInputEmpty) {
            searchInput.assertTextEquals("")
        }
        return this
    }

    // =========================================================================
    // COMPOUND ACTIONS: Multi-step flows commonly used in tests
    // =========================================================================

    /** Search and tap the first result */
    fun searchAndSelectFirst(query: String): MapPage {
        return searchFor(query)
            .also { assertSearchResultsVisible() }
            .tapSearchResult(0)
    }

    /** Search and verify no results */
    fun searchAndVerifyEmpty(query: String): MapPage {
        return searchFor(query)
            .also { assertSearchEmpty() }
    }
}
