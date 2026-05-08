package com.gustavo.brilhante.cutecats.core.domain.usecase

import com.gustavo.brilhante.cutecats.core.domain.MediaRepository

class RefreshMediaUseCase(private val repository: MediaRepository) {
    suspend operator fun invoke() = repository.refresh()
}
