package com.maptest.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.maptest.domain.model.LocationCategory
import com.maptest.domain.model.SavedLocation

// Room storage row. Kept separate from the domain SavedLocation so DB
// schema changes don't propagate into the UI layer.

@Entity(tableName = "saved_locations")
data class LocationEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val address: String = "",
    val isFavorite: Boolean = false,
    val savedTimestamp: Long = System.currentTimeMillis(),
    val category: String = LocationCategory.OTHER.name
) {
    fun toDomainModel(): SavedLocation = SavedLocation(
        id = id,
        name = name,
        latitude = latitude,
        longitude = longitude,
        address = address,
        isFavorite = isFavorite,
        savedTimestamp = savedTimestamp,
        category = try {
            LocationCategory.valueOf(category)
        } catch (e: IllegalArgumentException) {
            LocationCategory.OTHER
        }
    )

    companion object {
        fun fromDomainModel(location: SavedLocation): LocationEntity = LocationEntity(
            id = location.id,
            name = location.name,
            latitude = location.latitude,
            longitude = location.longitude,
            address = location.address,
            isFavorite = location.isFavorite,
            savedTimestamp = location.savedTimestamp,
            category = location.category.name
        )
    }
}
