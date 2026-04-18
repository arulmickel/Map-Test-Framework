package com.maptest.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.hilt.navigation.compose.hiltViewModel
import com.maptest.ui.screens.FavoritesScreen
import com.maptest.ui.screens.MapScreen
import com.maptest.ui.viewmodel.MapViewModel
import dagger.hilt.android.AndroidEntryPoint

// =============================================================================
// MAIN ACTIVITY
// =============================================================================
// @AndroidEntryPoint: Tells Hilt to inject dependencies into this Activity.
// In tests, we use @HiltAndroidTest on the test class, which swaps the
// real dependencies with test doubles.
// =============================================================================

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                MapTestApp()
            }
        }
    }
}

@Composable
fun MapTestApp(
    viewModel: MapViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar(
                modifier = Modifier.testTag(TestTags.NAV_BAR)
            ) {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.LocationOn, contentDescription = "Map") },
                    label = { Text("Map") },
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    modifier = Modifier.testTag(TestTags.NAV_MAP_TAB)
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Favorite, contentDescription = "Favorites") },
                    label = { Text("Favorites") },
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    modifier = Modifier.testTag(TestTags.NAV_FAVORITES_TAB)
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (selectedTab) {
                0 -> MapScreen(
                    uiState = uiState,
                    searchQuery = searchQuery,
                    onSearchQueryChanged = viewModel::onSearchQueryChanged,
                    onLocationSelected = viewModel::onLocationSelected,
                    onToggleFavorite = viewModel::onToggleFavorite
                )
                1 -> FavoritesScreen(
                    favorites = uiState.favoriteLocations,
                    onLocationSelected = viewModel::onLocationSelected,
                    onToggleFavorite = viewModel::onToggleFavorite
                )
            }
        }
    }
}
