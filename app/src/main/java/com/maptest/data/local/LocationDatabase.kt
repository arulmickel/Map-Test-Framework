package com.maptest.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

// version = 1: bump and add a Migration when the schema changes.
// exportSchema = false for simplicity; flip to true to write schema JSON
// for migration validation. Tests use Room.inMemoryDatabaseBuilder().

@Database(
    entities = [LocationEntity::class],
    version = 1,
    exportSchema = false
)
abstract class LocationDatabase : RoomDatabase() {
    abstract fun locationDao(): LocationDao
}
