package com.gustavo.brilhante.cutestickers.domain.usecase

import com.gustavo.brilhante.cutestickers.domain.MediaRepository

class LoadNextPageUseCase(private val repository: MediaRepository) {
    suspend operator fun invoke() = repository.loadNextPage()
}
