package com.gustavo.brilhante.cutecats.feature.stickers.domain

data class StickerPack(
    val id: String,
    val name: String,
    val publisher: String,
    val trayImageFileName: String,
    val stickers: List<StickerItem>
)

data class StickerItem(
    val imageFileName: String,
    val emojis: List<String>
)
