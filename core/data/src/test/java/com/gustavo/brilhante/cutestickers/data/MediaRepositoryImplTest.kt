package com.gustavo.brilhante.cutestickers.data

import com.gustavo.brilhante.cutestickers.common.TimeProvider
import com.gustavo.brilhante.cutestickers.database.CacheMetadataDao
import com.gustavo.brilhante.cutestickers.database.CacheMetadataEntity
import com.gustavo.brilhante.cutestickers.network.MediaService
import com.gustavo.brilhante.cutestickers.network.model.NetworkMediaItem
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MediaRepositoryImplTest {

    private val mediaService = mockk<MediaService>()
    private val localDataSource = mockk<MediaLocalDataSource>(relaxed = true)
    private val cacheMetadataDao = mockk<CacheMetadataDao>(relaxed = true)
    private val paginationSession = PaginationSession()
    private val timeProvider = mockk<TimeProvider>()
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var repository: MediaRepositoryImpl

    @Before
    fun setup() {
        repository = MediaRepositoryImpl(
            mediaService = mediaService,
            localDataSource = localDataSource,
            cacheMetadataDao = cacheMetadataDao,
            paginationSession = paginationSession,
            timeProvider = timeProvider,
            featureKey = "test",
            ioDispatcher = testDispatcher
        )
    }

    @Test
    fun `getMedia should refresh when cache is expired`() = runTest(testDispatcher) {
        // Arrange
        val currentTime = 1000000L
        val expiredTime = currentTime - (11 * 60 * 1000L) // 11 mins ago
        every { timeProvider.getCurrentTimeMillis() } returns currentTime
        coEvery { cacheMetadataDao.getMetadata("test") } returns CacheMetadataEntity("test", expiredTime)
        coEvery { mediaService.getMedia(any(), any()) } returns listOf(NetworkMediaItem("1", "url1"))
        every { localDataSource.getMedia() } returns flowOf(emptyList())

        // Act
        repository.getMedia().first()

        // Assert
        coVerify { mediaService.getMedia(limit = 20, page = 0) }
        coVerify { localDataSource.clear() }
        coVerify { localDataSource.insertAll(any()) }
    }

    @Test
    fun `getMedia should NOT refresh when cache is young`() = runTest(testDispatcher) {
        // Arrange
        val currentTime = 1000000L
        val youngTime = currentTime - (5 * 60 * 1000L) // 5 mins ago
        every { timeProvider.getCurrentTimeMillis() } returns currentTime
        coEvery { cacheMetadataDao.getMetadata("test") } returns CacheMetadataEntity("test", youngTime)
        every { localDataSource.getMedia() } returns flowOf(emptyList())

        // Act
        repository.getMedia().first()

        // Assert
        coVerify(exactly = 0) { mediaService.getMedia(any(), any()) }
    }

    @Test
    fun `loadNextPage should increment page and session count`() = runTest(testDispatcher) {
        // Arrange
        coEvery { cacheMetadataDao.getMetadata("test") } returns CacheMetadataEntity("test", 1000L, nextPage = 2)
        coEvery { mediaService.getMedia(any(), any()) } returns listOf(NetworkMediaItem("2", "url2"))

        // Act
        repository.loadNextPage()

        // Assert
        coVerify { mediaService.getMedia(limit = 20, page = 2) }
        coVerify { cacheMetadataDao.insertMetadata(match { it.nextPage == 3 }) }
        assertEquals(1, paginationSession.getExtraPagesCount("test"))
    }

    @Test
    fun `loadNextPage should stop after 4 extra pages`() = runTest(testDispatcher) {
        // Arrange
        coEvery { cacheMetadataDao.getMetadata("test") } returns CacheMetadataEntity("test", 1000L, nextPage = 2)
        coEvery { mediaService.getMedia(any(), any()) } returns listOf(NetworkMediaItem("2", "url2"))

        // Act - 5 attempts
        repeat(5) { repository.loadNextPage() }

        // Assert
        coVerify(exactly = 4) { mediaService.getMedia(any(), any()) }
        assertEquals(4, paginationSession.getExtraPagesCount("test"))
    }

    @Test
    fun `refresh should reset session count`() = runTest(testDispatcher) {
        // Arrange
        paginationSession.incrementPageCount("test")
        coEvery { mediaService.getMedia(any(), any()) } returns emptyList()
        every { timeProvider.getCurrentTimeMillis() } returns 2000L

        // Act
        repository.refresh()

        // Assert
        assertEquals(0, paginationSession.getExtraPagesCount("test"))
    }
}
