package com.gustavo.brilhante.cutestickers.stickers.domain

import android.content.Intent

interface StickerExporter {
    fun isWhatsAppInstalled(): Boolean
    fun buildExportIntent(pack: StickerPack): Result<Intent>
}
