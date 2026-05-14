package com.gustavo.brilhante.cutestickers.domain.usecase

import com.gustavo.brilhante.cutestickers.domain.MediaRepository

class RefreshMediaUseCase(private val repository: MediaRepository) {
    suspend operator fun invoke(force: Boolean = true): Boolean = repository.refresh(force)
}
