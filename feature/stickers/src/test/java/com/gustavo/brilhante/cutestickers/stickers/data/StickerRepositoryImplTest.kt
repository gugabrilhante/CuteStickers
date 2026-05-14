package com.gustavo.brilhante.cutestickers.stickers.data

import com.gustavo.brilhante.cutestickers.common.TimeProvider
import com.gustavo.brilhante.cutestickers.model.MediaType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class StickerRepositoryImplTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val imageProcessor = mockk<ImageProcessor>()
    private val fileManager = mockk<StickerFileManager>()
    private val stickerStore = mockk<StickerStore>(relaxed = true)
    private val galleryDataSource = mockk<GalleryDataSource>()
    private val timeProvider = mockk<TimeProvider>()
    private val okHttpClient = mockk<OkHttpClient>()
    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var repository: StickerRepositoryImpl

    @Before
    fun setup() {
        repository = StickerRepositoryImpl(
            imageProcessor = imageProcessor,
            fileManager = fileManager,
            stickerStore = stickerStore,
            galleryDataSource = galleryDataSource,
            timeProvider = timeProvider,
            ioDispatcher = testDispatcher,
            okHttpClient = okHttpClient
        )
    }

    @Test
    fun `createStickerFromUrl succeeds and saves pack`() = runTest {
        // Arrange
        val imageUrl = "https://example.com/image.webp"
        val mediaId = "123"
        val mediaType = MediaType.Static
        val packId = "static_pack"
        val packDir = tempFolder.newFolder(packId)
        
        every { fileManager.packDir(any()) } returns packDir
        every { imageProcessor.downloadAndProcess(any(), any(), any(), any()) } answers {
            val file = it.invocation.args[1] as File
            file.createNewFile()
            Result.success(file)
        }
        every { stickerStore.loadAllPacks() } returns emptyList()
        every { timeProvider.getCurrentTimeMillis() } returns 123456789L

        // Act
        val result = repository.createStickerFromUrl(imageUrl, mediaId, mediaType)

        // Assert
        assertTrue("Result should be success", result.isSuccess)
        coVerify { imageProcessor.downloadAndProcess(imageUrl, any(), ImageProcessor.STICKER_SIZE, any()) }
        coVerify { stickerStore.savePack(match { it.id == packId }) }
    }

    @Test
    fun `createStickerFromUrl adds placeholders if less than 3 stickers`() = runTest {
        // Arrange
        val imageUrl = "https://example.com/image.webp"
        val mediaId = "123"
        val packId = "static_pack"
        val packDir = tempFolder.newFolder(packId)
        
        every { fileManager.packDir(any()) } returns packDir
        every { imageProcessor.downloadAndProcess(any(), any(), any(), any()) } answers {
            val file = it.invocation.args[1] as File
            file.createNewFile()
            Result.success(file)
        }
        every { stickerStore.loadAllPacks() } returns emptyList()
        val capturedPack = mutableListOf<StickerPackInfo>()
        every { stickerStore.savePack(capture(capturedPack)) } returns Unit
        every { timeProvider.getCurrentTimeMillis() } returns 123456789L

        // Act
        repository.createStickerFromUrl(imageUrl, mediaId, MediaType.Static)

        // Assert
        val pack = capturedPack.first()
        assertEquals(3, pack.stickers.size)
        assertTrue(pack.stickers[0].imageFileName == "123.webp")
        assertTrue(pack.stickers[1].imageFileName == "placeholder_1.webp")
        assertTrue(File(packDir, "placeholder_1.webp").exists())
    }

    @Test
    fun `saveMediaToGallery uses timeProvider and galleryDataSource`() = runTest {
        // Arrange
        val imageUrl = "https://example.com/image.jpg"
        val currentTime = 123456789L
        val mockResponse = mockk<okhttp3.Response>(relaxed = true)
        val mockCall = mockk<okhttp3.Call>()
        val mockResponseBody = mockk<okhttp3.ResponseBody>()

        every { timeProvider.getCurrentTimeMillis() } returns currentTime
        every { okHttpClient.newCall(any()) } returns mockCall
        every { mockCall.execute() } returns mockResponse
        every { mockResponse.isSuccessful } returns true
        every { mockResponse.body } returns mockResponseBody
        every { mockResponseBody.bytes() } returns byteArrayOf(1, 2, 3)
        coEvery { galleryDataSource.saveImage(any(), any(), any()) } returns Result.success(Unit)

        // Act
        val result = repository.saveMediaToGallery(imageUrl)

        // Assert
        assertTrue(result.isSuccess)
        coVerify { timeProvider.getCurrentTimeMillis() }
        coVerify { galleryDataSource.saveImage(any(), "cutesticker_$currentTime.jpg", "image/jpeg") }
    }
}
