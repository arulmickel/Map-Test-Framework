package com.maptest.util

import com.maptest.domain.model.SavedLocation
import java.util.LinkedList
import java.util.PriorityQueue

// Weighted, directed graph for route validation.
//   Nodes = locations (SavedLocation)
//   Edges = routes between locations, weighted by distance (km)
//
// Three traversals are exposed because each answers a different question:
//   BFS      — fewest hops between two locations          O(V + E)
//   DFS      — any path / reachability check              O(V + E)
//   Dijkstra — shortest weighted path                     O((V + E) log V)

class RouteGraph {

    // Adjacency list: locationId → list of (neighborId, distance in km)
    private val adjacencyList = HashMap<String, MutableList<Edge>>()

    // Quick lookup: locationId → SavedLocation
    private val locations = HashMap<String, SavedLocation>()

    data class Edge(val toId: String, val distanceKm: Double)

    /** A computed route: ordered list of locations with total distance. */
    data class Route(
        val path: List<SavedLocation>,
        val totalDistanceKm: Double
    ) {
        val hopCount: Int get() = path.size - 1
        val isValid: Boolean get() = path.size >= 2
    }

    // =========================================================================
    // GRAPH BUILDING
    // =========================================================================

    fun addLocation(location: SavedLocation) {
        locations[location.id] = location
        adjacencyList.getOrPut(location.id) { mutableListOf() }
    }

    /**
     * Add a directed edge from → to. For bidirectional roads, call this
     * twice with the arguments swapped, or use [addBidirectionalRoute].
     */
    fun addRoute(fromId: String, toId: String, distanceKm: Double) {
        adjacencyList.getOrPut(fromId) { mutableListOf() }
            .add(Edge(toId, distanceKm))
    }

    /** Convenience: adds edges in both directions (same distance). */
    fun addBidirectionalRoute(fromId: String, toId: String, distanceKm: Double) {
        addRoute(fromId, toId, distanceKm)
        addRoute(toId, fromId, distanceKm)
    }

    /**
     * Auto-calculate the distance between two already-added locations using
     * Haversine, then create a bidirectional route.
     */
    fun addAutoRoute(fromId: String, toId: String) {
        val from = locations[fromId] ?: return
        val to = locations[toId] ?: return
        val dist = from.distanceTo(to.latitude, to.longitude)
        addBidirectionalRoute(fromId, toId, dist)
    }

    // =========================================================================
    // BFS — fewest hops (unweighted shortest path)
    // =========================================================================
    fun bfs(startId: String, endId: String): Route? {
        if (startId == endId) {
            val loc = locations[startId] ?: return null
            return Route(listOf(loc), 0.0)
        }

        val visited = HashSet<String>()
        val queue = LinkedList<List<String>>() // each item is a path (list of IDs)
        queue.add(listOf(startId))
        visited.add(startId)

        while (queue.isNotEmpty()) {
            val path = queue.poll()
            val currentId = path.last()

            for (edge in adjacencyList[currentId] ?: emptyList()) {
                if (edge.toId in visited) continue
                val newPath = path + edge.toId

                if (edge.toId == endId) {
                    return buildRoute(newPath)
                }

                visited.add(edge.toId)
                queue.add(newPath)
            }
        }

        return null // No path found — nodes are disconnected
    }

    // =========================================================================
    // DFS — find any path (reachability check)
    // =========================================================================
    fun dfs(startId: String, endId: String): Route? {
        val visited = HashSet<String>()
        val path = mutableListOf<String>()

        fun dfsRecursive(currentId: String): Boolean {
            if (currentId == endId) {
                path.add(currentId)
                return true
            }
            visited.add(currentId)
            path.add(currentId)

            for (edge in adjacencyList[currentId] ?: emptyList()) {
                if (edge.toId !in visited && dfsRecursive(edge.toId)) {
                    return true
                }
            }

            path.removeAt(path.lastIndex)
            return false
        }

        return if (dfsRecursive(startId)) buildRoute(path) else null
    }

    // Dijkstra: min-heap keyed by total distance from start. The first time
    // we pop the end node, it's via the shortest path.
    fun dijkstra(startId: String, endId: String): Route? {
        if (startId == endId) {
            val loc = locations[startId] ?: return null
            return Route(listOf(loc), 0.0)
        }

        // dist[nodeId] = shortest known distance from start
        val dist = HashMap<String, Double>()
        // prev[nodeId] = previous node on the shortest path
        val prev = HashMap<String, String>()

        // PQ entries: (distance, nodeId)
        val pq = PriorityQueue<Pair<Double, String>>(compareBy { it.first })

        dist[startId] = 0.0
        pq.add(0.0 to startId)

        while (pq.isNotEmpty()) {
            val (currentDist, currentId) = pq.poll()

            if (currentId == endId) {
                // Reconstruct path
                val path = mutableListOf(endId)
                var node = endId
                while (prev.containsKey(node)) {
                    node = prev[node]!!
                    path.add(0, node)
                }
                return buildRoute(path)
            }

            // Skip stale entries — we may have already found a shorter path.
            if (currentDist > (dist[currentId] ?: Double.MAX_VALUE)) continue

            for (edge in adjacencyList[currentId] ?: emptyList()) {
                val newDist = currentDist + edge.distanceKm
                if (newDist < (dist[edge.toId] ?: Double.MAX_VALUE)) {
                    dist[edge.toId] = newDist
                    prev[edge.toId] = currentId
                    pq.add(newDist to edge.toId)
                }
            }
        }

        return null // No path found
    }

    // =========================================================================
    // UTILITY
    // =========================================================================

    /** All location IDs reachable from startId (BFS-based). */
    fun reachableFrom(startId: String): Set<String> {
        val visited = HashSet<String>()
        val queue = LinkedList<String>()
        queue.add(startId)
        visited.add(startId)

        while (queue.isNotEmpty()) {
            val current = queue.poll()
            for (edge in adjacencyList[current] ?: emptyList()) {
                if (edge.toId !in visited) {
                    visited.add(edge.toId)
                    queue.add(edge.toId)
                }
            }
        }
        return visited
    }

    /** True if every location can reach every other location. */
    fun isFullyConnected(): Boolean {
        if (locations.isEmpty()) return true
        val total = locations.size
        return locations.keys.all { reachableFrom(it).size == total }
    }

    fun getNeighbors(locationId: String): List<Edge> =
        adjacencyList[locationId] ?: emptyList()

    fun locationCount(): Int = locations.size

    fun edgeCount(): Int = adjacencyList.values.sumOf { it.size }

    fun clear() {
        adjacencyList.clear()
        locations.clear()
    }

    // =========================================================================
    // PRIVATE HELPER
    // =========================================================================
    private fun buildRoute(idPath: List<String>): Route {
        val locPath = idPath.mapNotNull { locations[it] }
        if (locPath.size < 2) {
            return Route(locPath, 0.0)
        }
        var totalDist = 0.0
        for (i in 0 until locPath.size - 1) {
            totalDist += locPath[i].distanceTo(locPath[i + 1].latitude, locPath[i + 1].longitude)
        }
        return Route(locPath, totalDist)
    }
}
