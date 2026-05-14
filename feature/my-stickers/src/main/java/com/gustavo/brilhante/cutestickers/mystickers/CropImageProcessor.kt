package com.gustavo.brilhante.cutestickers.mystickers

import android.graphics.Bitmap
import android.net.Uri

interface CropImageProcessor {
    suspend fun loadBitmapWithExifCorrection(uri: Uri): Bitmap?
    suspend fun saveCroppedBitmap(source: Bitmap, x: Int, y: Int, size: Int, rotation: Float): Uri
}
