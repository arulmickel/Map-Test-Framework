package com.maptest.framework.pages

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.maptest.ui.MainActivity
import com.maptest.ui.TestTags

// =============================================================================
// NAVIGATION PAGE OBJECT
// =============================================================================
// Handles bottom navigation bar interactions.
// Separated from MapPage/FavoritesPage because navigation is shared.
// =============================================================================

class NavigationPage(
    private val rule: AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>
) {
    private val navBar get() = rule.onNodeWithTag(TestTags.NAV_BAR)
    private val mapTab get() = rule.onNodeWithTag(TestTags.NAV_MAP_TAB)
    private val favoritesTab get() = rule.onNodeWithTag(TestTags.NAV_FAVORITES_TAB)

    fun goToMap(): NavigationPage {
        mapTab.performClick()
        rule.waitForIdle()
        return this
    }

    fun goToFavorites(): NavigationPage {
        favoritesTab.performClick()
        rule.waitForIdle()
        return this
    }

    fun assertNavBarVisible(): NavigationPage {
        navBar.assertIsDisplayed()
        return this
    }

    fun assertMapTabSelected(): NavigationPage {
        mapTab.assertIsSelected()
        return this
    }

    fun assertFavoritesTabSelected(): NavigationPage {
        favoritesTab.assertIsSelected()
        return this
    }
}
