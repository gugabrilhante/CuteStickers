package com.gustavo.brilhante.cutestickers.stickers.data

import android.content.Context
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
internal data class StickerPackInfo(
    val id: String,
    val name: String,
    val publisher: String,
    @SerialName("tray_image_file") val trayImageFileName: String,
    val stickers: List<StickerInfo>,
    val isAnimated: Boolean = false
)

@Serializable
internal data class StickerInfo(
    @SerialName("image_file") val imageFileName: String,
    val emojis: List<String> = listOf("😊")
)

internal class StickerStore(context: Context) {
    val stickersRoot = File(context.filesDir, "stickers")

    companion object {
        private val json = Json { ignoreUnknownKeys = true }
    }

    fun getPackVersion(packId: String): String {
        val infoFile = File(File(stickersRoot, packId), "pack_info.json")
        return if (infoFile.exists()) infoFile.lastModified().toString() else "1"
    }

    fun savePack(pack: StickerPackInfo) {
        val packDir = File(stickersRoot, pack.id).also { it.mkdirs() }
        File(packDir, "pack_info.json").writeText(json.encodeToString(pack))
    }

    fun loadAllPacks(): List<StickerPackInfo> =
        stickersRoot.listFiles()
            ?.filter { it.isDirectory }
            ?.mapNotNull { loadPackFromDir(it) }
            ?: emptyList()

    fun loadStickers(packId: String): List<StickerInfo> =
        loadPackFromDir(File(stickersRoot, packId))?.stickers ?: emptyList()

    fun getStickerFile(packId: String, fileName: String): File =
        File(File(stickersRoot, packId), fileName)

    private fun loadPackFromDir(dir: File): StickerPackInfo? = runCatching {
        val infoFile = File(dir, "pack_info.json")
        if (!infoFile.exists()) return null
        json.decodeFromString<StickerPackInfo>(infoFile.readText())
    }.getOrNull()
}
