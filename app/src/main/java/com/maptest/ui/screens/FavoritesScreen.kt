package com.maptest.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.maptest.domain.model.SavedLocation
import com.maptest.ui.TestTags
import com.maptest.ui.components.LocationCard

// =============================================================================
// FAVORITES SCREEN
// =============================================================================
// Displays user's favorite locations.
//
// TESTING SCENARIOS:
// 1. Empty state: No favorites → show empty message
// 2. With favorites: Show list of favorites
// 3. Remove favorite: Tap heart icon → location disappears from list
// 4. Tap location: Navigate to map centered on that location
//
// These are common interview scenarios:
// "Write a test that verifies removing a favorite updates the list."
// =============================================================================

@Composable
fun FavoritesScreen(
    favorites: List<SavedLocation>,
    onLocationSelected: (SavedLocation) -> Unit,
    onToggleFavorite: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Header
        Text(
            text = "Favorites",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(16.dp)
        )

        if (favorites.isEmpty()) {
            // =================================================================
            // EMPTY STATE
            // =================================================================
            // TESTING: Verify this shows when favorites list is empty
            // composeTestRule.onNodeWithTag(TestTags.FAVORITES_EMPTY_STATE)
            //     .assertIsDisplayed()
            // =================================================================
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag(TestTags.FAVORITES_EMPTY_STATE),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No favorite locations yet",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Tap the heart icon on any location to save it here",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            // =================================================================
            // FAVORITES LIST
            // =================================================================
            LazyColumn(
                modifier = Modifier.testTag(TestTags.FAVORITES_LIST)
            ) {
                itemsIndexed(favorites) { index, location ->
                    LocationCard(
                        location = location,
                        onClick = { onLocationSelected(location) },
                        onFavoriteClick = { onToggleFavorite(location.id) },
                        index = index,
                        modifier = Modifier.testTag(
                            TestTags.favoriteItemTag(location.id)
                        )
                    )
                }
            }
        }
    }
}
