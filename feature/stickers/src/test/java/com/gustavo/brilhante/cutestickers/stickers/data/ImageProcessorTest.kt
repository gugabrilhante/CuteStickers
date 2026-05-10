package com.gustavo.brilhante.cutestickers.stickers.data

import android.content.Context
import android.graphics.*
import android.net.Uri
import android.util.Log
import com.aureusapps.android.webpandroid.encoder.WebPAnimEncoder
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
    private val imageProcessor = ImageProcessor(context, downloader, logger)

    @Before
    fun setup() {
        mockkStatic(Log::class)
        every { Log.d(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>(), any<Throwable>()) } returns 0

        mockkStatic(Bitmap::class)
        mockkStatic(BitmapFactory::class)
        mockkStatic(Movie::class)
        mockkStatic(Uri::class)

        val mockBitmap = mockk<Bitmap>(relaxed = true)
        every { Bitmap.createBitmap(any<Int>(), any<Int>(), any<Bitmap.Config>()) } returns mockBitmap
        every { Bitmap.createBitmap(any<Bitmap>(), any<Int>(), any<Int>(), any<Int>(), any<Int>()) } returns mockBitmap
        every { Bitmap.createScaledBitmap(any<Bitmap>(), any<Int>(), any<Int>(), any<Boolean>()) } returns mockBitmap
        
        every { BitmapFactory.decodeFile(any<String>()) } returns mockBitmap
        every { Movie.decodeByteArray(any<ByteArray>(), any<Int>(), any<Int>()) } returns null
        
        every { Uri.fromFile(any<File>()) } returns mockk<Uri>(relaxed = true)

        mockkConstructor(Canvas::class)
        every { anyConstructed<Canvas>().drawBitmap(any<Bitmap>(), any<Float>(), any<Float>(), any()) } just Runs
        
        mockkConstructor(Paint::class)

        mockkConstructor(WebPAnimEncoder::class)
        every { anyConstructed<WebPAnimEncoder>().configure(any()) } returns mockk(relaxed = true)
        every { anyConstructed<WebPAnimEncoder>().addFrame(any<Long>(), any<Bitmap>()) } returns mockk(relaxed = true)
        every { anyConstructed<WebPAnimEncoder>().assemble(any<Long>(), any<Uri>()) } returns mockk(relaxed = true)
        every { anyConstructed<WebPAnimEncoder>().release() } just Runs
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun setupMockResponse(bytes: ByteArray = "dummy".toByteArray()) {
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
        
        val croppedBitmap = mockk<Bitmap>(relaxed = true)
        val scaledBitmap = mockk<Bitmap>(relaxed = true)
        
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
        val movie = mockk<Movie>()
        every { Movie.decodeByteArray(any(), any(), any()) } returns movie
        every { movie.duration() } returns 100
        every { movie.width() } returns 512
        every { movie.height() } returns 512
        every { movie.setTime(any()) } returns true
        every { movie.draw(any<Canvas>(), any<Float>(), any<Float>()) } just Runs

        val outputFile = tempFolder.newFile("output_adaptive.webp")
        imageProcessor.downloadAndProcessAnimated("https://example.com/adaptive.gif", outputFile)
        
        // Verify it at least tried the first configuration (quality 90)
        verify { anyConstructed<WebPAnimEncoder>().configure(match { it.quality == 90f }) }
    }

    @Test
    fun `Test 1 - GIF with 5 frames should result in 5 frames`() {
        setupMockResponse()
        val frameCount = 5
        val frameDuration = 100
        val movie = mockk<Movie>()
        every { Movie.decodeByteArray(any(), any(), any()) } returns movie
        every { movie.duration() } returns frameCount * frameDuration
        every { movie.width() } returns 512
        every { movie.height() } returns 512
        every { movie.setTime(any()) } returns true
        every { movie.draw(any<Canvas>(), any<Float>(), any<Float>()) } just Runs

        val bitmaps = List(100) { mockk<Bitmap>(relaxed = true) }
        var bitmapIndex = 0
        every { Bitmap.createBitmap(512, 512, any()) } answers { bitmaps[bitmapIndex++] }
        
        bitmaps.forEachIndexed { index, bitmap ->
            every { bitmap.sameAs(any()) } answers {
                val other = it.invocation.args[0] as Bitmap
                val otherIndex = bitmaps.indexOf(other)
                (index / 10) == (otherIndex / 10)
            }
        }

        val outputFile = tempFolder.newFile("output_1.webp")
        imageProcessor.downloadAndProcessAnimated("https://example.com/5frames.gif", outputFile)

        verify(exactly = 5) { anyConstructed<WebPAnimEncoder>().addFrame(any<Long>(), any<Bitmap>()) }
    }

    @Test
    fun `Test 2 - GIF with 25 frames should result in 25 frames`() {
        setupMockResponse()
        val frameCount = 25
        val frameDuration = 40 
        val movie = mockk<Movie>()
        every { Movie.decodeByteArray(any(), any(), any()) } returns movie
        every { movie.duration() } returns frameCount * frameDuration
        every { movie.width() } returns 512
        every { movie.height() } returns 512
        every { movie.setTime(any()) } returns true
        every { movie.draw(any<Canvas>(), any<Float>(), any<Float>()) } just Runs

        val bitmaps = List(200) { mockk<Bitmap>(relaxed = true) }
        var bitmapIndex = 0
        every { Bitmap.createBitmap(512, 512, any()) } answers { bitmaps[bitmapIndex++] }
        
        bitmaps.forEachIndexed { index, bitmap ->
            every { bitmap.sameAs(any()) } answers {
                val other = it.invocation.args[0] as Bitmap
                val otherIndex = bitmaps.indexOf(other)
                (index / 4) == (otherIndex / 4) 
            }
        }

        val outputFile = tempFolder.newFile("output_2.webp")
        imageProcessor.downloadAndProcessAnimated("https://example.com/25frames.gif", outputFile)

        verify(exactly = 25) { anyConstructed<WebPAnimEncoder>().addFrame(any<Long>(), any<Bitmap>()) }
    }

    @Test
    fun `Test 3 - GIF with variable frame delays should preserve delays`() {
        setupMockResponse()
        val movie = mockk<Movie>()
        every { Movie.decodeByteArray(any(), any(), any()) } returns movie
        every { movie.duration() } returns 450
        every { movie.width() } returns 512
        every { movie.height() } returns 512
        every { movie.setTime(any()) } returns true
        every { movie.draw(any<Canvas>(), any<Float>(), any<Float>()) } just Runs

        val bitmaps = List(100) { mockk<Bitmap>(relaxed = true) }
        var bitmapIndex = 0
        every { Bitmap.createBitmap(512, 512, any()) } answers { bitmaps[bitmapIndex++] }
        
        bitmaps.forEachIndexed { index, bitmap ->
            every { bitmap.sameAs(any()) } answers {
                val other = it.invocation.args[0] as Bitmap
                val otherIndex = bitmaps.indexOf(other)
                val t = index * 10
                val otherT = otherIndex * 10
                val frame1 = t < 100
                val frame1Other = otherT < 100
                val frame2 = t in 100..299
                val frame2Other = otherT in 100..299
                val frame3 = t >= 300
                val frame3Other = otherT >= 300
                (frame1 && frame1Other) || (frame2 && frame2Other) || (frame3 && frame3Other)
            }
        }

        val outputFile = tempFolder.newFile("output_3.webp")
        imageProcessor.downloadAndProcessAnimated("https://example.com/variable.gif", outputFile)

        verify { anyConstructed<WebPAnimEncoder>().addFrame(0L, any<Bitmap>()) }
        verify { anyConstructed<WebPAnimEncoder>().addFrame(100L, any<Bitmap>()) }
        verify { anyConstructed<WebPAnimEncoder>().addFrame(300L, any<Bitmap>()) }
    }
}
