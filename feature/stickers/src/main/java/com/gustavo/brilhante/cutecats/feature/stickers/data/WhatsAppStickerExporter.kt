package com.gustavo.brilhante.cutecats.feature.stickers.data

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.gustavo.brilhante.cutecats.feature.stickers.domain.StickerExporter
import com.gustavo.brilhante.cutecats.feature.stickers.domain.StickerPack
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class WhatsAppStickerExporter @Inject constructor(
    @ApplicationContext private val context: Context
) : StickerExporter {

    companion object {
        private const val TAG = "Sticker - WhatsAppExporter"
        private const val WHATSAPP_PACKAGE = "com.whatsapp"
        private const val WHATSAPP_BUSINESS_PACKAGE = "com.whatsapp.w4b"
        private const val ACTION_ENABLE_STICKER_PACK = "com.whatsapp.intent.action.ENABLE_STICKER_PACK"
    }

    override fun isWhatsAppInstalled(): Boolean =
        isPackageInstalled(WHATSAPP_PACKAGE) || isPackageInstalled(WHATSAPP_BUSINESS_PACKAGE)

    override fun buildExportIntent(pack: StickerPack): Result<Intent> = runCatching {
        Log.d(TAG, "buildExportIntent starting for pack: ${pack.id}")
        // Resolve which WhatsApp variant is installed. setPackage() is required —
        // without it, Android may not route an implicit intent to WhatsApp at all.
        val targetPackage = when {
            isPackageInstalled(WHATSAPP_PACKAGE) -> {
                Log.d(TAG, "WhatsApp found: $WHATSAPP_PACKAGE")
                WHATSAPP_PACKAGE
            }
            isPackageInstalled(WHATSAPP_BUSINESS_PACKAGE) -> {
                Log.d(TAG, "WhatsApp Business found: $WHATSAPP_BUSINESS_PACKAGE")
                WHATSAPP_BUSINESS_PACKAGE
            }
            else -> {
                Log.e(TAG, "No WhatsApp variant found")
                error("WhatsApp is not installed on this device")
            }
        }

        // Validate that all sticker files actually exist on disk before launching WhatsApp.
        // WhatsApp queries the ContentProvider synchronously during the import flow — missing
        // files cause a silent failure (no popup, no error message).
        validatePackFiles(pack)

        val authority = "${context.packageName}.StickerContentProvider"
        Log.d(TAG, "building intent — targetPackage=$targetPackage, authority=$authority, packId=${pack.id}, packName=${pack.name}")

        val metadataUri = Uri.parse("content://$authority/metadata/${pack.id}")
        Log.d(TAG, "Metadata URI: $metadataUri")

        val intent = Intent(ACTION_ENABLE_STICKER_PACK).apply {
            setPackage(targetPackage)
            putExtra("sticker_pack_id", pack.id)
            putExtra("sticker_pack_authority", authority)
            putExtra("sticker_pack_name", pack.name)
            putExtra("sticker_pack_publisher", pack.publisher)
            putExtra("sticker_pack_tray_image_file_name", pack.trayImageFileName)

            clipData = ClipData.newRawUri("", metadataUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        Log.d(TAG, "Granting URI permission to $targetPackage")
        context.grantUriPermission(
            targetPackage,
            metadataUri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )

        Log.d(TAG, "Intent built successfully:")
        Log.d(TAG, "  Action: ${intent.action}")
        Log.d(TAG, "  Package: $targetPackage")
        Log.d(TAG, "  Intent ClipData: ${intent.clipData}")
        Log.d(TAG, "  Flags: ${intent.flags}")

        intent
    }

    private fun validatePackFiles(pack: StickerPack) {
        val store = StickerStore(context)

        val trayFile = store.getStickerFile(pack.id, pack.trayImageFileName)
        if (!trayFile.exists()) error("Tray icon not found on disk: ${trayFile.absolutePath}")
        Log.d(TAG, "tray icon OK — ${trayFile.length()}B")

        pack.stickers.forEach { sticker ->
            val file = store.getStickerFile(pack.id, sticker.imageFileName)
            if (!file.exists()) error("Sticker file missing: ${file.absolutePath}")
            if (file.length() == 0L) error("Sticker file is empty: ${file.absolutePath}")
            Log.d(TAG, "sticker ${sticker.imageFileName} OK — ${file.length()}B")
        }
    }

    private fun isPackageInstalled(packageName: String): Boolean = runCatching {
        @Suppress("DEPRECATION")
        context.packageManager.getPackageInfo(packageName, 0)
        true
    }.getOrDefault(false)
}
