package com.gustavo.brilhante.cutestickers.stickers.domain

import javax.inject.Inject

class SaveMediaUseCase @Inject constructor(
    private val repository: StickerRepository
) {
    suspend operator fun invoke(imageUrl: String): Result<Unit> =
        repository.saveMediaToGallery(imageUrl)
}
