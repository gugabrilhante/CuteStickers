package com.gustavo.brilhante.cutestickers.dogs.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.gustavo.brilhante.cutestickers.database.CacheMetadataEntity

@Database(
    entities = [
        DogEntity::class,
        CacheMetadataEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class DogDatabase : RoomDatabase() {
    abstract fun dogDao(): DogDao
    abstract fun dogCacheMetadataDao(): DogCacheMetadataDao
}
