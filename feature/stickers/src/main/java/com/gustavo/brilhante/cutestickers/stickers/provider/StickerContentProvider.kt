package com.gustavo.brilhante.cutestickers.stickers.provider

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.gustavo.brilhante.cutestickers.common.Logger
import com.gustavo.brilhante.cutestickers.stickers.data.StickerInfo
import com.gustavo.brilhante.cutestickers.stickers.data.StickerPackInfo
import com.gustavo.brilhante.cutestickers.stickers.data.StickerStore
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
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

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    internal interface StickerContentProviderEntryPoint {
        fun stickerStore(): StickerStore
        fun logger(): Logger
    }

    private val stickerStore: StickerStore by lazy {
        EntryPointAccessors.fromApplication(
            context?.applicationContext ?: throw IllegalStateException("Context is null"),
            StickerContentProviderEntryPoint::class.java
        ).stickerStore()
    }

    private val logger: Logger by lazy {
        EntryPointAccessors.fromApplication(
            context?.applicationContext ?: throw IllegalStateException("Context is null"),
            StickerContentProviderEntryPoint::class.java
        ).logger()
    }

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

        private val STICKER_COLUMNS = arrayOf(
            "sticker_file_name",
            "sticker_emoji"
        )
    }

    private val authority: String by lazy { 
        context?.packageName?.let { "$it.StickerContentProvider" } ?: "com.gustavo.brilhante.cutestickers.StickerContentProvider"
    }

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
        logger.d(TAG, "Query URI: $uri | Match: $match")
        val store = stickerStore
        return when (match) {
            METADATA_ALL -> {
                val packs = store.loadAllPacks()
                logger.d(TAG, "Packs found: ${packs.size}")
                buildPacksCursor(packs)
            }
            METADATA_PACK -> {
                val packId = uri.lastPathSegment
                val packs = store.loadAllPacks()
                val pack = packs.find { it.id == packId }
                logger.d(TAG, "Pack ID $packId found: ${pack != null}")
                buildPacksCursor(if (pack != null) listOf(pack) else emptyList())
            }
            STICKERS -> {
                val packId = uri.lastPathSegment ?: return null
                val stickers = store.loadStickers(packId).filter { sticker ->
                    val file = store.getStickerFile(packId, sticker.imageFileName)
                    val exists = file.exists()
                    if (!exists) {
                        logger.e(TAG, "Sticker file missing: $packId/${sticker.imageFileName}")
                    } else if (file.length() > 500 * 1024) {
                        logger.e(TAG, "Sticker file too large: $packId/${sticker.imageFileName} (${file.length()} bytes)")
                    }
                    exists
                }
                logger.d(TAG, "Stickers for $packId: ${stickers.size}")
                buildStickersCursor(stickers)
            }
            else -> {
                logger.e(TAG, "No match for URI: $uri")
                null
            }
        }
    }

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        logger.d(TAG, "OpenFile URI: $uri | Mode: $mode")
        if (mode != "r") throw SecurityException("Only read-only mode is supported")
        
        val match = uriMatcher.match(uri)
        if (match != STICKERS_ASSET) {
            logger.e(TAG, "OpenFile match failed: $match")
            throw FileNotFoundException("Unrecognised URI: $uri")
        }
        
        val segments = uri.pathSegments
        if (segments.size != 3) throw FileNotFoundException("Expected 3 path segments: $uri")
        
        val packId = segments[1]
        val fileName = segments[2]

        // Validation: reject any path segment (both packId and fileName) that contains "/", "\","..", or null bytes
        val invalidChars = listOf("/", "\\", "..", "\u0000")
        if (invalidChars.any { packId.contains(it) } || invalidChars.any { fileName.contains(it) }) {
            logger.e(TAG, "Invalid path segment: packId=$packId, fileName=$fileName")
            throw SecurityException("Invalid path segment")
        }

        val store = stickerStore
        val file = store.getStickerFile(packId, fileName)

        // Resolve the resulting File to its canonical path and ensure it is a descendant of stickersRoot before opening
        try {
            val canonicalFile = file.canonicalFile
            val canonicalRoot = store.stickersRoot.canonicalFile
            if (!canonicalFile.path.startsWith(canonicalRoot.path)) {
                logger.e(TAG, "Path traversal attempt: ${canonicalFile.path} is not under ${canonicalRoot.path}")
                throw SecurityException("Access denied")
            }
        } catch (e: Exception) {
            logger.e(TAG, "Error validating file path", e)
            throw FileNotFoundException("Invalid file path")
        }
        
        if (!file.exists()) {
            logger.e(TAG, "File not found: ${file.absolutePath}")
            throw FileNotFoundException("Sticker not found: $packId/$fileName")
        }
        
        logger.d(TAG, "Serving file: ${file.name} (${file.length()} bytes)")
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    }

    override fun getType(uri: Uri): String {
        val match = uriMatcher.match(uri)
        val type = when (match) {
            METADATA_ALL -> "vnd.android.cursor.dir/vnd.$authority.metadata"
            METADATA_PACK -> "vnd.android.cursor.item/vnd.$authority.metadata"
            STICKERS -> "vnd.android.cursor.dir/vnd.$authority.stickers"
            STICKERS_ASSET -> {
                val segments = uri.pathSegments
                if (segments.size == 3) {
                    val fileName = segments[2].lowercase()
                    if (fileName.endsWith(".png")) "image/png"
                    else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) "image/jpeg"
                    else "image/webp"
                } else "image/webp"
            }
            else -> "image/webp"
        }
        logger.d(TAG, "GetType URI: $uri | Result: $type")
        return type
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
                stickerStore.getPackVersion(pack.id), // image_data_version
                1, // avoid_cache (1 = true)
                if (pack.isAnimated) 1 else 0  // animated_sticker_pack
            )
            cursor.addRow(row)
        }
        return cursor
    }

    private fun buildStickersCursor(
        stickers: List<StickerInfo>
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
