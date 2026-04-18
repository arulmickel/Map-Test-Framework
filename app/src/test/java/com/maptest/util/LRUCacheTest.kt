package com.maptest.util

import com.maptest.domain.model.LocationCategory
import com.maptest.domain.model.SavedLocation
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test

// =============================================================================
// LRU CACHE UNIT TESTS
// =============================================================================
// ⭐⭐⭐ THE MOST IMPORTANT TEST FILE FOR YOUR DSA INTERVIEWS ⭐⭐⭐
//
// If an interviewer says "implement an LRU cache and write tests for it,"
// you can walk through this entire file from memory.
//
// TEST STRATEGY:
// 1. Basic operations (get, put, remove)
// 2. Eviction behavior (the core of LRU)
// 3. Access order tracking (what makes it LRU, not FIFO)
// 4. Edge cases (empty cache, single element, null-like scenarios)
// 5. Capacity boundaries (exactly full, overflow, zero capacity)
// 6. Domain-specific tests (nearest location, cache ordering)
//
// EVERY test follows Arrange-Act-Assert. EVERY test is independent.
// EVERY test name reads like a specification.
// =============================================================================

class LRUCacheTest {

    private lateinit var cache: LRULocationCache

    // Helper to create test locations quickly
    private fun location(
        id: String,
        name: String = "Location $id",
        lat: Double = 30.0,
        lng: Double = -97.0
    ) = SavedLocation(
        id = id,
        name = name,
        latitude = lat,
        longitude = lng,
        address = "$id Test St"
    )

    @Before
    fun setUp() {
        // Default cache with capacity 3 — small enough to test eviction easily
        cache = LRULocationCache(capacity = 3)
    }

    // =========================================================================
    // BASIC OPERATIONS
    // =========================================================================

    @Test
    fun `put and get - stores and retrieves a location`() {
        // ARRANGE:
        val loc = location("1")

        // ACT:
        cache.put("1", loc)
        val result = cache.get("1")

        // ASSERT:
        assertThat(result).isEqualTo(loc)
    }

    @Test
    fun `get - returns null for missing key`() {
        val result = cache.get("nonexistent")
        assertThat(result).isNull()
    }

    @Test
    fun `put - overwrites existing key with new value`() {
        // ARRANGE:
        val original = location("1", name = "Original")
        val updated = location("1", name = "Updated")

        // ACT:
        cache.put("1", original)
        cache.put("1", updated)

        // ASSERT:
        assertThat(cache.get("1")?.name).isEqualTo("Updated")
        assertThat(cache.currentSize()).isEqualTo(1) // No duplicate
    }

    @Test
    fun `remove - removes existing key and returns true`() {
        cache.put("1", location("1"))

        val removed = cache.remove("1")

        assertThat(removed).isTrue()
        assertThat(cache.get("1")).isNull()
        assertThat(cache.currentSize()).isEqualTo(0)
    }

    @Test
    fun `remove - returns false for missing key`() {
        val removed = cache.remove("nonexistent")
        assertThat(removed).isFalse()
    }

    // =========================================================================
    // EVICTION BEHAVIOR (the core of LRU)
    // =========================================================================
    // This is what interviewers care about most. LRU means:
    // "When the cache is full and we add a new item, REMOVE the item
    // that was accessed LEAST RECENTLY."
    // =========================================================================

    @Test
    fun `eviction - removes least recently used when full`() {
        // ARRANGE: Fill cache to capacity (3)
        cache.put("1", location("1"))  // Oldest
        cache.put("2", location("2"))
        cache.put("3", location("3"))  // Newest

        // ACT: Add a 4th item — should evict "1" (least recently used)
        cache.put("4", location("4"))

        // ASSERT:
        assertThat(cache.get("1")).isNull()     // Evicted!
        assertThat(cache.get("2")).isNotNull()   // Still here
        assertThat(cache.get("3")).isNotNull()   // Still here
        assertThat(cache.get("4")).isNotNull()   // Newly added
        assertThat(cache.currentSize()).isEqualTo(3)
    }

    @Test
    fun `eviction - get refreshes access order`() {
        // ARRANGE: Fill cache
        cache.put("1", location("1"))
        cache.put("2", location("2"))
        cache.put("3", location("3"))

        // ACT: Access "1" — it's now the MOST recently used
        cache.get("1")

        // Add "4" — should evict "2" (now the least recently used)
        cache.put("4", location("4"))

        // ASSERT:
        assertThat(cache.get("1")).isNotNull()   // Refreshed — survived
        assertThat(cache.get("2")).isNull()       // Evicted!
        assertThat(cache.get("3")).isNotNull()
        assertThat(cache.get("4")).isNotNull()
    }

    @Test
    fun `eviction - put on existing key refreshes access order`() {
        // ARRANGE: Fill cache
        cache.put("1", location("1"))
        cache.put("2", location("2"))
        cache.put("3", location("3"))

        // ACT: Update "1" — it's now most recently used
        cache.put("1", location("1", name = "Updated"))

        // Add "4" — should evict "2"
        cache.put("4", location("4"))

        // ASSERT:
        assertThat(cache.get("1")).isNotNull()
        assertThat(cache.get("1")?.name).isEqualTo("Updated")
        assertThat(cache.get("2")).isNull()       // Evicted!
    }

    @Test
    fun `eviction - sequential eviction maintains order`() {
        // ARRANGE: Fill cache
        cache.put("1", location("1"))
        cache.put("2", location("2"))
        cache.put("3", location("3"))

        // ACT: Add 3 more — should evict 1, 2, 3 in order
        cache.put("4", location("4")) // Evicts 1
        cache.put("5", location("5")) // Evicts 2
        cache.put("6", location("6")) // Evicts 3

        // ASSERT:
        assertThat(cache.get("1")).isNull()
        assertThat(cache.get("2")).isNull()
        assertThat(cache.get("3")).isNull()
        assertThat(cache.get("4")).isNotNull()
        assertThat(cache.get("5")).isNotNull()
        assertThat(cache.get("6")).isNotNull()
    }

    // =========================================================================
    // ACCESS ORDER (getAllInOrder)
    // =========================================================================

    @Test
    fun `getAllInOrder - returns most recent first`() {
        cache.put("1", location("1"))
        cache.put("2", location("2"))
        cache.put("3", location("3"))

        val ordered = cache.getAllInOrder()

        assertThat(ordered.map { it.id }).isEqualTo(listOf("3", "2", "1"))
    }

    @Test
    fun `getAllInOrder - reflects access order after get`() {
        cache.put("1", location("1"))
        cache.put("2", location("2"))
        cache.put("3", location("3"))

        // Access "1" — moves it to front
        cache.get("1")

        val ordered = cache.getAllInOrder()

        assertThat(ordered.map { it.id }).isEqualTo(listOf("1", "3", "2"))
    }

    // =========================================================================
    // EDGE CASES
    // =========================================================================

    @Test
    fun `empty cache - get returns null`() {
        assertThat(cache.get("anything")).isNull()
    }

    @Test
    fun `empty cache - isEmpty returns true`() {
        assertThat(cache.isEmpty()).isTrue()
    }

    @Test
    fun `empty cache - getAllInOrder returns empty list`() {
        assertThat(cache.getAllInOrder()).isEmpty()
    }

    @Test
    fun `single element cache - works correctly`() {
        val smallCache = LRULocationCache(capacity = 1)

        smallCache.put("1", location("1"))
        assertThat(smallCache.get("1")).isNotNull()

        // Adding second evicts first
        smallCache.put("2", location("2"))
        assertThat(smallCache.get("1")).isNull()
        assertThat(smallCache.get("2")).isNotNull()
    }

    @Test
    fun `clear - removes all items`() {
        cache.put("1", location("1"))
        cache.put("2", location("2"))

        cache.clear()

        assertThat(cache.isEmpty()).isTrue()
        assertThat(cache.currentSize()).isEqualTo(0)
        assertThat(cache.get("1")).isNull()
    }

    @Test
    fun `isFull - returns true when at capacity`() {
        cache.put("1", location("1"))
        cache.put("2", location("2"))
        assertThat(cache.isFull()).isFalse()

        cache.put("3", location("3"))
        assertThat(cache.isFull()).isTrue()
    }

    @Test
    fun `contains - returns correct boolean`() {
        cache.put("1", location("1"))

        assertThat(cache.contains("1")).isTrue()
        assertThat(cache.contains("2")).isFalse()
    }

    // =========================================================================
    // DOMAIN-SPECIFIC: findNearest
    // =========================================================================
    // This tests the Haversine distance calculation + linear search.
    // DSA: finding minimum in an unsorted collection — O(n) scan.
    // =========================================================================

    @Test
    fun `findNearest - returns closest location`() {
        // Cache: Austin, Dallas, Houston
        cache.put("austin", location("austin", lat = 30.2672, lng = -97.7431))
        cache.put("dallas", location("dallas", lat = 32.7767, lng = -96.7970))
        cache.put("houston", location("houston", lat = 29.7604, lng = -95.3698))

        // Find nearest to San Antonio (29.4241, -98.4936) — Austin is closest
        val nearest = cache.findNearest(29.4241, -98.4936)

        assertThat(nearest?.id).isEqualTo("austin")
    }

    @Test
    fun `findNearest - returns null for empty cache`() {
        val result = cache.findNearest(30.0, -97.0)
        assertThat(result).isNull()
    }

    @Test
    fun `findNearest - works with single item`() {
        cache.put("only", location("only", lat = 30.0, lng = -97.0))

        val result = cache.findNearest(40.0, -80.0)

        assertThat(result?.id).isEqualTo("only")
    }

    // =========================================================================
    // STRESS / PERFORMANCE
    // =========================================================================

    @Test
    fun `largeCache - handles many operations without issues`() {
        val largeCache = LRULocationCache(capacity = 1000)

        // Insert 2000 items (forces 1000 evictions)
        repeat(2000) { i ->
            largeCache.put("loc_$i", location("loc_$i"))
        }

        // Only last 1000 should remain
        assertThat(largeCache.currentSize()).isEqualTo(1000)
        assertThat(largeCache.get("loc_0")).isNull()       // Evicted
        assertThat(largeCache.get("loc_1999")).isNotNull()  // Recent
    }

    @Test
    fun `repeatedAccessSameKey - does not grow cache`() {
        cache.put("1", location("1"))

        repeat(1000) {
            cache.get("1")
            cache.put("1", location("1", name = "Update $it"))
        }

        assertThat(cache.currentSize()).isEqualTo(1)
    }
}
