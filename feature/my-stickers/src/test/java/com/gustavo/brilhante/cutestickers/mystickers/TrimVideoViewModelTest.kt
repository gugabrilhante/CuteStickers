package com.gustavo.brilhante.cutestickers.mystickers

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class TrimVideoViewModelTest {

    private val videoImportProcessor = mockk<VideoImportProcessor>()
    private val animatedStickerProcessor = mockk<AnimatedStickerProcessor>()
    private val context = mockk<Context>(relaxed = true)
    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var viewModel: TrimVideoViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockkStatic(MediaMetadataRetriever::class)
        viewModel = TrimVideoViewModel(videoImportProcessor, animatedStickerProcessor, context)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadVideo updates duration and trim points`() {
        val uri = mockk<Uri>()
        val retriever = mockk<MediaMetadataRetriever>(relaxed = true)
        every { anyConstructed<MediaMetadataRetriever>().setDataSource(any<Context>(), any()) } returns Unit
        every { anyConstructed<MediaMetadataRetriever>().extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION) } returns "10000"
        
        viewModel.loadVideo(uri)

        val state = viewModel.uiState.value
        assertEquals(uri, state.videoUri)
        assertEquals(10000L, state.durationMs)
        assertEquals(0L, state.startTimeMs)
        assertEquals(5000L, state.endTimeMs) // Max duration limit
    }

    @Test
    fun `onTrimChanged updates state and validates duration`() {
        viewModel.onTrimChanged(1000L, 7000L) // 6 seconds

        val state = viewModel.uiState.value
        assertEquals(1000L, state.startTimeMs)
        assertEquals(7000L, state.endTimeMs)
        assertFalse(state.isDurationValid) // Over 5s
    }

    @Test
    fun `onTrimChanged with valid duration sets isDurationValid to true`() {
        viewModel.onTrimChanged(1000L, 4000L) // 3 seconds

        val state = viewModel.uiState.value
        assertTrue(state.isDurationValid)
    }
}
