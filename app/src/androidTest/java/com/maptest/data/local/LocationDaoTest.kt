package com.maptest.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.maptest.framework.data.TestDataBuilder
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

// Integration tests against a real Room database (in-memory).
//
// Real SQL means a typo in a @Query or schema drift fails here, where a
// mocked DAO would happily return whatever you told it to. Real Flow
// semantics mean we exercise Room's invalidation tracker. Real
// OnConflictStrategy behaviour means we exercise actual SQLite semantics,
// not Kotlin code paths.
//
// inMemoryDatabaseBuilder: no disk I/O, no leaked state between tests.
// allowMainThreadQueries: tests block on results so a Flow assertion can't
// deadlock waiting for an off-main-thread query. Never used in production.

@RunWith(AndroidJUnit4::class)
class LocationDaoTest {

    private lateinit var database: LocationDatabase
    private lateinit var dao: LocationDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            LocationDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()
        dao = database.locationDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    // =========================================================================
    // BASIC CRUD
    // =========================================================================

    @Test
    fun insertLocation_singleRow_roundTripsById() = runTest {
        val entity = TestDataBuilder.locationEntity(id = "rt1", name = "Round Trip")

        dao.insertLocation(entity)

        val fetched = dao.getLocationById("rt1")
        assertThat(fetched).isNotNull()
        assertThat(fetched!!.name).isEqualTo("Round Trip")
        assertThat(fetched.latitude).isEqualTo(entity.latitude)
    }

    @Test
    fun getLocationById_missingId_returnsNull() = runTest {
        val fetched = dao.getLocationById("does_not_exist")
        assertThat(fetched).isNull()
    }

    @Test
    fun insertLocations_batch_allRowsPersisted() = runTest {
        val entities = (1..5).map {
            TestDataBuilder.locationEntity(id = "batch_$it", name = "Batch $it")
        }

        dao.insertLocations(entities)

        assertThat(dao.getLocationCount()).isEqualTo(5)
    }

    @Test
    fun updateLocation_changesPersisted() = runTest {
        val original = TestDataBuilder.locationEntity(id = "upd", name = "Original")
        dao.insertLocation(original)

        val updated = original.copy(name = "Updated", isFavorite = true)
        dao.updateLocation(updated)

        val fetched = dao.getLocationById("upd")
        assertThat(fetched!!.name).isEqualTo("Updated")
        assertThat(fetched.isFavorite).isTrue()
    }

    @Test
    fun deleteLocation_byEntity_removesRow() = runTest {
        val entity = TestDataBuilder.locationEntity(id = "del1")
        dao.insertLocation(entity)
        assertThat(dao.getLocationCount()).isEqualTo(1)

        dao.deleteLocation(entity)

        assertThat(dao.getLocationCount()).isEqualTo(0)
        assertThat(dao.getLocationById("del1")).isNull()
    }

    @Test
    fun deleteLocationById_removesRow() = runTest {
        dao.insertLocation(TestDataBuilder.locationEntity(id = "del2"))

        dao.deleteLocationById("del2")

        assertThat(dao.getLocationById("del2")).isNull()
    }

    @Test
    fun deleteAllLocations_emptiesTable() = runTest {
        dao.insertLocations(TestDataBuilder.austinLandmarks().map {
            LocationEntity.fromDomainModel(it)
        })
        assertThat(dao.getLocationCount()).isEqualTo(3)

        dao.deleteAllLocations()

        assertThat(dao.getLocationCount()).isEqualTo(0)
    }

    // =========================================================================
    // CONFLICT STRATEGY — proves @Insert(onConflict = REPLACE) works
    // =========================================================================

    @Test
    fun insertLocation_sameId_replacesInsteadOfFailing() = runTest {
        val v1 = TestDataBuilder.locationEntity(id = "conflict", name = "Version 1")
        val v2 = TestDataBuilder.locationEntity(id = "conflict", name = "Version 2")

        dao.insertLocation(v1)
        dao.insertLocation(v2) // Same PK — REPLACE strategy should overwrite.

        assertThat(dao.getLocationCount()).isEqualTo(1)
        assertThat(dao.getLocationById("conflict")!!.name).isEqualTo("Version 2")
    }

    // =========================================================================
    // FLOW SEMANTICS — the reason we don't mock this DAO
    // =========================================================================

    @Test
    fun getAllLocations_emitsInitialEmptyList() = runTest {
        dao.getAllLocations().test {
            assertThat(awaitItem()).isEmpty()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getAllLocations_reEmitsAfterInsert() = runTest {
        dao.getAllLocations().test {
            // Initial empty emission
            assertThat(awaitItem()).isEmpty()

            // Insert → Room invalidates the query → Flow re-emits
            dao.insertLocation(TestDataBuilder.locationEntity(id = "flow1", name = "A"))

            val afterInsert = awaitItem()
            assertThat(afterInsert).hasSize(1)
            assertThat(afterInsert[0].id).isEqualTo("flow1")

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getAllLocations_reEmitsAfterDelete() = runTest {
        dao.insertLocation(TestDataBuilder.locationEntity(id = "tmp"))

        dao.getAllLocations().test {
            assertThat(awaitItem()).hasSize(1)

            dao.deleteLocationById("tmp")

            assertThat(awaitItem()).isEmpty()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getAllLocations_orderedBySavedTimestampDescending() = runTest {
        val older = TestDataBuilder.locationEntity(
            id = "older", name = "Older", savedTimestamp = 1_000L
        )
        val newer = TestDataBuilder.locationEntity(
            id = "newer", name = "Newer", savedTimestamp = 2_000L
        )
        dao.insertLocation(older)
        dao.insertLocation(newer)

        dao.getAllLocations().test {
            val rows = awaitItem()
            assertThat(rows.map { it.id }).containsExactly("newer", "older").inOrder()
            cancelAndIgnoreRemainingEvents()
        }
    }

    // =========================================================================
    // FAVORITES
    // =========================================================================

    @Test
    fun getFavoriteLocations_returnsOnlyFavorites_orderedByName() = runTest {
        val mix = TestDataBuilder.mixedFavorites().map { LocationEntity.fromDomainModel(it) }
        dao.insertLocations(mix)

        dao.getFavoriteLocations().test {
            val favorites = awaitItem()
            assertThat(favorites).hasSize(3)
            assertThat(favorites.all { it.isFavorite }).isTrue()
            // Name ASC — "Favorite Coffee Shop" < "Favorite Hospital" < "Favorite Park"
            assertThat(favorites.map { it.name }).isInOrder()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getFavoriteCount_countsOnlyIsFavoriteTrue() = runTest {
        val mix = TestDataBuilder.mixedFavorites().map { LocationEntity.fromDomainModel(it) }
        dao.insertLocations(mix)

        assertThat(dao.getLocationCount()).isEqualTo(5)
        assertThat(dao.getFavoriteCount()).isEqualTo(3)
    }

    // =========================================================================
    // SEARCH — SQL LIKE pattern matching
    // =========================================================================

    @Test
    fun searchLocations_matchesByName() = runTest {
        dao.insertLocation(TestDataBuilder.locationEntity(id = "a", name = "Coffee Shop"))
        dao.insertLocation(TestDataBuilder.locationEntity(id = "b", name = "Tea House"))
        dao.insertLocation(TestDataBuilder.locationEntity(id = "c", name = "Coffee Bean"))

        dao.searchLocations("Coffee").test {
            val results = awaitItem()
            assertThat(results.map { it.id }).containsExactly("c", "a") // name ASC
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun searchLocations_matchesByAddress() = runTest {
        dao.insertLocation(TestDataBuilder.locationEntity(
            id = "x", name = "Anonymous", address = "123 Main Street"
        ))
        dao.insertLocation(TestDataBuilder.locationEntity(
            id = "y", name = "Somewhere", address = "456 Oak Ave"
        ))

        dao.searchLocations("Main").test {
            val results = awaitItem()
            assertThat(results).hasSize(1)
            assertThat(results[0].id).isEqualTo("x")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun searchLocations_emptyQuery_matchesEveryRow() = runTest {
        // LIKE '%%' matches every non-null string. This pins the contract:
        // the DAO does not short-circuit empty queries. Repository-layer
        // code should decide whether to blank-out the query before calling.
        dao.insertLocations(
            TestDataBuilder.austinLandmarks().map { LocationEntity.fromDomainModel(it) }
        )

        dao.searchLocations("").test {
            assertThat(awaitItem()).hasSize(3)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun searchLocations_noMatches_emitsEmptyList() = runTest {
        dao.insertLocation(TestDataBuilder.locationEntity(id = "z", name = "Library"))

        dao.searchLocations("zzzzzz").test {
            assertThat(awaitItem()).isEmpty()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun searchLocations_isCaseInsensitiveByDefault() = runTest {
        // SQLite LIKE is case-insensitive for ASCII by default. This test
        // documents that behavior so no one is surprised when unicode
        // characters (Café, Résumé) hit the case-sensitivity edge.
        dao.insertLocation(TestDataBuilder.locationEntity(id = "u", name = "UPPERCASE NAME"))

        dao.searchLocations("uppercase").test {
            assertThat(awaitItem()).hasSize(1)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // =========================================================================
    // NEARBY SEARCH (bounding box)
    // =========================================================================

    @Test
    fun getLocationsInBounds_returnsOnlyPointsInBox() = runTest {
        // Seed Austin-ish points plus one way outside the box.
        dao.insertLocations(
            TestDataBuilder.austinLandmarks().map { LocationEntity.fromDomainModel(it) }
        )
        dao.insertLocation(TestDataBuilder.locationEntity(
            id = "nyc", name = "NYC", latitude = 40.7128, longitude = -74.0060
        ))

        val inBounds = dao.getLocationsInBounds(
            minLat = 30.25, maxLat = 30.30,
            minLng = -97.80, maxLng = -97.70
        )

        assertThat(inBounds.map { it.id }).containsExactly("capitol", "zilker", "ut_tower")
        assertThat(inBounds.any { it.id == "nyc" }).isFalse()
    }

    @Test
    fun getLocationsInBounds_inclusiveBoundary_matchesBETWEEN() = runTest {
        // SQL BETWEEN is inclusive on both ends — pin that contract so a
        // future change to use strict < doesn't silently drop corner points.
        dao.insertLocation(TestDataBuilder.locationEntity(
            id = "edge", name = "Edge", latitude = 30.0, longitude = -97.0
        ))

        val inBounds = dao.getLocationsInBounds(
            minLat = 30.0, maxLat = 30.0,
            minLng = -97.0, maxLng = -97.0
        )

        assertThat(inBounds).hasSize(1)
    }

    @Test
    fun getLocationsInBounds_emptyBox_returnsEmptyList() = runTest {
        dao.insertLocation(TestDataBuilder.locationEntity(id = "only"))

        val inBounds = dao.getLocationsInBounds(
            minLat = 0.0, maxLat = 0.0,
            minLng = 0.0, maxLng = 0.0
        )

        assertThat(inBounds).isEmpty()
    }

    // =========================================================================
    // EDGE CASES — boundary-style inputs from TestDataBuilder
    // =========================================================================

    @Test
    fun insert_edgeCaseLocations_allPersist() = runTest {
        // Null Island (0,0), poles (±90,0), date line, empty name, unicode,
        // and a 500-char name all go in. The test proves the schema has no
        // length constraint and unicode is stored correctly.
        val edges = TestDataBuilder.edgeCaseLocations().map {
            LocationEntity.fromDomainModel(it)
        }

        dao.insertLocations(edges)

        assertThat(dao.getLocationCount()).isEqualTo(edges.size)

        val unicode = dao.getLocationById("special_chars")
        assertThat(unicode!!.name).isEqualTo("Café & Résumé — L'Artiste")

        val longName = dao.getLocationById("very_long_name")
        assertThat(longName!!.name).hasLength(500)
    }

    @Test
    fun insert_poleCoordinates_roundTripExactly() = runTest {
        val northPole = TestDataBuilder.locationEntity(
            id = "np", latitude = 90.0, longitude = 0.0
        )
        val southPole = TestDataBuilder.locationEntity(
            id = "sp", latitude = -90.0, longitude = 180.0
        )

        dao.insertLocations(listOf(northPole, southPole))

        assertThat(dao.getLocationById("np")!!.latitude).isEqualTo(90.0)
        assertThat(dao.getLocationById("sp")!!.longitude).isEqualTo(180.0)
    }
}
