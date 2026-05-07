package com.gustavo.brilhante.cutecats.feature.stickers.provider

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import com.gustavo.brilhante.cutecats.feature.stickers.data.StickerInfo
import com.gustavo.brilhante.cutecats.feature.stickers.data.StickerPackInfo
import com.gustavo.brilhante.cutecats.feature.stickers.data.StickerStore
import java.io.FileNotFoundException

/**
 * Exposes sticker assets and pack metadata to WhatsApp via the official ContentProvider protocol.
 *
 * Exact URI scheme WhatsApp uses (verified against the official sample):
 *   content://<authority>/metadata                           → all packs list
 *   content://<authority>/metadata/<packId>                 → sticker list for one pack
 *   content://<authority>/stickers_asset/<packId>/<file>    → sticker or tray WebP file
 *
 * NOTE: The path segment for files is "stickers_asset", NOT "stickers". Using the wrong
 * path causes UriMatcher to return NO_MATCH → openFile() throws → WhatsApp silently
 * aborts validation before ever showing the import popup.
 *
 * The provider MUST run in our app's process (no multiprocess="true") so that openFile()
 * can read from our private filesDir and hand WhatsApp a cross-process ParcelFileDescriptor.
 */
class StickerContentProvider : ContentProvider() {

    companion object {
        private const val TAG = "StickerProvider"

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

        // sticker_file_size is NOT required by the official sample, although some guides mention it.
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
            addURI(authority, "stickers_asset/*/*",   STICKERS_ASSET)  // ← "stickers_asset", not "stickers"
        }
    }

    override fun onCreate(): Boolean {
        Log.d(TAG, "StickerContentProvider.onCreate — authority=$authority")
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): Cursor? {
        Log.d(TAG, "StickerContentProvider.query ← $uri")
        val match = uriMatcher.match(uri)
        Log.d(TAG, "  match code: $match")
        val store = StickerStore(context!!)
        return when (match) {
            METADATA_ALL -> {
                val packs = store.loadAllPacks()
                Log.d(TAG, "  METADATA_ALL → ${packs.size} pack(s) found")
                buildPacksCursor(packs)
            }
            METADATA_PACK -> {
                val packId = uri.lastPathSegment
                val packs = store.loadAllPacks()
                val pack = packs.find { it.id == packId }
                Log.d(TAG, "  METADATA_PACK($packId) → ${if (pack != null) "found" else "NOT FOUND"}")
                buildPacksCursor(if (pack != null) listOf(pack) else emptyList())
            }
            STICKERS -> {
                val packId = uri.lastPathSegment
                    ?: return null.also { Log.w(TAG, "  STICKERS — missing packId in $uri") }
                val stickers = store.loadStickers(packId)
                Log.d(TAG, "  STICKERS($packId) → ${stickers.size} sticker(s) found")
                buildStickersCursor(packId, stickers, store)
            }
            else -> null.also { Log.w(TAG, "  UNMATCHED URI — $uri (match=$match)") }
        }
    }

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        Log.d(TAG, "StickerContentProvider.openFile ← $uri")
        if (uriMatcher.match(uri) != STICKERS_ASSET) {
            Log.e(TAG, "  openFile — URI not matched (STICKERS_ASSET expected): $uri")
            throw FileNotFoundException("Unrecognised URI: $uri")
        }
        // URI segments: [0]=stickers_asset, [1]=packId, [2]=fileName
        val segments = uri.pathSegments
        if (segments.size != 3) throw FileNotFoundException("Expected 3 path segments, got ${segments.size}: $uri")
        val packId = segments[1]
        val fileName = segments[2]

        val file = StickerStore(context!!).getStickerFile(packId, fileName)
        Log.d(TAG, "  serving $packId/$fileName — exists=${file.exists()}, size=${file.length()}B, path=${file.absolutePath}")
        if (!file.exists()) throw FileNotFoundException("Sticker not found: $packId/$fileName")
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    }

    override fun getType(uri: Uri): String {
        Log.d(TAG, "getType ← $uri")
        val match = uriMatcher.match(uri)
        val result = when (match) {
            METADATA_ALL -> "vnd.android.cursor.dir/vnd.$authority.metadata"
            METADATA_PACK -> "vnd.android.cursor.item/vnd.$authority.metadata"
            STICKERS -> "vnd.android.cursor.dir/vnd.$authority.stickers"
            STICKERS_ASSET -> "image/webp"
            else -> {
                Log.w(TAG, "  getType — UNKNOWN URI: $uri (match=$match)")
                throw IllegalArgumentException("Unknown URI: $uri (match=$match)")
            }
        }
        Log.d(TAG, "  getType → $result")
        return result
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
                1, // avoid_cache — always re-fetch so cached failures don't block re-import
                0  // animated_sticker_pack
            )
            cursor.addRow(row)
        }
        Log.d(TAG, "  buildPacksCursor → rows: ${cursor.count}")
        return cursor
    }

    private fun buildStickersCursor(
        packId: String,
        stickers: List<StickerInfo>,
        store: StickerStore
    ): Cursor {
        val cursor = MatrixCursor(STICKER_COLUMNS)
        stickers.forEach { sticker ->
            val file = store.getStickerFile(packId, sticker.imageFileName)
            Log.d(TAG, "  sticker ${sticker.imageFileName} — exists=${file.exists()}, size=${file.length()}B")
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
