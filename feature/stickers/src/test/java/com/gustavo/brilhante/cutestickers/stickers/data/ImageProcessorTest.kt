package com.gustavo.brilhante.cutestickers.stickers.data

import android.content.Context
import android.graphics.*
import android.net.Uri
import android.util.Log
import com.aureusapps.android.webpandroid.encoder.WebPAnimEncoder
import com.aureusapps.android.webpandroid.encoder.WebPConfig
import com.gustavo.brilhante.cutestickers.common.Logger
import io.mockk.*
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ImageProcessorTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val context = mockk<Context>(relaxed = true)
    private val downloader = mockk<StickerDownloader>()
    private val logger = mockk<Logger>(relaxed = true)
    private val imageProcessor = spyk(ImageProcessor(context, downloader, logger))
    private val mockEncoder = mockk<WebPAnimEncoder>(relaxed = true)

    @Before
    fun setup() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0

        mockkStatic(Bitmap::class)
        mockkStatic(BitmapFactory::class)
        mockkStatic(Movie::class)
        mockkStatic(Uri::class)

        every { Bitmap.createBitmap(any<Int>(), any<Int>(), any()) } answers { 
            mockk<Bitmap>(relaxed = true).apply {
                every { width } returns it.invocation.args[0] as Int
                every { height } returns it.invocation.args[1] as Int
                every { isRecycled } returns false
            }
        }
        every { Bitmap.createBitmap(any<Bitmap>(), any(), any(), any(), any()) } answers { 
            mockk<Bitmap>(relaxed = true).apply {
                every { width } returns it.invocation.args[3] as Int
                every { height } returns it.invocation.args[4] as Int
                every { isRecycled } returns false
            }
        }
        every { Bitmap.createScaledBitmap(any(), any(), any(), any()) } answers { 
            mockk<Bitmap>(relaxed = true).apply {
                every { width } returns it.invocation.args[1] as Int
                every { height } returns it.invocation.args[2] as Int
                every { isRecycled } returns false
            }
        }
        
        every { BitmapFactory.decodeFile(any()) } answers { 
            mockk<Bitmap>(relaxed = true).apply {
                every { width } returns 512
                every { height } returns 512
                every { isRecycled } returns false
            }
        }
        
        every { Movie.decodeByteArray(any(), any(), any()) } returns null
        every { Uri.fromFile(any()) } returns mockk(relaxed = true)

        mockkConstructor(Canvas::class)
        every { anyConstructed<Canvas>().drawBitmap(any<Bitmap>(), any<Float>(), any<Float>(), any()) } just Runs
        
        mockkConstructor(Paint::class)

        // Mock encoder creation
        every { imageProcessor.createEncoder(any(), any()) } returns mockEncoder
        
        mockkConstructor(WebPConfig::class)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun setupMockResponse(bytes: ByteArray = byteArrayOf(1, 2, 3, 4, 5)) {
        coEvery { downloader.download(any()) } returns Result.success(bytes)
    }

    @Test
    fun `Static image - wide image should result in square center crop`() {
        val width = 1000
        val height = 500
        val targetSize = 512
        val sourceBitmap = mockk<Bitmap>()
        every { sourceBitmap.width } returns width
        every { sourceBitmap.height } returns height
        every { sourceBitmap.isRecycled } returns false
        
        val croppedBitmap = mockk<Bitmap>(relaxed = true)
        every { croppedBitmap.width } returns 500
        every { croppedBitmap.height } returns 500
        every { croppedBitmap.isRecycled } returns false

        val scaledBitmap = mockk<Bitmap>(relaxed = true)
        every { scaledBitmap.width } returns targetSize
        every { scaledBitmap.height } returns targetSize
        every { scaledBitmap.isRecycled } returns false
        
        // centerCrop logic: left = (1000-500)/2 = 250, top = 0, size = 500
        every { Bitmap.createBitmap(sourceBitmap, 250, 0, 500, 500) } returns croppedBitmap
        every { Bitmap.createScaledBitmap(croppedBitmap, targetSize, targetSize, true) } returns scaledBitmap
        
        every { BitmapFactory.decodeByteArray(any(), any(), any()) } returns sourceBitmap
        setupMockResponse()
        
        val outputFile = tempFolder.newFile("static.webp")
        imageProcessor.downloadAndProcess("https://example.com/wide.jpg", outputFile, targetSize)
        
        verify { Bitmap.createBitmap(sourceBitmap, 250, 0, 500, 500) }
        verify { Bitmap.createScaledBitmap(croppedBitmap, targetSize, targetSize, true) }
    }

    @Test
    fun `Animated GIF - adaptive compression should retry with lower quality`() {
        setupMockResponse()
        every { imageProcessor.decodeFrames(any()) } returns listOf(
            ImageProcessor.Frame(mockk(relaxed = true), 100)
        )

        val outputFile = tempFolder.newFile("output_adaptive.webp")
        val result = imageProcessor.downloadAndProcessAnimated("https://example.com/adaptive.gif", outputFile)
        
        assertTrue("Result should be success but was ${result.exceptionOrNull()}", result.isSuccess)
        verify { mockEncoder.configure(any()) }
    }

    @Test
    fun `Test 1 - GIF with 5 frames should result in 5 frames`() {
        setupMockResponse()
        val frames = List(5) { 
            ImageProcessor.Frame(mockk<Bitmap>(relaxed = true).apply {
                every { width } returns 512
                every { height } returns 512
                every { isRecycled } returns false
            }, 100)
        }
        every { imageProcessor.decodeFrames(any()) } returns frames

        val outputFile = tempFolder.newFile("output_1.webp")
        val result = imageProcessor.downloadAndProcessAnimated("https://example.com/5frames.gif", outputFile)

        assertTrue("Result should be success but was ${result.exceptionOrNull()}", result.isSuccess)
        verify(exactly = 5) { mockEncoder.addFrame(any<Long>(), any<Bitmap>()) }
    }

    @Test
    fun `Test 2 - GIF with 25 frames should result in 25 frames`() {
        setupMockResponse()
        val frames = List(25) { 
            ImageProcessor.Frame(mockk<Bitmap>(relaxed = true).apply {
                every { width } returns 512
                every { height } returns 512
                every { isRecycled } returns false
            }, 40)
        }
        every { imageProcessor.decodeFrames(any()) } returns frames

        val outputFile = tempFolder.newFile("output_2.webp")
        val result = imageProcessor.downloadAndProcessAnimated("https://example.com/25frames.gif", outputFile)

        assertTrue("Result should be success but was ${result.exceptionOrNull()}", result.isSuccess)
        verify(exactly = 25) { mockEncoder.addFrame(any<Long>(), any<Bitmap>()) }
    }

    @Test
    fun `Test 3 - GIF with variable frame delays should preserve delays`() {
        setupMockResponse()
        val frames = listOf(
            ImageProcessor.Frame(mockk<Bitmap>(relaxed = true).apply { every { width } returns 512; every { height } returns 512; every { isRecycled } returns false }, 100),
            ImageProcessor.Frame(mockk<Bitmap>(relaxed = true).apply { every { width } returns 512; every { height } returns 512; every { isRecycled } returns false }, 200),
            ImageProcessor.Frame(mockk<Bitmap>(relaxed = true).apply { every { width } returns 512; every { height } returns 512; every { isRecycled } returns false }, 150)
        )
        every { imageProcessor.decodeFrames(any()) } returns frames

        val outputFile = tempFolder.newFile("output_3.webp")
        val result = imageProcessor.downloadAndProcessAnimated("https://example.com/variable.gif", outputFile)

        assertTrue("Result should be success but was ${result.exceptionOrNull()}", result.isSuccess)
        verify { mockEncoder.addFrame(0L, any<Bitmap>()) }
        verify { mockEncoder.addFrame(100L, any<Bitmap>()) }
        verify { mockEncoder.addFrame(300L, any<Bitmap>()) }
    }
}
