package com.gustavo.brilhante.cutestickers.data

import android.content.res.AssetManager
import com.gustavo.brilhante.cutestickers.common.TimeProvider
import com.gustavo.brilhante.cutestickers.common.ToastManager
import com.gustavo.brilhante.cutestickers.common.network.NetworkMonitor
import com.gustavo.brilhante.cutestickers.database.CacheMetadataDao
import com.gustavo.brilhante.cutestickers.database.CacheMetadataEntity
import com.gustavo.brilhante.cutestickers.network.MediaService
import com.gustavo.brilhante.cutestickers.network.model.NetworkMediaItem
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream

@OptIn(ExperimentalCoroutinesApi::class)
class MediaRepositoryImplTest {

    private val mediaService = mockk<MediaService>()
    private val localDataSource = mockk<MediaLocalDataSource>(relaxed = true)
    private val cacheMetadataDao = mockk<CacheMetadataDao>(relaxed = true)
    private val paginationSession = PaginationSession()
    private val timeProvider = mockk<TimeProvider>()
    private val networkMonitor = mockk<NetworkMonitor>()
    private val toastManager = mockk<ToastManager>(relaxed = true)
    private val assetManager = mockk<AssetManager>()
    private val json = Json { ignoreUnknownKeys = true }
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
            networkMonitor = networkMonitor,
            toastManager = toastManager,
            assetManager = assetManager,
            json = json,
            featureKey = "test",
            seedFiles = listOf("seed.json"),
            ioDispatcher = testDispatcher
        )
    }

    @Test
    fun `refresh - API success - should update local storage`() = runTest(testDispatcher) {
        // Arrange
        every { networkMonitor.isOnline } returns flowOf(true)
        every { timeProvider.getCurrentTimeMillis() } returns 1000L
        coEvery { mediaService.getMedia(any(), any()) } returns listOf(NetworkMediaItem("1", "url1"))

        // Act
        repository.refresh()

        // Assert
        coVerify { mediaService.getMedia(limit = 20, page = 0) }
        coVerify { localDataSource.clear() }
        coVerify { localDataSource.insertAll(any()) }
    }

    @Test
    fun `refresh - API fail + Room success - should show toast and NOT load assets`() = runTest(testDispatcher) {
        // Arrange
        every { networkMonitor.isOnline } returns flowOf(true)
        every { timeProvider.getCurrentTimeMillis() } returns 1000L
        coEvery { mediaService.getMedia(any(), any()) } throws Exception("Network error")
        coEvery { localDataSource.isEmpty() } returns false

        // Act
        repository.refresh()

        // Assert
        coVerify { toastManager.showToast("Falha ao atualizar. Exibindo conteúdo salvo.") }
        coVerify(exactly = 0) { assetManager.open(any()) }
    }

    @Test
    fun `refresh - API fail + Room empty + Seed success - should show toast and load assets`() = runTest(testDispatcher) {
        // Arrange
        every { networkMonitor.isOnline } returns flowOf(true)
        every { timeProvider.getCurrentTimeMillis() } returns 1000L
        coEvery { mediaService.getMedia(any(), any()) } throws Exception("Network error")
        coEvery { localDataSource.isEmpty() } returns true
        val jsonSeed = """[{"id":"seed1","url":"seed_url"}]"""
        every { assetManager.open("seed.json") } returns ByteArrayInputStream(jsonSeed.toByteArray())

        // Act
        repository.refresh()

        // Assert
        coVerify { toastManager.showToast("Falha ao atualizar. Exibindo conteúdo salvo.") }
        coVerify { localDataSource.insertAll(match { it.first().id == "seed1" }) }
    }

    @Test
    fun `refresh - Offline + Room success - should show toast and NOT load assets`() = runTest(testDispatcher) {
        // Arrange
        every { networkMonitor.isOnline } returns flowOf(false)
        coEvery { localDataSource.isEmpty() } returns false

        // Act
        repository.refresh()

        // Assert
        coVerify { toastManager.showToast("Sem internet. Exibindo conteúdo offline.") }
        coVerify(exactly = 0) { mediaService.getMedia(any(), any()) }
        coVerify(exactly = 0) { assetManager.open(any()) }
    }

    @Test
    fun `refresh - Offline + Room empty + Seed success - should show toast and load assets`() = runTest(testDispatcher) {
        // Arrange
        every { networkMonitor.isOnline } returns flowOf(false)
        coEvery { localDataSource.isEmpty() } returns true
        val jsonSeed = """[{"id":"seed1","url":"seed_url"}]"""
        every { assetManager.open("seed.json") } returns ByteArrayInputStream(jsonSeed.toByteArray())

        // Act
        repository.refresh()

        // Assert
        coVerify { toastManager.showToast("Sem internet. Exibindo conteúdo offline.") }
        coVerify { localDataSource.insertAll(any()) }
    }

    @Test
    fun `loadNextPage - should increment page and session count when online`() = runTest(testDispatcher) {
        // Arrange
        every { networkMonitor.isOnline } returns flowOf(true)
        coEvery { cacheMetadataDao.getMetadata("test") } returns CacheMetadataEntity("test", 1000L, nextPage = 2)
        coEvery { mediaService.getMedia(any(), any()) } returns listOf(NetworkMediaItem("2", "url2"))

        // Act
        repository.loadNextPage()

        // Assert
        coVerify { mediaService.getMedia(limit = 20, page = 2) }
        assertEquals(1, paginationSession.getExtraPagesCount("test"))
    }
}
