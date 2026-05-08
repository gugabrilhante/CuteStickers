package com.gustavo.brilhante.cutestickers.database

interface CacheMetadataDao {
    suspend fun getMetadata(featureKey: String): CacheMetadataEntity?

    suspend fun insertMetadata(metadata: CacheMetadataEntity)

    suspend fun deleteMetadata(featureKey: String)
}
