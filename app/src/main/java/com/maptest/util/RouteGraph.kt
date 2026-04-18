package com.maptest.util

import com.maptest.domain.model.SavedLocation
import java.util.LinkedList
import java.util.PriorityQueue

// =============================================================================
// ROUTE GRAPH — BFS / DFS / DIJKSTRA for map route validation
// =============================================================================
//
// ⭐ DSA INTERVIEW QUESTION — "Design a routing system for a maps app"
//
// WHAT THIS IS:
// A weighted, directed graph where:
//   - Nodes = locations (SavedLocation)
//   - Edges = routes between locations, weighted by distance (km)
//
// WHY THREE ALGORITHMS:
//
//   BFS (Breadth-First Search):
//     Finds the route with the fewest HOPS (stops). Good for "minimum
//     transfers" in transit, or testing that two locations are reachable.
//     Time: O(V + E)  |  Space: O(V)
//
//   DFS (Depth-First Search):
//     Finds ANY path between two locations. Good for "can I get there at
//     all?" — the reachability check. Also detects cycles.
//     Time: O(V + E)  |  Space: O(V)
//
//   Dijkstra (Shortest Weighted Path):
//     Finds the route with the shortest DISTANCE. This is what Google/Apple
//     Maps actually uses (plus heuristics = A*). Asked in every graph
//     interview.
//     Time: O((V + E) log V)  |  Space: O(V)
//
// HOW THIS MAPS TO APPLE MAPS SDET WORK:
// An SDET wouldn't build the routing engine — the Maps team does that. But
// the SDET VALIDATES routes: "given this graph, does the algorithm return
// the expected path?" That's what the test class proves.
//
// INTERVIEW QUESTION: "How would you test a routing algorithm?"
// ANSWER: "I build a known graph with hand-calculated shortest paths, run
// the algorithm, and assert the returned path and distance match. I also
// test edge cases: disconnected nodes, cycles, zero-weight edges, single
// node, and same start/end. I don't test the algorithm itself — I test that
// the SYSTEM returns correct results for known inputs."
// =============================================================================

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

    // =========================================================================
    // DIJKSTRA — shortest weighted path
    //
    // Uses a min-heap (PriorityQueue) to always expand the closest unvisited
    // node. This guarantees the first time we reach the end node, it's via
    // the shortest total distance.
    //
    // DSA: This is THE graph algorithm to know for maps interviews. Apple
    // Maps uses A* (Dijkstra + a heuristic), but Dijkstra is the foundation.
    // =========================================================================
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
        val start = locations.keys.first()
        return reachableFrom(start).size == locations.size
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
