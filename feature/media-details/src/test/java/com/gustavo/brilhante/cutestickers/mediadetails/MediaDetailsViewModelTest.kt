package com.gustavo.brilhante.cutestickers.mediadetails

import android.app.Activity
import android.content.Intent
import com.gustavo.brilhante.cutestickers.common.MediaMetadataResolver
import com.gustavo.brilhante.cutestickers.model.MediaType
import com.gustavo.brilhante.cutestickers.mystickers.domain.MySticker
import com.gustavo.brilhante.cutestickers.mystickers.domain.MyStickersRepository
import com.gustavo.brilhante.cutestickers.mystickers.domain.SourceType
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
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MediaDetailsViewModelTest {

    private val createStickerUseCase = mockk<CreateStickerUseCase>()
    private val exportStickerPackUseCase = mockk<ExportStickerPackUseCase>()
    private val saveMediaUseCase = mockk<SaveMediaUseCase>()
    private val metadataResolver = mockk<MediaMetadataResolver>()
    private val myStickersRepository = mockk<MyStickersRepository>()
    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var viewModel: MediaDetailsViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { metadataResolver.getMediaType(any()) } returns MediaType.Static
        viewModel = MediaDetailsViewModel(
            createStickerUseCase,
            exportStickerPackUseCase,
            saveMediaUseCase,
            metadataResolver,
            myStickersRepository
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initSetsCorrectMediaTypeFromResolver() = runTest(testDispatcher) {
        val url = "https://example.com/cat.gif"
        every { metadataResolver.getMediaType(url) } returns MediaType.Animated
        
        viewModel.init(url, "123")
        
        assertEquals(MediaType.Animated, viewModel.uiState.value.mediaType)
        assertEquals(url, viewModel.uiState.value.imageUrl)
    }

    @Test
    fun onAddToWhatsAppUpdatesStateToSuccess() = runTest(testDispatcher) {
        val pack = StickerPack("id", "name", "pub", "tray", emptyList(), false)
        coEvery { createStickerUseCase(any(), any(), any(), any()) } returns Result.success(pack)
        
        viewModel.init("url", "id")
        viewModel.onAddToWhatsApp()
        
        assertTrue(viewModel.uiState.value.stickerState is StickerState.Success)
        assertEquals(pack, (viewModel.uiState.value.stickerState as StickerState.Success).pack)
    }

    @Test
    fun onConfirmExportEmitsEvent() = runTest(testDispatcher) {
        val pack = StickerPack("id", "name", "pub", "tray", emptyList(), false)
        val metadata = ExportMetadata("id", "auth", "name", "pub", "tray", false, "pkg")
        coEvery { exportStickerPackUseCase.getExportMetadata(pack) } returns Result.success(metadata)
        
        val events = mutableListOf<MediaDetailsEvent>()
        val collectJob = launch {
            viewModel.events.toList(events)
        }
        
        viewModel.onConfirmExport(pack)
        
        assertTrue(events.first() is MediaDetailsEvent.ExportToWhatsApp)
        val event = events.first() as MediaDetailsEvent.ExportToWhatsApp
        assertEquals("id", event.packId)
        assertEquals("pkg", event.targetPackage)
        collectJob.cancel()
    }

    @Test
    fun onExportResultSuccessUpdatesState() = runTest(testDispatcher) {
        viewModel.onExportResult(Activity.RESULT_OK, null)
        
        assertTrue(viewModel.uiState.value.stickerState is StickerState.Idle)
    }

    @Test
    fun onDownloadMediaUpdatesToError() = runTest(testDispatcher) {
        coEvery { saveMediaUseCase(any()) } returns Result.failure(Exception("Disk full"))

        viewModel.init("url", "id")
        viewModel.onDownloadMedia()

        assertTrue(viewModel.uiState.value.downloadState is DownloadState.Error)
        assertEquals("Disk full", (viewModel.uiState.value.downloadState as DownloadState.Error).message)
    }

    @Test
    fun saveToMyStickersUpdatesToSuccess() = runTest(testDispatcher) {
        val sticker = MySticker("id", "/path/img.jpg", SourceType.APP, 1000L)
        coEvery { myStickersRepository.saveFromUrl(any(), any()) } returns Result.success(sticker)

        viewModel.init("url", "id")
        viewModel.saveToMyStickers()

        assertTrue(viewModel.uiState.value.saveToMyStickersState is SaveToMyStickersState.Success)
    }

    @Test
    fun saveToMyStickersUpdatesToError() = runTest(testDispatcher) {
        coEvery { myStickersRepository.saveFromUrl(any(), any()) } returns Result.failure(Exception("Network error"))

        viewModel.init("url", "id")
        viewModel.saveToMyStickers()

        assertTrue(viewModel.uiState.value.saveToMyStickersState is SaveToMyStickersState.Error)
        assertEquals("Network error", (viewModel.uiState.value.saveToMyStickersState as SaveToMyStickersState.Error).message)
    }

    // Bug 2: "Save to My Stickers" must be hidden when media already comes from the local library
    @Test
    fun initWithFileUrl_setsIsLocalMediaTrue() = runTest(testDispatcher) {
        viewModel.init("file:///data/user/0/com.example/files/my-stickers/img.jpg", "id")

        assertTrue(viewModel.uiState.value.isLocalMedia)
    }

    @Test
    fun initWithAbsolutePath_setsIsLocalMediaTrue() = runTest(testDispatcher) {
        viewModel.init("/data/user/0/com.example/files/my-stickers/img.jpg", "id")

        assertTrue(viewModel.uiState.value.isLocalMedia)
    }

    @Test
    fun initWithHttpUrl_doesNotSetIsLocalMedia() = runTest(testDispatcher) {
        viewModel.init("https://example.com/cat.gif", "id")

        assertFalse(viewModel.uiState.value.isLocalMedia)
    }

    @Test
    fun dismissSaveToMyStickersResetsState() = runTest(testDispatcher) {
        val sticker = MySticker("id", "/path/img.jpg", SourceType.APP, 1000L)
        coEvery { myStickersRepository.saveFromUrl(any(), any()) } returns Result.success(sticker)
        viewModel.init("url", "id")
        viewModel.saveToMyStickers()

        viewModel.dismissSaveToMyStickers()

        assertTrue(viewModel.uiState.value.saveToMyStickersState is SaveToMyStickersState.Idle)
    }
}
