package com.gustavo.brilhante.cutecats.feature.stickers.domain

interface StickerRepository {
    suspend fun createStickerFromUrl(imageUrl: String, mediaId: String): Result<StickerPack>
    suspend fun saveMediaToGallery(imageUrl: String): Result<Unit>
}
