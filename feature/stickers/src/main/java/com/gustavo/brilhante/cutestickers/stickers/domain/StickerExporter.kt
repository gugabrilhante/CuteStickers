package com.gustavo.brilhante.cutestickers.stickers.domain

data class ExportMetadata(
    val packId: String,
    val authority: String,
    val packName: String,
    val publisher: String,
    val trayImageFileName: String,
    val isAnimated: Boolean,
    val targetPackage: String
)

interface StickerExporter {
    fun isWhatsAppInstalled(): Boolean
    fun getExportMetadata(pack: StickerPack): Result<ExportMetadata>
}
