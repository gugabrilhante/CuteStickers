package com.gustavo.brilhante.cutestickers.mystickers.domain

data class MySticker(
    val id: String,
    val localPath: String,
    val sourceType: SourceType,
    val createdAt: Long
)
