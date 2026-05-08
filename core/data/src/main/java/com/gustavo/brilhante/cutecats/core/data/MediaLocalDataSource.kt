package com.gustavo.brilhante.cutecats.core.data

import com.gustavo.brilhante.cutecats.core.model.MediaItem
import kotlinx.coroutines.flow.Flow

interface MediaLocalDataSource {
    fun getMedia(): Flow<List<MediaItem>>
    suspend fun insertAll(items: List<MediaItem>)
    suspend fun clear()
}
