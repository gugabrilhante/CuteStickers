package com.gustavo.brilhante.cutecats.core.domain

import com.gustavo.brilhante.cutecats.core.model.MediaItem
import kotlinx.coroutines.flow.Flow

interface MediaRepository {
    fun getMedia(): Flow<List<MediaItem>>
    suspend fun refresh()
    suspend fun loadNextPage()
}
