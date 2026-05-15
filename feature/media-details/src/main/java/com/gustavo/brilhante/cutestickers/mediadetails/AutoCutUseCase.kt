package com.gustavo.brilhante.cutestickers.mediadetails

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.gustavo.brilhante.cutestickers.common.network.CatsDispatchers
import com.gustavo.brilhante.cutestickers.common.network.Dispatcher
import com.gustavo.brilhante.cutestickers.mystickers.AutoCutProcessor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

class AutoCutUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val autoCutProcessor: AutoCutProcessor,
    @Dispatcher(CatsDispatchers.IO) private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(imageUrl: String): Result<File> = withContext(ioDispatcher) {
        runCatching {
            val imageLoader = ImageLoader.Builder(context).build()
            val request = ImageRequest.Builder(context).data(imageUrl).allowHardware(false).build()
            val coilResult = imageLoader.execute(request)
            val bitmap = ((coilResult as? SuccessResult)?.drawable as? BitmapDrawable)?.bitmap
                ?: error("Failed to load bitmap from $imageUrl")
            val cutBitmap = autoCutProcessor.removeBackground(bitmap).getOrThrow()
            val file = File(context.cacheDir, "autocut_${System.currentTimeMillis()}.png")
            FileOutputStream(file).use { out -> cutBitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out) }
            file
        }
    }
}
