package com.gustavo.brilhante.cutecats.feature.stickers.domain

import android.content.Intent

interface StickerExporter {
    fun isWhatsAppInstalled(): Boolean
    fun buildExportIntent(pack: StickerPack): Result<Intent>
}
