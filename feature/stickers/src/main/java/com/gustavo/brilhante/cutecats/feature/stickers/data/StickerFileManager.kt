package com.gustavo.brilhante.cutecats.feature.stickers.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class StickerFileManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val stickersRoot: File
        get() = File(context.filesDir, "stickers").also { it.mkdirs() }

    fun packDir(packId: String): File =
        File(stickersRoot, packId).also { it.mkdirs() }

    fun stickerFile(packId: String, mediaId: String): File =
        File(packDir(packId), "$mediaId.webp")

    fun trayIconFile(packId: String): File =
        File(packDir(packId), "tray_icon.webp")

    fun packInfoFile(packId: String): File =
        File(packDir(packId), "pack_info.json")
}
