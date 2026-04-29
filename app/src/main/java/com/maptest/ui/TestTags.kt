package com.maptest.ui

// Centralized Compose test-tag constants. Both Composables and tests
// reference the same constant, so renaming a tag is a one-place change.
// Match by tag, not by text — text changes with localization.

object TestTags {
    // =========================================================================
    // MAP SCREEN
    // =========================================================================
    const val MAP_CONTAINER = "map_container"
    const val MAP_VIEW = "map_view"
    const val MAP_MARKER = "map_marker"
    const val MAP_MARKER_PREFIX = "map_marker_"  // Append location ID
    const val MAP_MY_LOCATION_BUTTON = "map_my_location_button"
    const val MAP_ZOOM_IN = "map_zoom_in"
    const val MAP_ZOOM_OUT = "map_zoom_out"

    // =========================================================================
    // SEARCH
    // =========================================================================
    const val SEARCH_BAR = "search_bar"
    const val SEARCH_INPUT = "search_input"
    const val SEARCH_CLEAR_BUTTON = "search_clear_button"
    const val SEARCH_RESULTS_LIST = "search_results_list"
    const val SEARCH_RESULT_ITEM = "search_result_item"
    const val SEARCH_RESULT_PREFIX = "search_result_" // Append index
    const val SEARCH_LOADING = "search_loading"
    const val SEARCH_EMPTY_STATE = "search_empty_state"
    const val SEARCH_ERROR = "search_error"

    // =========================================================================
    // LOCATION DETAILS
    // =========================================================================
    const val LOCATION_DETAIL_SHEET = "location_detail_sheet"
    const val LOCATION_NAME = "location_name"
    const val LOCATION_ADDRESS = "location_address"
    const val LOCATION_CATEGORY = "location_category"
    const val LOCATION_DISTANCE = "location_distance"
    const val LOCATION_FAVORITE_BUTTON = "location_favorite_button"
    const val LOCATION_SAVE_BUTTON = "location_save_button"
    const val LOCATION_DELETE_BUTTON = "location_delete_button"
    const val LOCATION_NAVIGATE_BUTTON = "location_navigate_button"

    // =========================================================================
    // FAVORITES SCREEN
    // =========================================================================
    const val FAVORITES_LIST = "favorites_list"
    const val FAVORITES_EMPTY_STATE = "favorites_empty_state"
    const val FAVORITES_ITEM_PREFIX = "favorites_item_" // Append location ID

    // =========================================================================
    // NAVIGATION
    // =========================================================================
    const val NAV_BAR = "navigation_bar"
    const val NAV_MAP_TAB = "nav_map_tab"
    const val NAV_FAVORITES_TAB = "nav_favorites_tab"

    // =========================================================================
    // COMMON
    // =========================================================================
    const val LOADING_INDICATOR = "loading_indicator"
    const val ERROR_BANNER = "error_banner"
    const val ERROR_RETRY_BUTTON = "error_retry_button"
    const val SNACKBAR = "snackbar"
    const val PERMISSION_DIALOG = "permission_dialog"
    const val PERMISSION_ALLOW_BUTTON = "permission_allow_button"
    const val PERMISSION_DENY_BUTTON = "permission_deny_button"
    const val PERMISSION_LOCATION_RATIONALE = "permission_location_rationale"
    const val PERMISSION_LOCATION_DENIED_CARD = "permission_location_denied_card"
    const val PERMISSION_NOTIFICATIONS_RATIONALE = "permission_notifications_rationale"
    const val PERMISSION_OPEN_SETTINGS_BUTTON = "permission_open_settings_button"
    const val OFFLINE_BANNER = "offline_banner"

    // =========================================================================
    // HELPER: Generate dynamic tags for list items
    // =========================================================================
    fun markerTag(locationId: String) = "$MAP_MARKER_PREFIX$locationId"
    fun searchResultTag(index: Int) = "$SEARCH_RESULT_PREFIX$index"
    fun favoriteItemTag(locationId: String) = "$FAVORITES_ITEM_PREFIX$locationId"
}
