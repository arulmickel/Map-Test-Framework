package com.maptest.util

import com.maptest.domain.model.SavedLocation

// Prefix tree backing location-name autocomplete.
//
// SQL LIKE '%query%' would also work but scans every row at O(n * m). A trie
// answers prefix queries in O(prefix.length + results), independent of total
// location count, which is what the search-as-you-type dropdown needs.
//
// Time complexity:
//   insert(word):         O(word.length)
//   search(prefix):       O(prefix.length + matching results)
//   containsExact(word):  O(word.length)
//   remove(word):         O(word.length)
// Space: O(total characters across all words).

class LocationTrie {

    // `locations` holds a list (not a flag) because many real locations
    // share the same name (chains like Starbucks); the caller ranks by
    // distance, recency, or favorites.
    private class TrieNode {
        val children = HashMap<Char, TrieNode>()
        val locations = mutableListOf<SavedLocation>()
        var isEndOfWord = false
    }

    private val root = TrieNode()
    private var wordCount = 0

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

    fun containsExact(name: String): Boolean {
        val key = name.lowercase()
        var current = root
        for (char in key) {
            current = current.children[char] ?: return false
        }
        return current.isEndOfWord
    }

    // Does NOT prune empty intermediate nodes; the added complexity isn't
    // worth it for typical cache sizes.
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

    /** Number of distinct word-ends (not total locations — duplicates share a node). */
    fun size(): Int = wordCount

    fun isEmpty(): Boolean = wordCount == 0

    fun clear() {
        root.children.clear()
        wordCount = 0
    }

    private fun collectAll(node: TrieNode, results: MutableList<SavedLocation>) {
        if (node.isEndOfWord) {
            results.addAll(node.locations)
        }
        for (child in node.children.values) {
            collectAll(child, results)
        }
    }
}
