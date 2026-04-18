package com.maptest.util

import com.google.common.truth.Truth.assertThat
import com.maptest.domain.model.SavedLocation
import org.junit.Before
import org.junit.Test

// =============================================================================
// ROUTE GRAPH UNIT TESTS
// =============================================================================
// ⭐ DSA TEST FILE — validates BFS, DFS, and Dijkstra on known graphs.
//
// Test strategy: we build small, hand-traceable graphs, run each algorithm,
// and assert the returned path and distance. Every test could be drawn on
// a whiteboard in an interview.
//
// GRAPH USED IN MOST TESTS:
//
//     A ——2——> B ——3——> D
//     |        |        ↑
//     4        1        |
//     ↓        ↓        2
//     C ——5——> E ——2——> F
//
//   Shortest path A→D:
//     By hops (BFS):     A→B→D          (2 hops)
//     By distance (Dij): A→B→E→F→D      (2+1+2+2 = 7) vs A→B→D (2+3 = 5)
//                         → A→B→D wins at distance 5
//
// INTERVIEW QUESTION: "Walk me through how you'd validate a routing algorithm."
// ANSWER: This file.
// =============================================================================

class RouteGraphTest {

    private lateinit var graph: RouteGraph

    // Locations with realistic Austin coordinates. The exact lat/lng don't
    // matter for these tests — we set explicit edge weights. But Haversine
    // is used when building Routes, so we keep them distinct.
    private val locA = loc("A", "Node A", 30.270, -97.740)
    private val locB = loc("B", "Node B", 30.275, -97.735)
    private val locC = loc("C", "Node C", 30.265, -97.745)
    private val locD = loc("D", "Node D", 30.280, -97.730)
    private val locE = loc("E", "Node E", 30.260, -97.740)
    private val locF = loc("F", "Node F", 30.255, -97.735)

    @Before
    fun setUp() {
        graph = RouteGraph()
    }

    /** Shorthand to avoid repeating SavedLocation boilerplate. */
    private fun loc(id: String, name: String, lat: Double, lng: Double) =
        SavedLocation(id = id, name = name, latitude = lat, longitude = lng)

    /** Builds the standard test graph from the diagram above. */
    private fun buildStandardGraph() {
        listOf(locA, locB, locC, locD, locE, locF).forEach { graph.addLocation(it) }
        graph.addRoute("A", "B", 2.0)
        graph.addRoute("A", "C", 4.0)
        graph.addRoute("B", "D", 3.0)
        graph.addRoute("B", "E", 1.0)
        graph.addRoute("C", "E", 5.0)
        graph.addRoute("E", "F", 2.0)
        graph.addRoute("F", "D", 2.0)
    }

    // =========================================================================
    // GRAPH BUILDING
    // =========================================================================

    @Test
    fun `empty graph - counts are zero`() {
        assertThat(graph.locationCount()).isEqualTo(0)
        assertThat(graph.edgeCount()).isEqualTo(0)
    }

    @Test
    fun `add locations and routes - counts correct`() {
        buildStandardGraph()
        assertThat(graph.locationCount()).isEqualTo(6)
        assertThat(graph.edgeCount()).isEqualTo(7) // 7 directed edges
    }

    @Test
    fun `addBidirectionalRoute - creates two edges`() {
        graph.addLocation(locA)
        graph.addLocation(locB)
        graph.addBidirectionalRoute("A", "B", 2.0)

        assertThat(graph.edgeCount()).isEqualTo(2)
        assertThat(graph.getNeighbors("A")).hasSize(1)
        assertThat(graph.getNeighbors("B")).hasSize(1)
    }

    @Test
    fun `addAutoRoute - calculates Haversine distance`() {
        graph.addLocation(locA)
        graph.addLocation(locD)
        graph.addAutoRoute("A", "D")

        val edges = graph.getNeighbors("A")
        assertThat(edges).hasSize(1)
        assertThat(edges[0].distanceKm).isGreaterThan(0.0)
    }

    @Test
    fun `clear - resets graph`() {
        buildStandardGraph()
        graph.clear()
        assertThat(graph.locationCount()).isEqualTo(0)
        assertThat(graph.edgeCount()).isEqualTo(0)
    }

    // =========================================================================
    // BFS — fewest hops
    // =========================================================================

    @Test
    fun `bfs - finds shortest hop path`() {
        buildStandardGraph()
        val route = graph.bfs("A", "D")

        assertThat(route).isNotNull()
        assertThat(route!!.path.map { it.id }).containsExactly("A", "B", "D").inOrder()
        assertThat(route.hopCount).isEqualTo(2)
    }

    @Test
    fun `bfs - same start and end - returns single node`() {
        graph.addLocation(locA)
        val route = graph.bfs("A", "A")

        assertThat(route).isNotNull()
        assertThat(route!!.path).hasSize(1)
        assertThat(route.totalDistanceKm).isEqualTo(0.0)
    }

    @Test
    fun `bfs - disconnected nodes - returns null`() {
        graph.addLocation(locA)
        graph.addLocation(locB)
        // No edges

        val route = graph.bfs("A", "B")
        assertThat(route).isNull()
    }

    @Test
    fun `bfs - directed edge one way - cannot traverse backwards`() {
        graph.addLocation(locA)
        graph.addLocation(locB)
        graph.addRoute("A", "B", 1.0) // A→B only

        assertThat(graph.bfs("A", "B")).isNotNull()
        assertThat(graph.bfs("B", "A")).isNull()
    }

    // =========================================================================
    // DFS — reachability
    // =========================================================================

    @Test
    fun `dfs - finds a path`() {
        buildStandardGraph()
        val route = graph.dfs("A", "D")

        assertThat(route).isNotNull()
        assertThat(route!!.path.first().id).isEqualTo("A")
        assertThat(route.path.last().id).isEqualTo("D")
        assertThat(route.isValid).isTrue()
    }

    @Test
    fun `dfs - disconnected nodes - returns null`() {
        graph.addLocation(locA)
        graph.addLocation(locB)

        assertThat(graph.dfs("A", "B")).isNull()
    }

    @Test
    fun `dfs - handles cycles without infinite loop`() {
        graph.addLocation(locA)
        graph.addLocation(locB)
        graph.addLocation(locC)
        graph.addBidirectionalRoute("A", "B", 1.0)
        graph.addBidirectionalRoute("B", "C", 1.0)
        graph.addBidirectionalRoute("C", "A", 1.0) // cycle: A↔B↔C↔A

        val route = graph.dfs("A", "C")
        assertThat(route).isNotNull()
        assertThat(route!!.path.last().id).isEqualTo("C")
    }

    // =========================================================================
    // DIJKSTRA — shortest weighted path
    // =========================================================================

    @Test
    fun `dijkstra - finds shortest distance path`() {
        buildStandardGraph()
        val route = graph.dijkstra("A", "D")

        assertThat(route).isNotNull()
        // A→B (2) + B→D (3) = 5 is shorter than A→B→E→F→D (2+1+2+2 = 7)
        assertThat(route!!.path.map { it.id }).containsExactly("A", "B", "D").inOrder()
    }

    @Test
    fun `dijkstra - prefers longer hop path if distance is shorter`() {
        // Build graph where direct edge is expensive but multi-hop is cheap:
        //   A ——10——> C
        //   A ——1——> B ——1——> C
        graph.addLocation(locA)
        graph.addLocation(locB)
        graph.addLocation(locC)
        graph.addRoute("A", "C", 10.0)
        graph.addRoute("A", "B", 1.0)
        graph.addRoute("B", "C", 1.0)

        val route = graph.dijkstra("A", "C")
        assertThat(route).isNotNull()
        assertThat(route!!.path.map { it.id }).containsExactly("A", "B", "C").inOrder()
    }

    @Test
    fun `dijkstra - same start and end - returns zero distance`() {
        graph.addLocation(locA)
        val route = graph.dijkstra("A", "A")

        assertThat(route).isNotNull()
        assertThat(route!!.totalDistanceKm).isEqualTo(0.0)
    }

    @Test
    fun `dijkstra - disconnected nodes - returns null`() {
        graph.addLocation(locA)
        graph.addLocation(locB)

        assertThat(graph.dijkstra("A", "B")).isNull()
    }

    @Test
    fun `dijkstra - zero weight edges - still works`() {
        graph.addLocation(locA)
        graph.addLocation(locB)
        graph.addRoute("A", "B", 0.0)

        val route = graph.dijkstra("A", "B")
        assertThat(route).isNotNull()
        assertThat(route!!.path.map { it.id }).containsExactly("A", "B").inOrder()
    }

    // =========================================================================
    // REACHABILITY / CONNECTIVITY
    // =========================================================================

    @Test
    fun `reachableFrom - returns all reachable nodes`() {
        buildStandardGraph()
        val reachable = graph.reachableFrom("A")

        assertThat(reachable).containsExactly("A", "B", "C", "D", "E", "F")
    }

    @Test
    fun `reachableFrom - isolated node returns only itself`() {
        graph.addLocation(locA)
        assertThat(graph.reachableFrom("A")).containsExactly("A")
    }

    @Test
    fun `isFullyConnected - connected graph returns true`() {
        // Use bidirectional edges so every node can reach every other.
        listOf(locA, locB, locC).forEach { graph.addLocation(it) }
        graph.addBidirectionalRoute("A", "B", 1.0)
        graph.addBidirectionalRoute("B", "C", 1.0)

        assertThat(graph.isFullyConnected()).isTrue()
    }

    @Test
    fun `isFullyConnected - disconnected graph returns false`() {
        buildStandardGraph()
        // Standard graph is directed — D/E/F cannot reach A, so not
        // fully connected by directed reachability.
        assertThat(graph.isFullyConnected()).isFalse()
    }

    @Test
    fun `isFullyConnected - empty graph returns true`() {
        assertThat(graph.isFullyConnected()).isTrue()
    }

    // =========================================================================
    // ROUTE DATA CLASS
    // =========================================================================

    @Test
    fun `route isValid - single node is not valid`() {
        graph.addLocation(locA)
        val route = graph.bfs("A", "A")
        assertThat(route!!.isValid).isFalse()
    }

    @Test
    fun `route isValid - two or more nodes is valid`() {
        graph.addLocation(locA)
        graph.addLocation(locB)
        graph.addRoute("A", "B", 1.0)

        val route = graph.bfs("A", "B")
        assertThat(route!!.isValid).isTrue()
    }

    // =========================================================================
    // COMPLEX SCENARIO — "plan a real route"
    // =========================================================================

    @Test
    fun `complex - Austin landmarks route validation`() {
        // Build a mini Austin map:
        //   Capitol ——> UT Tower ——> Stadium
        //       \                     ↑
        //        ——> Zilker ——> SoCo ——
        val capitol = loc("cap", "Texas State Capitol", 30.2747, -97.7404)
        val utTower = loc("ut", "UT Tower", 30.2862, -97.7394)
        val stadium = loc("sta", "DKR Stadium", 30.2836, -97.7326)
        val zilker = loc("zil", "Zilker Park", 30.2669, -97.7729)
        val soco = loc("soc", "South Congress", 30.2500, -97.7489)

        listOf(capitol, utTower, stadium, zilker, soco).forEach { graph.addLocation(it) }
        graph.addRoute("cap", "ut", 1.5)
        graph.addRoute("ut", "sta", 0.8)
        graph.addRoute("cap", "zil", 3.5)
        graph.addRoute("zil", "soc", 2.0)
        graph.addRoute("soc", "sta", 4.0)

        // BFS: fewest hops Capitol → Stadium
        val bfsRoute = graph.bfs("cap", "sta")
        assertThat(bfsRoute!!.path.map { it.id }).containsExactly("cap", "ut", "sta").inOrder()
        assertThat(bfsRoute.hopCount).isEqualTo(2)

        // Dijkstra: shortest distance Capitol → Stadium
        val dijRoute = graph.dijkstra("cap", "sta")
        assertThat(dijRoute).isNotNull()
        // cap→ut (1.5) + ut→sta (0.8) = 2.3  vs  cap→zil→soc→sta = 9.5
        assertThat(dijRoute!!.path.map { it.id }).containsExactly("cap", "ut", "sta").inOrder()

        // DFS: reachability
        val dfsRoute = graph.dfs("cap", "soc")
        assertThat(dfsRoute).isNotNull()
        assertThat(dfsRoute!!.path.last().id).isEqualTo("soc")
    }
}
