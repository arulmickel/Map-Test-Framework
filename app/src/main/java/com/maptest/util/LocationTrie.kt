package com.maptest.util

import com.maptest.domain.model.SavedLocation

// =============================================================================
// TRIE (PREFIX TREE) FOR LOCATION AUTOCOMPLETE
// =============================================================================
//
// ⭐ CLASSIC DSA INTERVIEW QUESTION — "Design an autocomplete system"
//
// WHAT IS A TRIE:
// Think of it like a dictionary where every letter is a node. To spell
// "CAT" you walk C → A → T. Once you're at a node, you can see all words
// that start with the letters you've typed so far.
//
//           (root)
//          /      \
//         c        p
//        / \        \
//       a   o        a
//      /     \        \
//     t*      f*       r*
//     |       |        |
//     s*      f*       k*
//             |
//             e*
//             |
//             e*
//
//   (* = a word ends here)
//   Searching "co" → finds "coffee" and "coff…"
//   Searching "ca" → finds "cat" and "cats"
//
// WHY A TRIE FOR MAPS:
// When the user types "Sta" in the search bar, we need to instantly suggest
// "Starbucks", "State Capitol", "Stadium" — all locations whose name starts
// with the typed prefix. A Trie does this in O(prefix length + results)
// time, no matter how many total locations we have.
//
// COMPARISON WITH SQL LIKE:
//   SQL LIKE '%query%' → scans every row, O(n * m). Matches substrings.
//   Trie prefix search → walks a tree, O(k + results). Matches prefixes.
//   The Trie is faster for prefix autocomplete; LIKE is better for general
//   substring search. A production app uses both:
//     - Trie for the dropdown while the user is typing (fast, prefix)
//     - LIKE for the full search after they press Enter (thorough, substring)
//
// TIME COMPLEXITY:
//   insert(word):         O(word.length)
//   search(prefix):       O(prefix.length + number of results)
//   containsExact(word):  O(word.length)
//   remove(word):         O(word.length)
//
// SPACE COMPLEXITY: O(total characters across all words)
//
// INTERVIEW QUESTION: "How would you build an autocomplete for a search bar?"
// ANSWER: "I'd use a Trie populated with location names. As the user types,
// I search the Trie for the current prefix, which gives me all matching
// location names in O(k) time where k is the prefix length. I'd also store
// a frequency or recency counter on each terminal node to rank suggestions.
// For fuzzy matching (typo tolerance), I'd extend it with an edit-distance
// walk or use a BK-Tree alongside."
// =============================================================================

class LocationTrie {

    // =========================================================================
    // TRIE NODE
    // =========================================================================
    // Each node represents one character in the tree. `children` maps the
    // next character to its child node. `locations` holds all SavedLocations
    // whose name ends (or passes through) this node when it's a word-end.
    //
    // WHY a MutableList of SavedLocation (not just a Boolean flag)?
    // Multiple locations can share the same name — there are 15,000+
    // Starbucks in the US alone. We store all of them so the caller can
    // rank by distance, recency, or favorites.
    // =========================================================================
    private class TrieNode {
        val children = HashMap<Char, TrieNode>()
        val locations = mutableListOf<SavedLocation>()
        var isEndOfWord = false
    }

    private val root = TrieNode()
    private var wordCount = 0

    // =========================================================================
    // INSERT — add a location to the trie
    // Time: O(name.length)
    // =========================================================================
    fun insert(location: SavedLocation) {
        val key = location.name.lowercase()
        var current = root
        for (char in key) {
            current = current.children.getOrPut(char) { TrieNode() }
        }
        // Only increment count if this is a genuinely new word-end, or a
        // new location at an existing word-end.
        if (!current.isEndOfWord) wordCount++
        current.isEndOfWord = true
        // Avoid duplicates: don't add the same location ID twice.
        if (current.locations.none { it.id == location.id }) {
            current.locations.add(location)
        }
    }

    // =========================================================================
    // SEARCH BY PREFIX — the core autocomplete operation
    // Returns all locations whose name starts with `prefix`.
    // Time: O(prefix.length + total characters in matching subtree)
    // =========================================================================
    fun search(prefix: String): List<SavedLocation> {
        val key = prefix.lowercase()
        var current = root
        // Walk down to the node representing the last char of the prefix.
        for (char in key) {
            current = current.children[char] ?: return emptyList()
        }
        // Collect every location in the subtree rooted at `current`.
        val results = mutableListOf<SavedLocation>()
        collectAll(current, results)
        return results
    }

    // =========================================================================
    // CONTAINS EXACT — does an exact location name exist in the trie?
    // Time: O(name.length)
    // =========================================================================
    fun containsExact(name: String): Boolean {
        val key = name.lowercase()
        var current = root
        for (char in key) {
            current = current.children[char] ?: return false
        }
        return current.isEndOfWord
    }

    // =========================================================================
    // REMOVE — delete a specific location from the trie
    // Removes the location from the terminal node's list. If no locations
    // remain at that node, clears isEndOfWord. Does NOT prune empty
    // intermediate nodes — a production impl could, but the added complexity
    // isn't worth it for typical cache sizes.
    // Time: O(name.length)
    // =========================================================================
    fun remove(locationId: String, name: String): Boolean {
        val key = name.lowercase()
        var current = root
        for (char in key) {
            current = current.children[char] ?: return false
        }
        if (!current.isEndOfWord) return false

        val removed = current.locations.removeAll { it.id == locationId }
        if (current.locations.isEmpty()) {
            current.isEndOfWord = false
            wordCount--
        }
        return removed
    }

    // =========================================================================
    // UTILITY
    // =========================================================================

    /** Number of distinct word-ends (not total locations — duplicates share a node). */
    fun size(): Int = wordCount

    fun isEmpty(): Boolean = wordCount == 0

    fun clear() {
        root.children.clear()
        wordCount = 0
    }

    // =========================================================================
    // PRIVATE: DFS traversal to collect all locations in a subtree
    // =========================================================================
    private fun collectAll(node: TrieNode, results: MutableList<SavedLocation>) {
        if (node.isEndOfWord) {
            results.addAll(node.locations)
        }
        for (child in node.children.values) {
            collectAll(child, results)
        }
    }
}
