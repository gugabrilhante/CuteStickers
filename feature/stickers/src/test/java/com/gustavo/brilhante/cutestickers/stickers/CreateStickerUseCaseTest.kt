package com.gustavo.brilhante.cutestickers.stickers

import com.gustavo.brilhante.cutestickers.stickers.domain.CreateStickerUseCase
import com.gustavo.brilhante.cutestickers.stickers.domain.StickerItem
import com.gustavo.brilhante.cutestickers.stickers.domain.StickerPack
import com.gustavo.brilhante.cutestickers.stickers.domain.StickerRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CreateStickerUseCaseTest {

    private val repository = mockk<StickerRepository>()
    private val useCase = CreateStickerUseCase(repository)

    @Test
    fun `invoke returns success when repository succeeds`() = runTest {
        val expected = StickerPack(
            id = "pack_abc",
            name = "CuteStickers",
            publisher = "CuteStickers App",
            trayImageFileName = "tray_icon.webp",
            stickers = listOf(StickerItem("abc.webp", listOf("😊")))
        )
        coEvery { repository.createStickerFromUrl("https://img.com/cat.jpg", "abc") } returns
            Result.success(expected)

        val result = useCase("https://img.com/cat.jpg", "abc")

        assertTrue(result.isSuccess)
        assertEquals(expected, result.getOrNull())
        coVerify(exactly = 1) { repository.createStickerFromUrl("https://img.com/cat.jpg", "abc") }
    }

    @Test
    fun `invoke returns failure when repository throws`() = runTest {
        val error = RuntimeException("Network error")
        coEvery { repository.createStickerFromUrl(any(), any()) } returns Result.failure(error)

        val result = useCase("https://img.com/cat.jpg", "abc")

        assertTrue(result.isFailure)
        assertEquals("Network error", result.exceptionOrNull()?.message)
    }

    @Test
    fun `invoke propagates exact error from repository`() = runTest {
        val error = IllegalStateException("Storage full")
        coEvery { repository.createStickerFromUrl(any(), any()) } returns Result.failure(error)

        val result = useCase("url", "id")

        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }
}
