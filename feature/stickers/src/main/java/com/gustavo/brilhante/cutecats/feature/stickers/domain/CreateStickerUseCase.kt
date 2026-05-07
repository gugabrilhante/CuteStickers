package com.gustavo.brilhante.cutecats.feature.stickers.domain

import javax.inject.Inject

class CreateStickerUseCase @Inject constructor(
    private val repository: StickerRepository
) {
    suspend operator fun invoke(imageUrl: String, mediaId: String): Result<StickerPack> =
        repository.createStickerFromUrl(imageUrl, mediaId)
}
