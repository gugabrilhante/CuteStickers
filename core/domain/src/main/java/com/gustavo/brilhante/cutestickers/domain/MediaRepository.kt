package com.gustavo.brilhante.cutestickers.domain

import com.gustavo.brilhante.cutestickers.model.MediaItem
import kotlinx.coroutines.flow.Flow

interface MediaRepository {
    fun getMedia(): Flow<List<MediaItem>>
    suspend fun refresh()
    suspend fun loadNextPage()
}
