package com.gustavo.brilhante.cutecats.feature.stickers

import com.gustavo.brilhante.cutecats.feature.stickers.domain.SaveMediaUseCase
import com.gustavo.brilhante.cutecats.feature.stickers.domain.StickerRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class SaveMediaUseCaseTest {

    private val repository = mockk<StickerRepository>()
    private val useCase = SaveMediaUseCase(repository)

    @Test
    fun `invoke returns success when repository saves successfully`() = runTest {
        coEvery { repository.saveMediaToGallery("https://img.com/cat.jpg") } returns
            Result.success(Unit)

        val result = useCase("https://img.com/cat.jpg")

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { repository.saveMediaToGallery("https://img.com/cat.jpg") }
    }

    @Test
    fun `invoke returns failure when repository fails`() = runTest {
        val error = RuntimeException("Permission denied")
        coEvery { repository.saveMediaToGallery(any()) } returns Result.failure(error)

        val result = useCase("https://img.com/cat.jpg")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is RuntimeException)
    }

    @Test
    fun `invoke delegates url to repository unchanged`() = runTest {
        val url = "https://cdn.example.com/image/12345.gif"
        coEvery { repository.saveMediaToGallery(url) } returns Result.success(Unit)

        useCase(url)

        coVerify { repository.saveMediaToGallery(url) }
    }
}
