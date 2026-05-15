package com.gustavo.brilhante.cutestickers.mystickers

import com.gustavo.brilhante.cutestickers.mystickers.domain.MySticker
import com.gustavo.brilhante.cutestickers.mystickers.domain.MyStickersRepository
import com.gustavo.brilhante.cutestickers.mystickers.domain.SourceType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MyStickersViewModelTest {

    private val repository = mockk<MyStickersRepository>()
    private val cropImageProcessor = mockk<CropImageProcessor>()
    private val stickersFlow = MutableStateFlow<List<MySticker>>(emptyList())
    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var viewModel: MyStickersViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { repository.getStickers() } returns stickersFlow
        viewModel = MyStickersViewModel(repository, cropImageProcessor)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun uiStateIsEmptyWhenNoStickers() = runTest(testDispatcher) {
        stickersFlow.value = emptyList()
        assertTrue(viewModel.uiState.first() is MyStickersUiState.Empty)
    }

    @Test
    fun uiStateIsSuccessWithItems() = runTest(testDispatcher) {
        val sticker = MySticker("id1", "/path/to/img.jpg", SourceType.GALLERY, 1000L)
        stickersFlow.value = listOf(sticker)

        val state = viewModel.uiState.first()
        assertTrue(state is MyStickersUiState.Success)
        val success = state as MyStickersUiState.Success
        assertEquals(1, success.items.size)
        assertEquals("id1", success.items.first().id)
        assertEquals("file:///path/to/img.jpg", success.items.first().url)
    }

    @Test
    fun importFromGalleryCallsRepositoryOnSuccess() = runTest(testDispatcher) {
        val sticker = MySticker("uuid", "/path/img.jpg", SourceType.GALLERY, 1000L)
        coEvery { repository.saveFromUri(any()) } returns Result.success(sticker)

        viewModel.importFromGallery("content://some/uri")

        coVerify { repository.saveFromUri("content://some/uri") }
    }

    @Test
    fun importFromGalleryOnFailureSetsImportError() = runTest(testDispatcher) {
        coEvery { repository.saveFromUri(any()) } returns Result.failure(Exception("Permission denied"))

        viewModel.importFromGallery("content://some/uri")

        val state = viewModel.uiState.first()
        assertTrue(state is MyStickersUiState.Success)
        assertEquals("Permission denied", (state as MyStickersUiState.Success).importError)
    }

    // Feature 3: multi-select + delete
    @Test
    fun toggleSelection_addsIdToSelectedIds() = runTest(testDispatcher) {
        val sticker = MySticker("id1", "/path/img.jpg", SourceType.GALLERY, 1000L)
        stickersFlow.value = listOf(sticker)

        viewModel.toggleSelection("id1")

        val state = viewModel.uiState.first() as MyStickersUiState.Success
        assertTrue("id1" in state.selectedIds)
    }

    @Test
    fun toggleSelection_alreadySelected_removesId() = runTest(testDispatcher) {
        val sticker = MySticker("id1", "/path/img.jpg", SourceType.GALLERY, 1000L)
        stickersFlow.value = listOf(sticker)
        viewModel.toggleSelection("id1")

        viewModel.toggleSelection("id1")

        val state = viewModel.uiState.first() as MyStickersUiState.Success
        assertTrue("id1" !in state.selectedIds)
    }

    @Test
    fun deleteSelected_callsRepositoryForEachSelectedId() = runTest(testDispatcher) {
        val sticker1 = MySticker("id1", "/path/img1.jpg", SourceType.GALLERY, 1000L)
        val sticker2 = MySticker("id2", "/path/img2.jpg", SourceType.GALLERY, 2000L)
        stickersFlow.value = listOf(sticker1, sticker2)
        coEvery { repository.deleteSticker(any()) } returns Result.success(Unit)
        viewModel.toggleSelection("id1")
        viewModel.toggleSelection("id2")

        viewModel.deleteSelected()

        coVerify { repository.deleteSticker("id1") }
        coVerify { repository.deleteSticker("id2") }
    }

    @Test
    fun deleteSelected_clearsSelectionBeforeDeleting() = runTest(testDispatcher) {
        val sticker = MySticker("id1", "/path/img.jpg", SourceType.GALLERY, 1000L)
        stickersFlow.value = listOf(sticker)
        coEvery { repository.deleteSticker(any()) } returns Result.success(Unit)
        viewModel.toggleSelection("id1")

        viewModel.deleteSelected()

        val state = viewModel.uiState.first() as MyStickersUiState.Success
        assertTrue(state.selectedIds.isEmpty())
    }

    @Test
    fun clearSelection_emptiesSelectedIds() = runTest(testDispatcher) {
        val sticker = MySticker("id1", "/path/img.jpg", SourceType.GALLERY, 1000L)
        stickersFlow.value = listOf(sticker)
        viewModel.toggleSelection("id1")

        viewModel.clearSelection()

        val state = viewModel.uiState.first() as MyStickersUiState.Success
        assertTrue(state.selectedIds.isEmpty())
    }

    @Test
    fun clearImportErrorRemovesError() = runTest(testDispatcher) {
        val existingSticker = MySticker("id1", "/path/img.jpg", SourceType.GALLERY, 1000L)
        stickersFlow.value = listOf(existingSticker)
        coEvery { repository.saveFromUri(any()) } returns Result.failure(Exception("Error"))
        viewModel.importFromGallery("content://uri")

        viewModel.clearImportError()

        val state = viewModel.uiState.first()
        assertTrue(state is MyStickersUiState.Success)
        assertEquals(null, (state as MyStickersUiState.Success).importError)
    }
}
