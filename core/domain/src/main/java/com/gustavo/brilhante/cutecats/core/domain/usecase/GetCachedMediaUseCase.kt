package com.gustavo.brilhante.cutecats.core.domain.usecase

import com.gustavo.brilhante.cutecats.core.domain.MediaRepository
import com.gustavo.brilhante.cutecats.core.model.MediaItem
import kotlinx.coroutines.flow.Flow

class GetCachedMediaUseCase(private val repository: MediaRepository) {
    operator fun invoke(): Flow<List<MediaItem>> = repository.getMedia()
}
