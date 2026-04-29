package com.maptest.util

import com.google.common.truth.Truth.assertThat
import com.maptest.domain.model.LocationCategory
import com.maptest.domain.model.SavedLocation
import org.junit.Before
import org.junit.Test

// JVM-only tests covering happy path, edge cases (empty string, single
// char, case sensitivity), boundary conditions (no matches, duplicate
// names), and structural correctness after remove.

class LocationTrieTest {

    private lateinit var trie: LocationTrie

    // Reusable test locations — small, focused, named by purpose.
    private val starbucks = SavedLocation(
        id = "sb", name = "Starbucks", latitude = 30.27, longitude = -97.74,
        address = "100 Congress Ave", category = LocationCategory.RESTAURANT
    )
    private val stateCapitol = SavedLocation(
        id = "sc", name = "State Capitol", latitude = 30.2747, longitude = -97.7404,
        address = "1100 Congress Ave"
    )
    private val stadium = SavedLocation(
        id = "st", name = "Stadium", latitude = 30.28, longitude = -97.73,
        address = "2100 San Jacinto"
    )
    private val zilkerPark = SavedLocation(
        id = "zp", name = "Zilker Park", latitude = 30.2669, longitude = -97.7729,
        address = "2100 Barton Springs Rd"
    )
    private val coffeeShop = SavedLocation(
        id = "cs", name = "Coffee Shop", latitude = 30.26, longitude = -97.75,
        address = "200 S Lamar"
    )

    @Before
    fun setUp() {
        trie = LocationTrie()
    }

    // =========================================================================
    // INSERT + SIZE
    // =========================================================================

    @Test
    fun `empty trie - size is 0 and isEmpty`() {
        assertThat(trie.size()).isEqualTo(0)
        assertThat(trie.isEmpty()).isTrue()
    }

    @Test
    fun `insert single location - size becomes 1`() {
        trie.insert(starbucks)
        assertThat(trie.size()).isEqualTo(1)
        assertThat(trie.isEmpty()).isFalse()
    }

    @Test
    fun `insert multiple locations - size counts distinct names`() {
        trie.insert(starbucks)
        trie.insert(stateCapitol)
        trie.insert(zilkerPark)
        assertThat(trie.size()).isEqualTo(3)
    }

    @Test
    fun `insert duplicate location id - does not add twice`() {
        trie.insert(starbucks)
        trie.insert(starbucks)

        val results = trie.search("Starbucks")
        assertThat(results).hasSize(1)
    }

    @Test
    fun `insert two locations with same name - both stored`() {
        val starbucks2 = starbucks.copy(id = "sb2", address = "Different address")
        trie.insert(starbucks)
        trie.insert(starbucks2)

        // Size is still 1 (one word-end), but both locations are stored.
        assertThat(trie.size()).isEqualTo(1)
        val results = trie.search("Starbucks")
        assertThat(results).hasSize(2)
        assertThat(results.map { it.id }).containsExactly("sb", "sb2")
    }

    // =========================================================================
    // SEARCH (prefix autocomplete — the main feature)
    // =========================================================================

    @Test
    fun `search exact name - returns matching location`() {
        trie.insert(starbucks)
        val results = trie.search("Starbucks")
        assertThat(results).containsExactly(starbucks)
    }

    @Test
    fun `search prefix - returns all locations starting with prefix`() {
        trie.insert(starbucks)
        trie.insert(stateCapitol)
        trie.insert(stadium)
        trie.insert(zilkerPark)

        val results = trie.search("Sta")
        assertThat(results.map { it.id }).containsExactly("sb", "sc", "st")
    }

    @Test
    fun `search single char prefix - returns all matching`() {
        trie.insert(starbucks)
        trie.insert(stateCapitol)
        trie.insert(stadium)
        trie.insert(zilkerPark)
        trie.insert(coffeeShop)

        val results = trie.search("S")
        assertThat(results).hasSize(3) // Starbucks, State Capitol, Stadium
    }

    @Test
    fun `search is case insensitive`() {
        trie.insert(starbucks)

        assertThat(trie.search("starbucks")).hasSize(1)
        assertThat(trie.search("STARBUCKS")).hasSize(1)
        assertThat(trie.search("StarBUCKS")).hasSize(1)
    }

    @Test
    fun `search nonexistent prefix - returns empty list`() {
        trie.insert(starbucks)
        assertThat(trie.search("xyz")).isEmpty()
    }

    @Test
    fun `search empty string - returns all locations`() {
        trie.insert(starbucks)
        trie.insert(zilkerPark)
        trie.insert(coffeeShop)

        val results = trie.search("")
        assertThat(results).hasSize(3)
    }

    @Test
    fun `search on empty trie - returns empty list`() {
        assertThat(trie.search("anything")).isEmpty()
    }

    @Test
    fun `search prefix that is itself a word - returns word plus extensions`() {
        // "State" is not a complete location name, but "State Capitol" has
        // "State" as a prefix. If we also inserted a location named "State",
        // search("state") should return both.
        val stateLoc = starbucks.copy(id = "s", name = "State")
        trie.insert(stateLoc)
        trie.insert(stateCapitol) // "State Capitol"

        val results = trie.search("State")
        assertThat(results).hasSize(2)
    }

    // =========================================================================
    // CONTAINS EXACT
    // =========================================================================

    @Test
    fun `containsExact - present name returns true`() {
        trie.insert(starbucks)
        assertThat(trie.containsExact("Starbucks")).isTrue()
    }

    @Test
    fun `containsExact - absent name returns false`() {
        trie.insert(starbucks)
        assertThat(trie.containsExact("Starbu")).isFalse() // prefix, not exact
    }

    @Test
    fun `containsExact - case insensitive`() {
        trie.insert(starbucks)
        assertThat(trie.containsExact("starbucks")).isTrue()
        assertThat(trie.containsExact("STARBUCKS")).isTrue()
    }

    @Test
    fun `containsExact - empty trie returns false`() {
        assertThat(trie.containsExact("anything")).isFalse()
    }

    // =========================================================================
    // REMOVE
    // =========================================================================

    @Test
    fun `remove existing location - no longer searchable`() {
        trie.insert(starbucks)
        val removed = trie.remove("sb", "Starbucks")

        assertThat(removed).isTrue()
        assertThat(trie.search("Starbucks")).isEmpty()
        assertThat(trie.containsExact("Starbucks")).isFalse()
        assertThat(trie.size()).isEqualTo(0)
    }

    @Test
    fun `remove one of two locations at same name - other remains`() {
        val starbucks2 = starbucks.copy(id = "sb2")
        trie.insert(starbucks)
        trie.insert(starbucks2)

        trie.remove("sb", "Starbucks")

        val results = trie.search("Starbucks")
        assertThat(results).hasSize(1)
        assertThat(results[0].id).isEqualTo("sb2")
        // Word-end is still active because sb2 remains.
        assertThat(trie.containsExact("Starbucks")).isTrue()
        assertThat(trie.size()).isEqualTo(1)
    }

    @Test
    fun `remove nonexistent id - returns false and changes nothing`() {
        trie.insert(starbucks)
        val removed = trie.remove("fake_id", "Starbucks")

        assertThat(removed).isFalse()
        assertThat(trie.search("Starbucks")).hasSize(1)
    }

    @Test
    fun `remove nonexistent name - returns false`() {
        trie.insert(starbucks)
        val removed = trie.remove("sb", "Nonexistent")

        assertThat(removed).isFalse()
    }

    @Test
    fun `remove does not affect other branches`() {
        trie.insert(starbucks)
        trie.insert(stateCapitol)
        trie.insert(zilkerPark)

        trie.remove("sb", "Starbucks")

        assertThat(trie.search("State")).hasSize(1) // State Capitol still there
        assertThat(trie.search("Z")).hasSize(1)     // Zilker still there
        assertThat(trie.size()).isEqualTo(2)
    }

    // =========================================================================
    // CLEAR
    // =========================================================================

    @Test
    fun `clear - resets trie to empty state`() {
        trie.insert(starbucks)
        trie.insert(stateCapitol)
        trie.insert(zilkerPark)

        trie.clear()

        assertThat(trie.size()).isEqualTo(0)
        assertThat(trie.isEmpty()).isTrue()
        assertThat(trie.search("")).isEmpty()
    }

    @Test
    fun `clear then reinsert - works correctly`() {
        trie.insert(starbucks)
        trie.clear()
        trie.insert(zilkerPark)

        assertThat(trie.size()).isEqualTo(1)
        assertThat(trie.search("Z")).containsExactly(zilkerPark)
        assertThat(trie.search("S")).isEmpty()
    }

    // =========================================================================
    // EDGE CASES
    // =========================================================================

    @Test
    fun `location with single character name`() {
        val loc = starbucks.copy(id = "x", name = "X")
        trie.insert(loc)

        assertThat(trie.containsExact("X")).isTrue()
        assertThat(trie.search("X")).hasSize(1)
    }

    @Test
    fun `location with unicode name`() {
        val loc = starbucks.copy(id = "cafe", name = "Café Résumé")
        trie.insert(loc)

        assertThat(trie.search("café")).hasSize(1)
        assertThat(trie.containsExact("café résumé")).isTrue()
    }

    @Test
    fun `location with spaces and special chars`() {
        val loc = starbucks.copy(id = "mc", name = "McDonald's #1234")
        trie.insert(loc)

        assertThat(trie.search("mcdonald's")).hasSize(1)
        assertThat(trie.search("mcdonald's #")).hasSize(1)
    }

    @Test
    fun `many locations - prefix narrows results progressively`() {
        // Insert: Starbucks, State Capitol, Stadium, Coffee Shop, Zilker Park
        listOf(starbucks, stateCapitol, stadium, coffeeShop, zilkerPark)
            .forEach { trie.insert(it) }

        // Progressively narrower prefixes
        assertThat(trie.search("")).hasSize(5)        // everything
        assertThat(trie.search("S")).hasSize(3)        // Star*, Sta*, Stad*
        assertThat(trie.search("St")).hasSize(3)       // same three
        assertThat(trie.search("Sta")).hasSize(3)      // still all three
        assertThat(trie.search("Star")).hasSize(1)     // only Starbucks
        assertThat(trie.search("Stad")).hasSize(1)     // only Stadium
        assertThat(trie.search("State")).hasSize(1)    // only State Capitol
    }
}
