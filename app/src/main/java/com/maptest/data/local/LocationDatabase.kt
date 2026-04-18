package com.maptest.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

// =============================================================================
// ROOM DATABASE
// =============================================================================
// This is the main database class. Room generates the implementation.
//
// KEY CONCEPTS:
// - version = 1: Schema version. When you change the schema, increment this
//   and provide a migration. Apple-scale apps have complex migration strategies.
// - exportSchema = false: In production, set to true and save schema JSON
//   for migration validation. False here for simplicity.
//
// TESTING: In tests, we use Room.inMemoryDatabaseBuilder() which creates
// a database that lives only in RAM and is destroyed when the process ends.
// This gives us fast, isolated, deterministic tests.
// =============================================================================

@Database(
    entities = [LocationEntity::class],
    version = 1,
    exportSchema = false
)
abstract class LocationDatabase : RoomDatabase() {
    abstract fun locationDao(): LocationDao
}
