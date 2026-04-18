package com.maptest.framework.pages

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.maptest.ui.MainActivity
import com.maptest.ui.TestTags

// =============================================================================
// FAVORITES PAGE OBJECT
// =============================================================================

class FavoritesPage(
    private val rule: AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>
) {
    private val favoritesList get() = rule.onNodeWithTag(TestTags.FAVORITES_LIST)
    private val emptyState get() = rule.onNodeWithTag(TestTags.FAVORITES_EMPTY_STATE)

    // ACTIONS

    fun tapFavorite(locationId: String): FavoritesPage {
        rule.onNodeWithTag(TestTags.favoriteItemTag(locationId)).performClick()
        rule.waitForIdle()
        return this
    }

    fun removeFavorite(locationId: String): FavoritesPage {
        rule.onNodeWithTag(TestTags.favoriteItemTag(locationId))
            .onChildren()
            .filter(hasTestTag(TestTags.LOCATION_FAVORITE_BUTTON))
            .onFirst()
            .performClick()
        rule.waitForIdle()
        return this
    }

    // ASSERTIONS

    fun assertFavoritesListVisible(): FavoritesPage {
        favoritesList.assertIsDisplayed()
        return this
    }

    fun assertEmptyStateVisible(): FavoritesPage {
        emptyState.assertIsDisplayed()
        return this
    }

    fun assertFavoriteExists(locationId: String): FavoritesPage {
        rule.onNodeWithTag(TestTags.favoriteItemTag(locationId)).assertExists()
        return this
    }

    fun assertFavoriteNotExists(locationId: String): FavoritesPage {
        rule.onNodeWithTag(TestTags.favoriteItemTag(locationId)).assertDoesNotExist()
        return this
    }

    fun assertFavoriteCount(count: Int): FavoritesPage {
        if (count == 0) {
            assertEmptyStateVisible()
        } else {
            favoritesList
                .onChildren()
                .assertCountEquals(count)
        }
        return this
    }
}
