package com.gustavo.brilhante.cutestickers.domain.usecase

import com.gustavo.brilhante.cutestickers.domain.MediaRepository
import com.gustavo.brilhante.cutestickers.model.MediaItem
import kotlinx.coroutines.flow.Flow

class GetCachedMediaUseCase(private val repository: MediaRepository) {
    operator fun invoke(): Flow<List<MediaItem>> = repository.getMedia()
}
