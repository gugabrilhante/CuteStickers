package com.gustavo.brilhante.cutestickers.stickers.data

import com.gustavo.brilhante.cutestickers.common.Logger
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
internal data class StickerPackInfo(
    val id: String,
    val name: String,
    val publisher: String,
    @SerialName("tray_image_file") val trayImageFileName: String,
    val stickers: List<StickerInfo>,
    val isAnimated: Boolean = false,
    val version: String = "1"
)

@Serializable
internal data class StickerInfo(
    @SerialName("image_file") val imageFileName: String,
    val emojis: List<String> = listOf("😊")
)

internal interface StickerStore {
    val stickersRoot: File
    fun getPackVersion(packId: String): String
    fun savePack(pack: StickerPackInfo)
    fun loadAllPacks(): List<StickerPackInfo>
    fun loadStickers(packId: String): List<StickerInfo>
    fun getStickerFile(packId: String, fileName: String): File
    fun migrateIfNeeded()
}

@Singleton
internal class StickerStoreImpl @Inject constructor(
    @com.gustavo.brilhante.cutestickers.stickers.di.StickersDir override val stickersRoot: File,
    private val logger: Logger
) : StickerStore {

    companion object {
        private const val TAG = "StickerStore"
        private val json = Json { ignoreUnknownKeys = true }
    }

    override fun migrateIfNeeded() {
        val files = stickersRoot.listFiles() ?: return
        val oldPacks = files.filter {
            it.isDirectory && it.name != "anim_pack" && it.name != "static_pack"
        }

        for (oldPackDir in oldPacks) {
            val packInfo = loadPackFromDir(oldPackDir) ?: continue
            val newId = if (packInfo.isAnimated) "anim_pack" else "static_pack"
            val newPackDir = File(stickersRoot, newId)

            if (!newPackDir.exists()) {
                if (oldPackDir.renameTo(newPackDir)) {
                    val updatedPackInfo = packInfo.copy(id = newId)
                    try {
                        File(newPackDir, "pack_info.json").writeText(json.encodeToString(updatedPackInfo))
                        logger.d(TAG, "Migrated pack from ${oldPackDir.name} to $newId")
                    } catch (e: Exception) {
                        logger.e(TAG, "Failed to write migrated pack info", e)
                    }
                }
            } else {
                oldPackDir.deleteRecursively()
            }
        }
    }

    override fun getPackVersion(packId: String): String {
        val infoFile = File(File(stickersRoot, packId), "pack_info.json")
        return if (infoFile.exists()) infoFile.lastModified().toString() else "1"
    }

    override fun savePack(pack: StickerPackInfo) {
        val packDir = File(stickersRoot, pack.id).also { it.mkdirs() }
        File(packDir, "pack_info.json").writeText(json.encodeToString(pack))
    }

    override fun loadAllPacks(): List<StickerPackInfo> =
        stickersRoot.listFiles()
            ?.filter { it.isDirectory }
            ?.mapNotNull { loadPackFromDir(it) }
            ?: emptyList()

    override fun loadStickers(packId: String): List<StickerInfo> =
        loadPackFromDir(File(stickersRoot, packId))?.stickers ?: emptyList()

    override fun getStickerFile(packId: String, fileName: String): File =
        File(File(stickersRoot, packId), fileName)

    private fun loadPackFromDir(dir: File): StickerPackInfo? = runCatching {
        val infoFile = File(dir, "pack_info.json")
        if (!infoFile.exists()) return null
        json.decodeFromString<StickerPackInfo>(infoFile.readText())
    }.getOrNull()
}
