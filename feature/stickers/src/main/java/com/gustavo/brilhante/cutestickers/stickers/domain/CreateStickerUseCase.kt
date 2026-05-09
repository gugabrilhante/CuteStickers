package com.gustavo.brilhante.cutestickers.stickers.domain

import com.gustavo.brilhante.cutestickers.model.MediaType
import javax.inject.Inject

class CreateStickerUseCase @Inject constructor(
    private val repository: StickerRepository
) {
    suspend operator fun invoke(imageUrl: String, mediaId: String, mediaType: MediaType): Result<StickerPack> =
        repository.createStickerFromUrl(imageUrl, mediaId, mediaType)
}
