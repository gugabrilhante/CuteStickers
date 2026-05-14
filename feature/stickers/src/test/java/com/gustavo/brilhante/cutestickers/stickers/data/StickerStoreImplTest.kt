package com.gustavo.brilhante.cutestickers.stickers.data

import com.gustavo.brilhante.cutestickers.common.Logger
import io.mockk.mockk
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class StickerStoreImplTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val logger = mockk<Logger>(relaxed = true)
    private val json = Json { ignoreUnknownKeys = true }

    private fun createStore(): StickerStoreImpl {
        return StickerStoreImpl(tempFolder.root, logger)
    }

    @Test
    fun `savePack writes json file to correct directory`() {
        val store = createStore()
        val pack = StickerPackInfo("id123", "Name", "Publisher", "tray.webp", emptyList())
        
        store.savePack(pack)
        
        val packDir = File(tempFolder.root, "id123")
        val infoFile = File(packDir, "pack_info.json")
        assertTrue(infoFile.exists())
        val savedPack = json.decodeFromString<StickerPackInfo>(infoFile.readText())
        assertEquals("id123", savedPack.id)
    }

    @Test
    fun `migrateIfNeeded renames old directories and updates json`() {
        // Setup old pack
        val oldDir = tempFolder.newFolder("old_id")
        val packInfo = StickerPackInfo("old_id", "Old Name", "Pub", "tray.webp", emptyList(), isAnimated = false)
        File(oldDir, "pack_info.json").writeText(json.encodeToString(packInfo))
        
        val store = createStore()
        store.migrateIfNeeded()
        
        assertFalse("Old directory should be gone", oldDir.exists())
        val newDir = File(tempFolder.root, "static_pack")
        assertTrue("New directory should exist", newDir.exists())
        
        val newInfoFile = File(newDir, "pack_info.json")
        val newPackInfo = json.decodeFromString<StickerPackInfo>(newInfoFile.readText())
        assertEquals("static_pack", newPackInfo.id)
    }

    @Test
    fun `loadAllPacks returns all packs in directory`() {
        val store = createStore()
        val pack1 = StickerPackInfo("id1", "N1", "P", "t.webp", emptyList())
        val pack2 = StickerPackInfo("id2", "N2", "P", "t.webp", emptyList())
        store.savePack(pack1)
        store.savePack(pack2)
        
        val allPacks = store.loadAllPacks()
        
        assertEquals(2, allPacks.size)
        assertTrue(allPacks.any { it.id == "id1" })
        assertTrue(allPacks.any { it.id == "id2" })
    }
}
