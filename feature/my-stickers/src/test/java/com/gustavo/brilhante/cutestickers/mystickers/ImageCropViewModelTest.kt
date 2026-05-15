package com.gustavo.brilhante.cutestickers.mystickers

import android.graphics.Bitmap
import android.net.Uri
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ImageCropViewModelTest {

    private val processor = mockk<CropImageProcessor>()
    private val autoCutProcessor = mockk<AutoCutProcessor>()
    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var viewModel: ImageCropViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = ImageCropViewModel(processor, autoCutProcessor)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // --- loadImage ---

    @Test
    fun initialState_isLoading() {
        assertTrue(viewModel.uiState.value is ImageCropUiState.Loading)
    }

    @Test
    fun loadImage_success_emitsReadyWithOriginalModeAndBitmap() = runTest(testDispatcher) {
        val bitmap = mockk<Bitmap>()
        coEvery { processor.loadBitmapWithExifCorrection(any()) } returns bitmap

        viewModel.loadImage(mockk<Uri>())

        val state = viewModel.uiState.value as ImageCropUiState.Ready
        assertSame(bitmap, state.displayBitmap)
        assertEquals(CropMode.ORIGINAL, state.mode)
        assertFalse(state.isAutoCutting)
        assertFalse(state.autoCutError)
    }

    @Test
    fun loadImage_nullBitmap_staysLoading() = runTest(testDispatcher) {
        coEvery { processor.loadBitmapWithExifCorrection(any()) } returns null

        viewModel.loadImage(mockk<Uri>())

        assertTrue(viewModel.uiState.value is ImageCropUiState.Loading)
    }

    @Test
    fun loadImage_calledTwice_resetsToLoading_thenReady() = runTest(testDispatcher) {
        val bitmap1 = mockk<Bitmap>()
        val bitmap2 = mockk<Bitmap>()
        coEvery { processor.loadBitmapWithExifCorrection(any()) } returnsMany listOf(bitmap1, bitmap2)
        val uri1 = mockk<Uri>()
        val uri2 = mockk<Uri>()

        viewModel.loadImage(uri1)
        viewModel.loadImage(uri2)

        val state = viewModel.uiState.value as ImageCropUiState.Ready
        assertSame(bitmap2, state.displayBitmap)
    }

    // --- setMode ---

    @Test
    fun setMode_toAutoCut_onSuccess_updatesDisplayBitmapAndClearsLoading() = runTest(testDispatcher) {
        val original = mockk<Bitmap>()
        val cutout = mockk<Bitmap>()
        coEvery { processor.loadBitmapWithExifCorrection(any()) } returns original
        coEvery { autoCutProcessor.removeBackground(original) } returns Result.success(cutout)
        viewModel.loadImage(mockk<Uri>())

        viewModel.setMode(CropMode.AUTO_CUT)

        val state = viewModel.uiState.value as ImageCropUiState.Ready
        assertEquals(CropMode.AUTO_CUT, state.mode)
        assertSame(cutout, state.displayBitmap)
        assertFalse(state.isAutoCutting)
        assertFalse(state.autoCutError)
    }

    @Test
    fun setMode_toAutoCut_onFailure_showsErrorAndRestoresOriginal() = runTest(testDispatcher) {
        val original = mockk<Bitmap>()
        coEvery { processor.loadBitmapWithExifCorrection(any()) } returns original
        coEvery { autoCutProcessor.removeBackground(original) } returns Result.failure(RuntimeException("ML Kit error"))
        viewModel.loadImage(mockk<Uri>())

        viewModel.setMode(CropMode.AUTO_CUT)

        val state = viewModel.uiState.value as ImageCropUiState.Ready
        assertEquals(CropMode.AUTO_CUT, state.mode)
        assertSame(original, state.displayBitmap)
        assertFalse(state.isAutoCutting)
        assertTrue(state.autoCutError)
    }

    @Test
    fun setMode_backToOriginal_restoresOriginalBitmapAndClearsError() = runTest(testDispatcher) {
        val original = mockk<Bitmap>()
        val cutout = mockk<Bitmap>()
        coEvery { processor.loadBitmapWithExifCorrection(any()) } returns original
        coEvery { autoCutProcessor.removeBackground(original) } returns Result.success(cutout)
        viewModel.loadImage(mockk<Uri>())
        viewModel.setMode(CropMode.AUTO_CUT)

        viewModel.setMode(CropMode.ORIGINAL)

        val state = viewModel.uiState.value as ImageCropUiState.Ready
        assertEquals(CropMode.ORIGINAL, state.mode)
        assertSame(original, state.displayBitmap)
        assertFalse(state.isAutoCutting)
        assertFalse(state.autoCutError)
    }

    @Test
    fun setMode_sameMode_isNoOp() = runTest(testDispatcher) {
        val bitmap = mockk<Bitmap>()
        coEvery { processor.loadBitmapWithExifCorrection(any()) } returns bitmap
        viewModel.loadImage(mockk<Uri>())
        val stateBefore = viewModel.uiState.value

        viewModel.setMode(CropMode.ORIGINAL) // already ORIGINAL

        assertEquals(stateBefore, viewModel.uiState.value)
        coVerify(exactly = 0) { autoCutProcessor.removeBackground(any()) }
    }

    @Test
    fun setMode_beforeLoadImage_isNoOp() {
        viewModel.setMode(CropMode.AUTO_CUT)

        assertTrue(viewModel.uiState.value is ImageCropUiState.Loading)
    }

    @Test
    fun setMode_toOriginalAfterAutoCutError_clearsPreviousError() = runTest(testDispatcher) {
        val original = mockk<Bitmap>()
        coEvery { processor.loadBitmapWithExifCorrection(any()) } returns original
        coEvery { autoCutProcessor.removeBackground(original) } returns Result.failure(RuntimeException())
        viewModel.loadImage(mockk<Uri>())
        viewModel.setMode(CropMode.AUTO_CUT)
        assertTrue((viewModel.uiState.value as ImageCropUiState.Ready).autoCutError)

        viewModel.setMode(CropMode.ORIGINAL)

        assertFalse((viewModel.uiState.value as ImageCropUiState.Ready).autoCutError)
    }

    // --- saveCrop ---

    @Test
    fun saveCrop_delegatesToProcessor() = runTest(testDispatcher) {
        val bmp = mockk<Bitmap>()
        val expectedUri = mockk<Uri>()
        coEvery { processor.saveCroppedBitmap(bmp, 10, 20, 100, 90f) } returns expectedUri

        val result = viewModel.saveCrop(bmp, 10, 20, 100, 90f)

        assertSame(expectedUri, result)
        coVerify(exactly = 1) { processor.saveCroppedBitmap(bmp, 10, 20, 100, 90f) }
    }
}
