package com.gustavo.brilhante.cutestickers.dogs

import com.gustavo.brilhante.cutestickers.domain.usecase.GetCachedMediaUseCase
import com.gustavo.brilhante.cutestickers.domain.usecase.LoadNextPageUseCase
import com.gustavo.brilhante.cutestickers.domain.usecase.RefreshMediaUseCase
import com.gustavo.brilhante.cutestickers.model.MediaItem
import com.gustavo.brilhante.cutestickers.mystickers.domain.MyStickersRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.Runs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DogsViewModelTest {

    private val getCachedMediaUseCase = mockk<GetCachedMediaUseCase>()
    private val refreshMediaUseCase = mockk<RefreshMediaUseCase>()
    private val loadNextPageUseCase = mockk<LoadNextPageUseCase>()
    private val myStickersRepository = mockk<MyStickersRepository>()
    private val mediaFlow = MutableStateFlow<List<MediaItem>>(emptyList())
    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var viewModel: DogsViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { getCachedMediaUseCase() } returns mediaFlow
        coEvery { refreshMediaUseCase() } just Runs
        viewModel = DogsViewModel(
            getCachedMediaUseCase,
            refreshMediaUseCase,
            loadNextPageUseCase,
            myStickersRepository
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun toggleSelection_addsItemId() = runTest(testDispatcher) {
        val item = MediaItem(id = "dog1", url = "https://example.com/dog.jpg")

        viewModel.toggleSelection(item)

        assertTrue("dog1" in viewModel.selectedIds.value)
    }

    @Test
    fun toggleSelection_alreadySelected_removesItemId() = runTest(testDispatcher) {
        val item = MediaItem(id = "dog1", url = "https://example.com/dog.jpg")
        viewModel.toggleSelection(item)

        viewModel.toggleSelection(item)

        assertFalse("dog1" in viewModel.selectedIds.value)
    }

    @Test
    fun clearSelection_emptiesSelectedIds() = runTest(testDispatcher) {
        val item = MediaItem(id = "dog1", url = "https://example.com/dog.jpg")
        viewModel.toggleSelection(item)

        viewModel.clearSelection()

        assertTrue(viewModel.selectedIds.value.isEmpty())
    }

    @Test
    fun saveSelectionToMyStickers_callsRepositoryForEachSelected() = runTest(testDispatcher) {
        val items = listOf(
            MediaItem(id = "dog1", url = "https://example.com/dog1.jpg"),
            MediaItem(id = "dog2", url = "https://example.com/dog2.jpg")
        )
        mediaFlow.value = items
        coEvery { myStickersRepository.saveFromUrl(any(), any()) } returns Result.success(mockk())
        viewModel.toggleSelection(items[0])
        viewModel.toggleSelection(items[1])

        viewModel.saveSelectionToMyStickers()

        coVerify { myStickersRepository.saveFromUrl("https://example.com/dog1.jpg", "dog1") }
        coVerify { myStickersRepository.saveFromUrl("https://example.com/dog2.jpg", "dog2") }
    }

    @Test
    fun saveSelectionToMyStickers_clearsSelectionImmediately() = runTest(testDispatcher) {
        val item = MediaItem(id = "dog1", url = "https://example.com/dog.jpg")
        mediaFlow.value = listOf(item)
        coEvery { myStickersRepository.saveFromUrl(any(), any()) } returns Result.success(mockk())
        viewModel.toggleSelection(item)

        viewModel.saveSelectionToMyStickers()

        assertTrue(viewModel.selectedIds.value.isEmpty())
    }
}
