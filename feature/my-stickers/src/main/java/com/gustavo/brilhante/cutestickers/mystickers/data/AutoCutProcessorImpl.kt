package com.gustavo.brilhante.cutestickers.mystickers.data

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
import com.gustavo.brilhante.cutestickers.common.network.CatsDispatchers
import com.gustavo.brilhante.cutestickers.common.network.Dispatcher
import com.gustavo.brilhante.cutestickers.mystickers.AutoCutProcessor
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal class AutoCutProcessorImpl @Inject constructor(
    @Dispatcher(CatsDispatchers.IO) private val ioDispatcher: CoroutineDispatcher
) : AutoCutProcessor {

    override suspend fun removeBackground(bitmap: Bitmap): Result<Bitmap> =
        withContext(ioDispatcher) {
            runCatching {
                val options = SubjectSegmenterOptions.Builder()
                    .enableForegroundConfidenceMask()
                    .build()
                val segmenter = SubjectSegmentation.getClient(options)
                val inputImage = InputImage.fromBitmap(bitmap, 0)

                val segResult = suspendCancellableCoroutine { cont ->
                    segmenter.process(inputImage)
                        .addOnSuccessListener { cont.resume(it) }
                        .addOnFailureListener { cont.resumeWithException(it) }
                    cont.invokeOnCancellation { segmenter.close() }
                }

                val maskBuffer = segResult.foregroundConfidenceMask
                    ?: error("No foreground mask returned")
                maskBuffer.rewind()
                val mask = FloatArray(bitmap.width * bitmap.height)
                maskBuffer.get(mask)

                applyMaskToAlpha(bitmap, mask)
            }
        }
}

internal fun applyConfidenceToPixels(pixels: IntArray, mask: FloatArray): IntArray =
    IntArray(pixels.size) { i ->
        val alpha = (mask[i] * 255).toInt().coerceIn(0, 255)
        (pixels[i] and 0x00FFFFFF) or (alpha shl 24)
    }

internal fun applyMaskToAlpha(source: Bitmap, mask: FloatArray): Bitmap {
    val output = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
    val pixels = IntArray(source.width * source.height)
    source.getPixels(pixels, 0, source.width, 0, 0, source.width, source.height)
    output.setPixels(
        applyConfidenceToPixels(pixels, mask),
        0, source.width, 0, 0, source.width, source.height
    )
    return output
}
