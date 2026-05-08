package com.gustavo.brilhante.cutestickers.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cache_metadata")
data class CacheMetadataEntity(
    @PrimaryKey val featureKey: String,
    val lastUpdated: Long,
    val nextPage: Int = 1,
    val isSessionExpired: Boolean = false
)
