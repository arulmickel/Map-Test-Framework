package com.maptest.util

import com.maptest.domain.model.SavedLocation

// =============================================================================
// LRU (Least Recently Used) LOCATION CACHE
// =============================================================================
//
// ⭐⭐⭐ THIS IS THE MOST IMPORTANT DSA FILE IN THIS PROJECT ⭐⭐⭐
//
// WHY: LRU Cache is one of the most commonly asked Apple SDET interview
// questions. It appeared in real Glassdoor reports for the SDET Austin role.
// LeetCode #146 — you MUST be able to implement this from scratch.
//
// WHAT IS AN LRU CACHE?
// Imagine you can only remember 5 phone numbers. When you learn a 6th,
// you forget the one you haven't used the longest. That's LRU.
//
// WHY IT'S USED IN MAPS:
// A maps app loads tiles (small square images of the map). You can't keep
// ALL tiles in memory — the phone would run out of RAM. So you keep the
// most recently viewed tiles and evict the oldest ones.
//
// HOW IT WORKS (the DSA part):
// We need TWO operations to be O(1) — constant time:
//   1. GET: Look up a location by key → O(1) with HashMap
//   2. PUT: Add a new location, evict oldest if full → O(1) with Doubly Linked List
//
// HashMap alone: O(1) lookup, but no order information.
// LinkedList alone: O(1) insert/delete, but O(n) lookup.
// Combined: O(1) for everything.
//
// STRUCTURE:
//   HashMap<Key, Node>  →  for O(1) lookup
//   DoublyLinkedList     →  for O(1) insert/remove + order tracking
//
//   Head ←→ Node1 ←→ Node2 ←→ Node3 ←→ Tail
//   ↑ Most recently used              Least recently used ↑
//   (front)                                         (back)
//
// OPERATIONS:
//   GET(key):
//     1. Look up in HashMap → O(1)
//     2. Move node to front of list (most recently used) → O(1)
//     3. Return value
//
//   PUT(key, value):
//     1. If key exists: update value, move to front → O(1)
//     2. If key doesn't exist:
//        a. If cache is full: remove node at tail (LRU) → O(1)
//        b. Create new node, add to front, add to HashMap → O(1)
//
// =============================================================================

class LRULocationCache(private val capacity: Int) {

    // =========================================================================
    // DOUBLY LINKED LIST NODE
    // =========================================================================
    // Each node holds the key-value pair and pointers to prev/next nodes.
    // WHY store the key? When we evict from the tail, we need the key to
    // also remove it from the HashMap.
    // =========================================================================
    private data class Node(
        val key: String,
        var value: SavedLocation,
        var prev: Node? = null,
        var next: Node? = null
    )

    // HashMap for O(1) key → node lookup
    private val map = HashMap<String, Node>()

    // Dummy head and tail nodes — simplifies edge cases
    // (no null checks when adding/removing)
    private val head = Node(key = "HEAD", value = dummyLocation())
    private val tail = Node(key = "TAIL", value = dummyLocation())

    // Current number of items in cache
    private var size = 0

    init {
        // Connect head ←→ tail (empty list)
        head.next = tail
        tail.prev = head
    }

    // =========================================================================
    // GET: Retrieve a location from cache
    // Time: O(1) | Space: O(1)
    // =========================================================================
    fun get(key: String): SavedLocation? {
        val node = map[key] ?: return null

        // Move to front — this location was just accessed, so it's now
        // the "most recently used"
        removeNode(node)
        addToFront(node)

        return node.value
    }

    // =========================================================================
    // PUT: Add or update a location in cache
    // Time: O(1) | Space: O(1)
    // =========================================================================
    fun put(key: String, location: SavedLocation) {
        if (map.containsKey(key)) {
            // Key exists → update value and move to front
            val existingNode = map[key]!!
            existingNode.value = location
            removeNode(existingNode)
            addToFront(existingNode)
        } else {
            // Key doesn't exist → create new node
            if (size == capacity) {
                // Cache is full → evict LRU (node just before tail)
                val lruNode = tail.prev!!
                removeNode(lruNode)
                map.remove(lruNode.key)
                size--
            }

            val newNode = Node(key = key, value = location)
            addToFront(newNode)
            map[key] = newNode
            size++
        }
    }

    // =========================================================================
    // REMOVE: Explicitly remove a location from cache
    // =========================================================================
    fun remove(key: String): Boolean {
        val node = map[key] ?: return false
        removeNode(node)
        map.remove(key)
        size--
        return true
    }

    // =========================================================================
    // UTILITY METHODS
    // =========================================================================

    fun contains(key: String): Boolean = map.containsKey(key)

    fun currentSize(): Int = size

    fun isEmpty(): Boolean = size == 0

    fun isFull(): Boolean = size == capacity

    fun clear() {
        head.next = tail
        tail.prev = head
        map.clear()
        size = 0
    }

    /**
     * Returns all cached locations in order from most recently used to least.
     * Useful for displaying "recent locations" in UI.
     *
     * DSA: This is a linked list traversal — O(n) time, O(n) space for the list.
     */
    fun getAllInOrder(): List<SavedLocation> {
        val result = mutableListOf<SavedLocation>()
        var current = head.next
        while (current != null && current != tail) {
            result.add(current.value)
            current = current.next
        }
        return result
    }

    /**
     * Find the nearest cached location to the given coordinates.
     * Uses the Haversine distance calculation from SavedLocation.
     *
     * DSA: Linear scan O(n). Could be optimized with a KD-Tree for large caches,
     * but for typical cache sizes (50-200), linear scan is fast enough.
     *
     * INTERVIEW FOLLOW-UP: "How would you optimize this for millions of locations?"
     * → "I'd use a KD-Tree or a spatial index like R-Tree for O(log n) nearest
     *    neighbor queries."
     */
    fun findNearest(latitude: Double, longitude: Double): SavedLocation? {
        var nearest: SavedLocation? = null
        var minDistance = Double.MAX_VALUE

        var current = head.next
        while (current != null && current != tail) {
            val distance = current.value.distanceTo(latitude, longitude)
            if (distance < minDistance) {
                minDistance = distance
                nearest = current.value
            }
            current = current.next
        }

        return nearest
    }

    // =========================================================================
    // PRIVATE HELPERS: Linked List Operations
    // =========================================================================

    /**
     * Add a node right after head (most recently used position).
     *
     * Before: head ←→ X ←→ ...
     * After:  head ←→ node ←→ X ←→ ...
     */
    private fun addToFront(node: Node) {
        node.prev = head
        node.next = head.next
        head.next?.prev = node
        head.next = node
    }

    /**
     * Remove a node from its current position in the list.
     *
     * Before: A ←→ node ←→ B
     * After:  A ←→ B      (node is disconnected)
     */
    private fun removeNode(node: Node) {
        node.prev?.next = node.next
        node.next?.prev = node.prev
    }

    private fun dummyLocation() = SavedLocation(
        id = "",
        name = "",
        latitude = 0.0,
        longitude = 0.0
    )
}
