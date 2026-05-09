package com.gustavo.brilhante.cutestickers.stickers.domain

data class StickerPack(
    val id: String,
    val name: String,
    val publisher: String,
    val trayImageFileName: String,
    val stickers: List<StickerItem>,
    val isAnimated: Boolean = false
)

data class StickerItem(
    val imageFileName: String,
    val emojis: List<String>
)
