package com.gustavo.brilhante.cutestickers.mediadetails

import android.app.Activity
import android.content.Intent
import com.gustavo.brilhante.cutestickers.common.MediaMetadataResolver
import com.gustavo.brilhante.cutestickers.model.MediaType
import com.gustavo.brilhante.cutestickers.stickers.domain.CreateStickerUseCase
import com.gustavo.brilhante.cutestickers.stickers.domain.ExportMetadata
import com.gustavo.brilhante.cutestickers.stickers.domain.ExportStickerPackUseCase
import com.gustavo.brilhante.cutestickers.stickers.domain.SaveMediaUseCase
import com.gustavo.brilhante.cutestickers.stickers.domain.StickerPack
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MediaDetailsViewModelTest {

    private val createStickerUseCase = mockk<CreateStickerUseCase>()
    private val exportStickerPackUseCase = mockk<ExportStickerPackUseCase>()
    private val saveMediaUseCase = mockk<SaveMediaUseCase>()
    private val metadataResolver = mockk<MediaMetadataResolver>()
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var viewModel: MediaDetailsViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = MediaDetailsViewModel(
            createStickerUseCase,
            exportStickerPackUseCase,
            saveMediaUseCase,
            metadataResolver
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initSetsCorrectMediaTypeFromResolver() = runTest {
        val url = "https://example.com/cat.gif"
        every { metadataResolver.getMediaType(url) } returns MediaType.Animated
        
        viewModel.init(url, "123")
        
        assertEquals(MediaType.Animated, viewModel.uiState.value.mediaType)
        assertEquals(url, viewModel.uiState.value.imageUrl)
    }

    @Test
    fun onAddToWhatsAppUpdatesStateToSuccess() = runTest {
        val pack = StickerPack("id", "name", "pub", "tray", emptyList(), false)
        every { metadataResolver.getMediaType(any()) } returns MediaType.Static
        coEvery { createStickerUseCase(any(), any(), any()) } returns Result.success(pack)
        
        viewModel.init("url", "id")
        viewModel.onAddToWhatsApp()
        
        testDispatcher.scheduler.advanceUntilIdle()
        
        assertTrue(viewModel.uiState.value.stickerState is StickerState.Success)
        assertEquals(pack, (viewModel.uiState.value.stickerState as StickerState.Success).pack)
    }

    @Test
    fun onConfirmExportEmitsEvent() = runTest {
        val pack = StickerPack("id", "name", "pub", "tray", emptyList(), false)
        val metadata = ExportMetadata("id", "auth", "name", "pub", "tray", false, "pkg")
        coEvery { exportStickerPackUseCase.getExportMetadata(pack) } returns Result.success(metadata)
        
        viewModel.onConfirmExport(pack)
        
        testDispatcher.scheduler.advanceUntilIdle()
        
        val event = viewModel.events.first()
        assertTrue(event is MediaDetailsEvent.LaunchIntent)
    }

    @Test
    fun onExportResultSuccessUpdatesState() = runTest {
        viewModel.onExportResult(Activity.RESULT_OK, null)
        
        assertTrue(viewModel.uiState.value.stickerState is StickerState.Idle)
    }

    @Test
    fun onDownloadMediaUpdatesToError() = runTest {
        coEvery { saveMediaUseCase(any()) } returns Result.failure<Unit>(Exception("Disk full"))
        
        viewModel.init("url", "id")
        viewModel.onDownloadMedia()
        
        testDispatcher.scheduler.advanceUntilIdle()
        
        assertTrue(viewModel.uiState.value.downloadState is DownloadState.Error)
        assertEquals("Disk full", (viewModel.uiState.value.downloadState as DownloadState.Error).message)
    }
}
