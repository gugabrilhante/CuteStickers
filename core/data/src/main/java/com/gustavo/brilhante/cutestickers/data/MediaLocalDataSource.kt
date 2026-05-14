package com.gustavo.brilhante.cutestickers.data

import com.gustavo.brilhante.cutestickers.model.MediaItem
import kotlinx.coroutines.flow.Flow

interface MediaLocalDataSource {
    fun getMedia(): Flow<List<MediaItem>>
    suspend fun insertAll(items: List<MediaItem>)
    suspend fun clear()
    suspend fun isEmpty(): Boolean
}
