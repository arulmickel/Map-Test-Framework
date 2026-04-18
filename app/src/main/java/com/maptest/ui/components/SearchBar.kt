package com.maptest.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.maptest.ui.TestTags

// =============================================================================
// SEARCH BAR COMPOSABLE
// =============================================================================
// CRITICAL TESTING CONCEPT: Every interactive element has a testTag.
//
// Modifier.testTag(TestTags.SEARCH_INPUT) ← This is how tests find this element
//
// In a test:
//   composeTestRule.onNodeWithTag(TestTags.SEARCH_INPUT)
//       .performTextInput("coffee shops")
//
// WHY testTag INSTEAD OF contentDescription:
// - testTag: Only for testing. Invisible to users. Stable identifier.
// - contentDescription: For accessibility (screen readers). Changes with locale.
// Using contentDescription as a test selector would break internationalized apps.
//
// INTERVIEW QUESTION: "How do you add testability to Compose UI?"
// ANSWER: "I add testTag modifiers to all interactive and assertable elements.
// Tags come from a shared TestTags object so both production code and tests
// reference the same constants. I also ensure semantic properties like
// contentDescription are set for accessibility, but I don't use them as
// test selectors."
// =============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    isSearching: Boolean,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag(TestTags.SEARCH_BAR)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag(TestTags.SEARCH_INPUT),  // ← TEST HOOK
            placeholder = { Text("Search places...") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search"
                )
            },
            trailingIcon = {
                if (query.isNotBlank()) {
                    IconButton(
                        onClick = { onQueryChange("") },
                        modifier = Modifier.testTag(TestTags.SEARCH_CLEAR_BUTTON) // ← TEST HOOK
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear search"
                        )
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = { focusManager.clearFocus() }
            )
        )

        // Loading indicator during search
        if (isSearching) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag(TestTags.SEARCH_LOADING)  // ← TEST HOOK
            )
        }
    }
}
