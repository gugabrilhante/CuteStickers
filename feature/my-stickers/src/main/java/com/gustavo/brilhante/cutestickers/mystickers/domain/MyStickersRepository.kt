package com.gustavo.brilhante.cutestickers.mystickers.domain

import kotlinx.coroutines.flow.Flow

interface MyStickersRepository {
    fun getStickers(): Flow<List<MySticker>>
    suspend fun saveFromUri(uriString: String): Result<MySticker>
    suspend fun saveFromUrl(imageUrl: String, mediaId: String): Result<MySticker>
    suspend fun deleteSticker(id: String): Result<Unit>
}
