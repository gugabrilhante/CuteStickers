package com.gustavo.brilhante.cutestickers.stickers.data

import com.gustavo.brilhante.cutestickers.common.PackageManagerWrapper
import com.gustavo.brilhante.cutestickers.stickers.domain.StickerItem
import com.gustavo.brilhante.cutestickers.stickers.domain.StickerPack
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class WhatsAppStickerExporterTest {

    private val packageManagerWrapper = mockk<PackageManagerWrapper>()
    private val stickerStore = mockk<StickerStore>()
    private val appPackageName = "com.test.app"

    private lateinit var exporter: WhatsAppStickerExporter

    @Before
    fun setup() {
        exporter = WhatsAppStickerExporter(
            appPackageName = appPackageName,
            packageManagerWrapper = packageManagerWrapper,
            stickerStore = stickerStore
        )
    }

    @Test
    fun `isWhatsAppInstalled returns true if any whatsapp package exists`() {
        every { packageManagerWrapper.isPackageInstalled("com.whatsapp") } returns false
        every { packageManagerWrapper.isPackageInstalled("com.whatsapp.w4b") } returns true
        
        assertTrue(exporter.isWhatsAppInstalled())
    }

    @Test
    fun `getExportMetadata returns success when whatsapp is installed and files exist`() {
        val pack = StickerPack(
            id = "pack1",
            name = "Pack 1",
            publisher = "Pub",
            trayImageFileName = "tray.webp",
            stickers = listOf(StickerItem("s1.webp", emptyList())),
            isAnimated = false
        )
        val trayFile = mockk<File>()
        val stickerFile = mockk<File>()
        
        every { packageManagerWrapper.isPackageInstalled("com.whatsapp") } returns true
        every { stickerStore.getStickerFile("pack1", "tray.webp") } returns trayFile
        every { stickerStore.getStickerFile("pack1", "s1.webp") } returns stickerFile
        every { trayFile.exists() } returns true
        every { stickerFile.exists() } returns true
        every { stickerFile.length() } returns 1024L

        val result = exporter.getExportMetadata(pack)

        assertTrue(result.isSuccess)
        val metadata = result.getOrThrow()
        assertEquals("com.whatsapp", metadata.targetPackage)
        assertEquals("$appPackageName.StickerContentProvider", metadata.authority)
    }

    @Test
    fun `getExportMetadata returns failure when files are missing`() {
        val pack = StickerPack("p1", "N", "P", "tray.webp", listOf(StickerItem("s1.webp", emptyList())), false)
        
        every { packageManagerWrapper.isPackageInstalled(any()) } returns true
        every { stickerStore.getStickerFile(any(), any()) } returns mockk<File> {
            every { exists() } returns false
        }

        val result = exporter.getExportMetadata(pack)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Tray icon missing") == true)
    }
}
