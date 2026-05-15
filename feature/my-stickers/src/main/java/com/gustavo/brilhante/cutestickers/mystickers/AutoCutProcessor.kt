package com.gustavo.brilhante.cutestickers.mystickers

import android.graphics.Bitmap

interface AutoCutProcessor {
    suspend fun removeBackground(bitmap: Bitmap): Result<Bitmap>
}
