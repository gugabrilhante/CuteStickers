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
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
internal class StickerIntegrationTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

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
        val packId = "static_pack"
        val packDir = tempFolder.newFolder(packId)
        
        every { fileManager.packDir(any()) } returns packDir
        every { imageProcessor.downloadAndProcess(any(), any(), any(), any()) } answers {
            val file = it.invocation.args[1] as File
            file.createNewFile()
            Result.success(file)
        }
        every { timeProvider.getCurrentTimeMillis() } returns 123456789L

        // Act
        val result = repository.createStickerFromUrl(imageUrl, mediaId, MediaType.Static)

        // Assert
        assertTrue("Result should be success but was ${result.exceptionOrNull()}", result.isSuccess)
        val packs = stickerStore.loadAllPacks()
        assertEquals(1, packs.size)
        assertEquals(packId, packs[0].id)
        // WhatsApp requirement adds placeholders if < 3
        assertEquals(3, packs[0].stickers.size)
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
        override fun migrateIfNeeded() {}
    }
}
