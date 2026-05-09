package com.gustavo.brilhante.cutestickers.stickers.domain

import javax.inject.Inject

class ExportStickerPackUseCase @Inject constructor(
    private val exporter: StickerExporter
) {
    fun isWhatsAppInstalled(): Boolean = exporter.isWhatsAppInstalled()
    fun getExportMetadata(pack: StickerPack): Result<ExportMetadata> = exporter.getExportMetadata(pack)
}
