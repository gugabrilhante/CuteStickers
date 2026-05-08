package com.gustavo.brilhante.cutecats.core.database

interface CacheMetadataDao {
    suspend fun getMetadata(featureKey: String): CacheMetadataEntity?

    suspend fun insertMetadata(metadata: CacheMetadataEntity)

    suspend fun deleteMetadata(featureKey: String)
}
