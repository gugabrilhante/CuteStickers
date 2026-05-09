package com.gustavo.brilhante.cutestickers.stickers.data

import com.gustavo.brilhante.cutestickers.common.TimeProvider
import com.gustavo.brilhante.cutestickers.model.MediaType
import com.gustavo.brilhante.cutestickers.stickers.domain.StickerRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class StickerIntegrationTest {

    private val imageProcessor = mockk<ImageProcessor>()
    private val fileManager = mockk<StickerFileManager>()
    private val timeProvider = mockk<TimeProvider>()
    private val downloader = mockk<StickerDownloader>()
    private val okHttpClient = mockk<OkHttpClient>()
    
    private lateinit var stickerStore: FakeStickerStore
    private lateinit var repository: StickerRepository

    @Before
    fun setup() {
        stickerStore = FakeStickerStore()
        repository = StickerRepositoryImpl(
            imageProcessor = imageProcessor,
            fileManager = fileManager,
            stickerStore = stickerStore,
            galleryDataSource = mockk(),
            timeProvider = timeProvider,
            ioDispatcher = UnconfinedTestDispatcher(),
            okHttpClient = okHttpClient
        )
    }

    @Test
    fun `creating sticker updates the store with new pack info`() = runTest {
        // Arrange
        val imageUrl = "https://example.com/cat.jpg"
        val mediaId = "cat123"
        val packId = "cute_stickers_static"
        
        every { fileManager.packDir(packId) } returns File("mock")
        coEvery { imageProcessor.downloadAndProcess(any(), any(), any()) } returns Result.success(File("mock"))

        // Act
        val result = repository.createStickerFromUrl(imageUrl, mediaId, MediaType.Static)

        // Assert
        assertTrue(result.isSuccess)
        val packs = stickerStore.loadAllPacks()
        assertEquals(1, packs.size)
        assertEquals(packId, packs[0].id)
        assertEquals(1, packs[0].stickers.size)
        assertEquals("${mediaId}.webp", packs[0].stickers[0].imageFileName)
    }

    class FakeStickerStore : StickerStore {
        private val packs = mutableMapOf<String, StickerPackInfo>()
        override val stickersRoot: File = File("fake")

        override fun getPackVersion(packId: String): String = "1"
        override fun savePack(pack: StickerPackInfo) { packs[pack.id] = pack }
        override fun loadAllPacks(): List<StickerPackInfo> = packs.values.toList()
        override fun loadStickers(packId: String): List<StickerInfo> = packs[packId]?.stickers ?: emptyList()
        override fun getStickerFile(packId: String, fileName: String): File = File("fake/$packId/$fileName")
    }
}
