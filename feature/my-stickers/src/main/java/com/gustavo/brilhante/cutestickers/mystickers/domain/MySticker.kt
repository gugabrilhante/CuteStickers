package com.gustavo.brilhante.cutestickers.mystickers.domain

import com.gustavo.brilhante.cutestickers.model.MediaType

data class MySticker(
    val id: String,
    val localPath: String,
    val sourceType: SourceType,
    val createdAt: Long,
    val mediaType: MediaType = MediaType.Static
)
