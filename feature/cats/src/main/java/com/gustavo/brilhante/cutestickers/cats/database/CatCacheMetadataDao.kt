package com.gustavo.brilhante.cutestickers.cats.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gustavo.brilhante.cutestickers.database.CacheMetadataDao
import com.gustavo.brilhante.cutestickers.database.CacheMetadataEntity

@Dao
interface CatCacheMetadataDao : CacheMetadataDao {
    @Query("SELECT * FROM cache_metadata WHERE featureKey = :featureKey")
    override suspend fun getMetadata(featureKey: String): CacheMetadataEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    override suspend fun insertMetadata(metadata: CacheMetadataEntity)

    @Query("DELETE FROM cache_metadata WHERE featureKey = :featureKey")
    override suspend fun deleteMetadata(featureKey: String)
}
