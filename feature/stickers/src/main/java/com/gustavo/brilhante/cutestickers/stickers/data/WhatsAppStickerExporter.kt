package com.gustavo.brilhante.cutestickers.stickers.data

import android.content.Context
import com.gustavo.brilhante.cutestickers.common.PackageManagerWrapper
import com.gustavo.brilhante.cutestickers.stickers.domain.ExportMetadata
import com.gustavo.brilhante.cutestickers.stickers.domain.StickerExporter
import com.gustavo.brilhante.cutestickers.stickers.domain.StickerPack
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class WhatsAppStickerExporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val packageManagerWrapper: PackageManagerWrapper,
    private val stickerStore: StickerStore
) : StickerExporter {

    companion object {
        private const val WHATSAPP_PACKAGE = "com.whatsapp"
        private const val WHATSAPP_BUSINESS_PACKAGE = "com.whatsapp.w4b"
    }

    override fun isWhatsAppInstalled(): Boolean =
        packageManagerWrapper.isPackageInstalled(WHATSAPP_PACKAGE) || 
        packageManagerWrapper.isPackageInstalled(WHATSAPP_BUSINESS_PACKAGE)

    override fun getExportMetadata(pack: StickerPack): Result<ExportMetadata> = runCatching {
        val targetPackage = when {
            packageManagerWrapper.isPackageInstalled(WHATSAPP_PACKAGE) -> WHATSAPP_PACKAGE
            packageManagerWrapper.isPackageInstalled(WHATSAPP_BUSINESS_PACKAGE) -> WHATSAPP_BUSINESS_PACKAGE
            else -> error("WhatsApp is not installed")
        }

        validatePackFiles(pack)

        ExportMetadata(
            packId = pack.id,
            authority = "${context.packageName}.StickerContentProvider",
            packName = pack.name,
            publisher = pack.publisher,
            trayImageFileName = pack.trayImageFileName,
            isAnimated = pack.isAnimated,
            targetPackage = targetPackage
        )
    }

    private fun validatePackFiles(pack: StickerPack) {
        val trayFile = stickerStore.getStickerFile(pack.id, pack.trayImageFileName)
        if (!trayFile.exists()) error("Tray icon missing")

        pack.stickers.forEach { sticker ->
            val file = stickerStore.getStickerFile(pack.id, sticker.imageFileName)
            if (!file.exists() || file.length() == 0L) error("Sticker file missing or empty: ${sticker.imageFileName}")
        }
    }
}
