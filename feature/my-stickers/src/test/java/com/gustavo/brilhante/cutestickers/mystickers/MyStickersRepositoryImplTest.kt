package com.gustavo.brilhante.cutestickers.mystickers

import com.gustavo.brilhante.cutestickers.common.TimeProvider
import com.gustavo.brilhante.cutestickers.mystickers.data.MyStickerDao
import com.gustavo.brilhante.cutestickers.mystickers.data.MyStickerEntity
import com.gustavo.brilhante.cutestickers.mystickers.data.MyStickersRepositoryImpl
import com.gustavo.brilhante.cutestickers.mystickers.domain.SourceType
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import org.junit.Assert.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MyStickersRepositoryImplTest {

    private val context = mockk<android.content.Context>(relaxed = true)
    private val dao = mockk<MyStickerDao>()
    private val timeProvider = mockk<TimeProvider>()
    private val okHttpClient = mockk<OkHttpClient>()
    private val testDispatcher = UnconfinedTestDispatcher()

    private fun createRepository() = MyStickersRepositoryImpl(
        context = context,
        dao = dao,
        timeProvider = timeProvider,
        ioDispatcher = testDispatcher,
        okHttpClient = okHttpClient
    )

    @Test
    fun getStickersReturnsMappedDomainObjects() = runTest(testDispatcher) {
        val entities = listOf(
            MyStickerEntity("id1", "/path/img1.jpg", "GALLERY", 1000L),
            MyStickerEntity("id2", "/path/img2.jpg", "APP", 2000L)
        )
        every { dao.getStickers() } returns flowOf(entities)

        val repo = createRepository()
        val stickers = repo.getStickers().first()

        assertEquals(2, stickers.size)
        assertEquals("id1", stickers[0].id)
        assertEquals(SourceType.GALLERY, stickers[0].sourceType)
        assertEquals("id2", stickers[1].id)
        assertEquals(SourceType.APP, stickers[1].sourceType)
    }

    @Test
    fun deleteStickersCallsDaoDeleteById() = runTest(testDispatcher) {
        coEvery { dao.getById("id1") } returns null
        coEvery { dao.deleteById(any()) } just Runs
        every { context.filesDir } returns mockk(relaxed = true)

        val repo = createRepository()
        val result = repo.deleteSticker("id1")

        assertTrue(result.isSuccess)
        coVerify { dao.deleteById("id1") }
    }

    // Bug 3: images from My Stickers must be readable when converted to WhatsApp stickers.
    // The EXIF normalization runs on Android framework classes (BitmapFactory/ExifInterface) so
    // it lives in instrumented tests. Here we verify saveFromUrl can read a local file path,
    // which is the prerequisite for the sticker pipeline to receive already-normalised bytes.
    @Test
    fun saveFromUrl_withLocalFilePath_savesSticker() = runTest(testDispatcher) {
        val tmpDir = java.io.File(System.getProperty("java.io.tmpdir")!!)
        val sourceFile = java.io.File.createTempFile("source", ".jpg").also { it.writeBytes(byteArrayOf(1, 2, 3)) }
        every { context.filesDir } returns tmpDir
        every { timeProvider.getCurrentTimeMillis() } returns 1000L
        coEvery { dao.insert(any()) } just Runs

        val repo = createRepository()
        val result = repo.saveFromUrl(sourceFile.absolutePath, "test-media-id")

        assertTrue(result.isSuccess)
        assertEquals("test-media-id", result.getOrNull()?.id)
        coVerify { dao.insert(any()) }

        sourceFile.delete()
        java.io.File(tmpDir, "my-stickers/test-media-id.jpg").delete()
        java.io.File(tmpDir, "my-stickers").delete()
    }

    @Test
    fun deleteStickersDeletesLocalFile() = runTest(testDispatcher) {
        val tempFile = java.io.File.createTempFile("sticker", ".jpg").also { it.writeText("data") }
        val entity = MyStickerEntity("id1", tempFile.absolutePath, "GALLERY", 1000L)
        coEvery { dao.getById("id1") } returns entity
        coEvery { dao.deleteById(any()) } just Runs

        val repo = createRepository()
        repo.deleteSticker("id1")

        assertTrue(!tempFile.exists())
    }
}
