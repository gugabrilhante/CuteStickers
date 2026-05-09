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
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class StickerRepositoryImplTest {

    private val imageProcessor = mockk<ImageProcessor>()
    private val fileManager = mockk<StickerFileManager>()
    private val stickerStore = mockk<StickerStore>()
    private val galleryDataSource = mockk<GalleryDataSource>()
    private val timeProvider = mockk<TimeProvider>()
    private val okHttpClient = mockk<OkHttpClient>()
    private val testDispatcher = UnconfinedTestDispatcher()

    private val repository = StickerRepositoryImpl(
        imageProcessor = imageProcessor,
        fileManager = fileManager,
        stickerStore = stickerStore,
        galleryDataSource = galleryDataSource,
        timeProvider = timeProvider,
        ioDispatcher = testDispatcher,
        okHttpClient = okHttpClient
    )

    @Test
    fun `createStickerFromUrl succeeds and saves pack`() = runTest {
        // Arrange
        val imageUrl = "https://example.com/image.webp"
        val mediaId = "123"
        val mediaType = MediaType.Static
        val packId = "cute_stickers_static"
        val packDir = File("mockDir")
        val mockFile = File("mockFile")
        
        every { fileManager.packDir(packId) } returns packDir
        coEvery { imageProcessor.downloadAndProcess(any(), any(), any()) } returns Result.success(mockFile)
        every { stickerStore.loadAllPacks() } returns emptyList()
        every { stickerStore.savePack(any()) } returns Unit

        // Act
        val result = repository.createStickerFromUrl(imageUrl, mediaId, mediaType)

        // Assert
        assertTrue(result.isSuccess)
        coVerify { imageProcessor.downloadAndProcess(imageUrl, any(), ImageProcessor.STICKER_SIZE) }
        coVerify { imageProcessor.downloadAndProcess(imageUrl, any(), ImageProcessor.TRAY_SIZE) }
        coVerify { stickerStore.savePack(match { it.id == packId }) }
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
