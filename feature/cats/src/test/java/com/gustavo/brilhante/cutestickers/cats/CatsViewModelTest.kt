package com.gustavo.brilhante.cutestickers.cats

import com.gustavo.brilhante.cutestickers.common.network.NetworkMonitor
import com.gustavo.brilhante.cutestickers.domain.usecase.GetCachedMediaUseCase
import com.gustavo.brilhante.cutestickers.domain.usecase.LoadNextPageUseCase
import com.gustavo.brilhante.cutestickers.domain.usecase.RefreshMediaUseCase
import com.gustavo.brilhante.cutestickers.model.MediaItem
import com.gustavo.brilhante.cutestickers.mystickers.domain.MyStickersRepository
import com.gustavo.brilhante.cutestickers.ui.DiscoverUiState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import java.net.UnknownHostException
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CatsViewModelTest {

    private val getCachedMediaUseCase = mockk<GetCachedMediaUseCase>()
    private val refreshMediaUseCase = mockk<RefreshMediaUseCase>()
    private val loadNextPageUseCase = mockk<LoadNextPageUseCase>()
    private val myStickersRepository = mockk<MyStickersRepository>()
    private val networkMonitor = mockk<NetworkMonitor>()
    private val isOnlineFlow = MutableStateFlow(true)
    private val mediaFlow = MutableStateFlow<List<MediaItem>>(emptyList())
    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var viewModel: CatsViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { getCachedMediaUseCase() } returns mediaFlow
        coEvery { refreshMediaUseCase(any()) } returns true
        every { networkMonitor.isOnline } returns isOnlineFlow
        viewModel = CatsViewModel(
            getCachedMediaUseCase,
            refreshMediaUseCase,
            loadNextPageUseCase,
            myStickersRepository,
            networkMonitor
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // Feature 4: multi-select in Discover
    @Test
    fun toggleSelection_addsItemId() = runTest(testDispatcher) {
        val item = MediaItem(id = "cat1", url = "https://example.com/cat.jpg")

        viewModel.toggleSelection(item)

        assertTrue("cat1" in viewModel.selectedIds.value)
    }

    @Test
    fun toggleSelection_alreadySelected_removesItemId() = runTest(testDispatcher) {
        val item = MediaItem(id = "cat1", url = "https://example.com/cat.jpg")
        viewModel.toggleSelection(item)

        viewModel.toggleSelection(item)

        assertFalse("cat1" in viewModel.selectedIds.value)
    }

    @Test
    fun clearSelection_emptiesSelectedIds() = runTest(testDispatcher) {
        val item = MediaItem(id = "cat1", url = "https://example.com/cat.jpg")
        viewModel.toggleSelection(item)

        viewModel.clearSelection()

        assertTrue(viewModel.selectedIds.value.isEmpty())
    }

    @Test
    fun saveSelectionToMyStickers_callsRepositoryForEachSelected() = runTest(testDispatcher) {
        val items = listOf(
            MediaItem(id = "cat1", url = "https://example.com/cat1.jpg"),
            MediaItem(id = "cat2", url = "https://example.com/cat2.jpg")
        )
        // getCachedMediaUseCase returns mediaFlow; set its value so .first() returns items
        mediaFlow.value = items
        coEvery { myStickersRepository.saveFromUrl(any(), any()) } returns Result.success(mockk())
        viewModel.toggleSelection(items[0])
        viewModel.toggleSelection(items[1])

        viewModel.saveSelectionToMyStickers()

        coVerify { myStickersRepository.saveFromUrl("https://example.com/cat1.jpg", "cat1") }
        coVerify { myStickersRepository.saveFromUrl("https://example.com/cat2.jpg", "cat2") }
    }

    @Test
    fun saveSelectionToMyStickers_clearsSelectionImmediately() = runTest(testDispatcher) {
        val item = MediaItem(id = "cat1", url = "https://example.com/cat.jpg")
        mediaFlow.value = listOf(item)
        coEvery { myStickersRepository.saveFromUrl(any(), any()) } returns Result.success(mockk())
        viewModel.toggleSelection(item)

        viewModel.saveSelectionToMyStickers()

        assertTrue(viewModel.selectedIds.value.isEmpty())
    }

    @Test
    fun refresh_onNetworkError_setsIsNoInternetTrue() = runTest(testDispatcher) {
        coEvery { refreshMediaUseCase(any()) } throws UnknownHostException("no network")
        val job = launch { viewModel.uiState.collect {} }

        viewModel.refresh()

        val state = viewModel.uiState.value
        assertTrue(state is DiscoverUiState.Error)
        assertTrue((state as DiscoverUiState.Error).isNoInternet)
        job.cancel()
    }

    @Test
    fun refresh_onGenericError_setsErrorStateWithMessage() = runTest(testDispatcher) {
        coEvery { refreshMediaUseCase(any()) } throws RuntimeException("server down")
        val job = launch { viewModel.uiState.collect {} }

        viewModel.refresh()

        val state = viewModel.uiState.value
        assertTrue(state is DiscoverUiState.Error)
        val error = state as DiscoverUiState.Error
        assertEquals("server down", error.message)
        assertFalse(error.isNoInternet)
        job.cancel()
    }

    @Test
    fun loadMore_preventsParallelLoads() = runTest(testDispatcher) {
        val barrier = CompletableDeferred<Unit>()
        coEvery { loadNextPageUseCase() } coAnswers { barrier.await() }

        viewModel.loadMore()
        viewModel.loadMore()
        barrier.complete(Unit)

        coVerify(exactly = 1) { loadNextPageUseCase() }
    }

    @Test
    fun uiState_whenOfflineWithItems_isOfflineTrue() = runTest(testDispatcher) {
        mediaFlow.value = listOf(MediaItem(id = "cat1", url = "https://example.com/cat.jpg"))
        isOnlineFlow.value = false
        val job = launch { viewModel.uiState.collect {} }

        val state = viewModel.uiState.value
        assertTrue(state is DiscoverUiState.Success)
        assertTrue((state as DiscoverUiState.Success).isOffline)
        job.cancel()
    }

    @Test
    fun uiState_whenOnlineWithItems_isOfflineFalse() = runTest(testDispatcher) {
        mediaFlow.value = listOf(MediaItem(id = "cat1", url = "https://example.com/cat.jpg"))
        isOnlineFlow.value = true
        val job = launch { viewModel.uiState.collect {} }

        val state = viewModel.uiState.value
        assertTrue(state is DiscoverUiState.Success)
        assertFalse((state as DiscoverUiState.Success).isOffline)
        job.cancel()
    }

    @Test
    fun refresh_offline_finishesRefreshingWithoutSpinner() = runTest(testDispatcher) {
        isOnlineFlow.value = false
        coEvery { refreshMediaUseCase(any()) } returns false
        mediaFlow.value = listOf(MediaItem(id = "cat1", url = ""))
        val job = launch { viewModel.uiState.collect {} }

        viewModel.refresh()

        val state = viewModel.uiState.value
        assertTrue(state is DiscoverUiState.Success)
        assertFalse((state as DiscoverUiState.Success).isRefreshing)
        job.cancel()
    }

    @Test
    fun uiState_networkSwitchesFromOfflineToOnline_bannerDisappears() = runTest(testDispatcher) {
        mediaFlow.value = listOf(MediaItem(id = "cat1", url = "https://example.com/cat.jpg"))
        isOnlineFlow.value = false
        val job = launch { viewModel.uiState.collect {} }

        assertTrue((viewModel.uiState.value as DiscoverUiState.Success).isOffline)

        isOnlineFlow.value = true

        assertFalse((viewModel.uiState.value as DiscoverUiState.Success).isOffline)
        job.cancel()
    }
}
