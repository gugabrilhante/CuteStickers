package com.gustavo.brilhante.cutecats.feature.stickers.domain

import android.content.Intent
import javax.inject.Inject

class ExportStickerPackUseCase @Inject constructor(
    private val exporter: StickerExporter
) {
    fun isWhatsAppInstalled(): Boolean = exporter.isWhatsAppInstalled()
    fun buildIntent(pack: StickerPack): Result<Intent> = exporter.buildExportIntent(pack)
}
