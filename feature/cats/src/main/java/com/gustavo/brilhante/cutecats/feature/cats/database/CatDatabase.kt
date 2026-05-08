package com.gustavo.brilhante.cutecats.feature.cats.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.gustavo.brilhante.cutecats.core.database.CacheMetadataDao
import com.gustavo.brilhante.cutecats.core.database.CacheMetadataEntity

@Database(
    entities = [
        CatEntity::class,
        CacheMetadataEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class CatDatabase : RoomDatabase() {
    abstract fun catDao(): CatDao
    abstract fun catCacheMetadataDao(): CatCacheMetadataDao
}
