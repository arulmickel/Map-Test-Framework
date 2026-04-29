package com.maptest.util

import com.maptest.domain.model.SavedLocation

// O(1) get / put / remove cache backing the location store.
//
// HashMap<Key, Node> for O(1) lookup; doubly-linked list for O(1) reordering
// and eviction. Head = most recently used, tail = LRU candidate.
//
//   Head ←→ Node1 ←→ Node2 ←→ Node3 ←→ Tail
//   ↑ Most recently used              Least recently used ↑

class LRULocationCache(private val capacity: Int) {

    // Stores the key alongside the value so eviction from the tail can
    // also remove the entry from the HashMap.
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

    fun get(key: String): SavedLocation? {
        val node = map[key] ?: return null

        // Move to front — this location was just accessed, so it's now
        // the "most recently used"
        removeNode(node)
        addToFront(node)

        return node.value
    }

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

    fun remove(key: String): Boolean {
        val node = map[key] ?: return false
        removeNode(node)
        map.remove(key)
        size--
        return true
    }

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

    /** All cached locations in MRU → LRU order. */
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
     * Nearest cached location to the given coordinates by Haversine distance.
     * Linear scan O(n); a KD-Tree / R-Tree spatial index would be O(log n)
     * but isn't worth it at typical cache sizes (50-200).
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

    private fun addToFront(node: Node) {
        node.prev = head
        node.next = head.next
        head.next?.prev = node
        head.next = node
    }

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
