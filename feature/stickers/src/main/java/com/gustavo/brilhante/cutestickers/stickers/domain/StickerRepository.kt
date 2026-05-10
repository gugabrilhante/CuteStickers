package com.gustavo.brilhante.cutestickers.stickers.domain

import com.gustavo.brilhante.cutestickers.model.MediaType

interface StickerRepository {
    suspend fun createStickerFromUrl(
        imageUrl: String,
        mediaId: String,
        mediaType: MediaType,
        isCropped: Boolean = true
    ): Result<StickerPack>
    suspend fun saveMediaToGallery(imageUrl: String): Result<Unit>
}
