package com.gustavo.brilhante.cutestickers.stickers.domain

interface StickerRepository {
    suspend fun createStickerFromUrl(imageUrl: String, mediaId: String): Result<StickerPack>
    suspend fun saveMediaToGallery(imageUrl: String): Result<Unit>
}
