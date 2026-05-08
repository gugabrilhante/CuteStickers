package com.gustavo.brilhante.cutestickers.stickers.provider

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.gustavo.brilhante.cutestickers.stickers.data.StickerInfo
import com.gustavo.brilhante.cutestickers.stickers.data.StickerPackInfo
import com.gustavo.brilhante.cutestickers.stickers.data.StickerStore
import java.io.FileNotFoundException

/**
 * Exposes sticker assets and pack metadata to WhatsApp via the official ContentProvider protocol.
 *
 * Exact URI scheme WhatsApp uses (verified against the official sample):
 *   content://<authority>/metadata                           → all packs list
 *   content://<authority>/metadata/<packId>                 → sticker list for one pack
 *   content://<authority>/stickers_asset/<packId>/<file>    → sticker or tray WebP file
 */
class StickerContentProvider : ContentProvider() {

    companion object {
        private const val METADATA_ALL = 1
        private const val METADATA_PACK = 2
        private const val STICKERS = 3
        private const val STICKERS_ASSET = 4   // "stickers_asset" — official WhatsApp path

        // Column set mirrors the official WhatsApp sticker sample exactly.
        private val PACK_COLUMNS = arrayOf(
            "sticker_pack_identifier",
            "sticker_pack_name",
            "sticker_pack_publisher",
            "sticker_pack_icon",
            "android_play_store_link",
            "ios_app_download_link",
            "sticker_pack_publisher_email",
            "sticker_pack_publisher_website",
            "sticker_pack_privacy_policy_website",
            "sticker_pack_license_agreenment_website", // Misspelling required by WhatsApp
            "image_data_version",
            "avoid_cache",
            "animated_sticker_pack"
        )

        private val STICKER_COLUMNS = arrayOf(
            "sticker_file_name",
            "sticker_emoji"
        )
    }

    private val authority: String by lazy { "${context!!.packageName}.StickerContentProvider" }

    private val uriMatcher: UriMatcher by lazy {
        UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(authority, "metadata",             METADATA_ALL)
            addURI(authority, "metadata/*",           METADATA_PACK)
            addURI(authority, "stickers/*",           STICKERS)
            addURI(authority, "stickers_asset/*/*",   STICKERS_ASSET)
        }
    }

    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): Cursor? {
        val match = uriMatcher.match(uri)
        val store = StickerStore(context!!)
        return when (match) {
            METADATA_ALL -> {
                val packs = store.loadAllPacks()
                buildPacksCursor(packs)
            }
            METADATA_PACK -> {
                val packId = uri.lastPathSegment
                val packs = store.loadAllPacks()
                val pack = packs.find { it.id == packId }
                buildPacksCursor(if (pack != null) listOf(pack) else emptyList())
            }
            STICKERS -> {
                val packId = uri.lastPathSegment ?: return null
                val stickers = store.loadStickers(packId)
                buildStickersCursor(packId, stickers, store)
            }
            else -> null
        }
    }

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        val match = uriMatcher.match(uri)
        if (match != STICKERS_ASSET) throw FileNotFoundException("Unrecognised URI: $uri")
        
        val segments = uri.pathSegments
        if (segments.size != 3) throw FileNotFoundException("Expected 3 path segments: $uri")
        
        val packId = segments[1]
        val fileName = segments[2]

        val file = StickerStore(context!!).getStickerFile(packId, fileName)
        if (!file.exists()) throw FileNotFoundException("Sticker not found: $packId/$fileName")
        
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    }

    override fun getType(uri: Uri): String {
        val match = uriMatcher.match(uri)
        return when (match) {
            METADATA_ALL -> "vnd.android.cursor.dir/vnd.$authority.metadata"
            METADATA_PACK -> "vnd.android.cursor.item/vnd.$authority.metadata"
            STICKERS -> "vnd.android.cursor.dir/vnd.$authority.stickers"
            STICKERS_ASSET -> "image/webp"
            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }
    }

    private fun buildPacksCursor(packs: List<StickerPackInfo>): Cursor {
        val cursor = MatrixCursor(PACK_COLUMNS)
        packs.forEach { pack ->
            val row = arrayOf<Any?>(
                pack.id,
                pack.name,
                pack.publisher,
                pack.trayImageFileName,
                "", // android_play_store_link
                "", // ios_app_download_link
                "", // sticker_pack_publisher_email
                "", // sticker_pack_publisher_website
                "", // sticker_pack_privacy_policy_website
                "", // sticker_pack_license_agreenment_website
                "1", // image_data_version
                1, // avoid_cache
                0  // animated_sticker_pack
            )
            cursor.addRow(row)
        }
        return cursor
    }

    private fun buildStickersCursor(
        packId: String,
        stickers: List<StickerInfo>,
        store: StickerStore
    ): Cursor {
        val cursor = MatrixCursor(STICKER_COLUMNS)
        stickers.forEach { sticker ->
            cursor.addRow(arrayOf(
                sticker.imageFileName,
                sticker.emojis.take(3).joinToString(",")
            ))
        }
        return cursor
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<String>?): Int = 0
}
