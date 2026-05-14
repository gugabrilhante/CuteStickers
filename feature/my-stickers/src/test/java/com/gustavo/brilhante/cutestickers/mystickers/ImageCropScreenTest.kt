package com.gustavo.brilhante.cutestickers.mystickers

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImageCropScreenTest {

    @Test
    fun computeCropRegion_centeredImage_returnsCorrectBitmapCoords() {
        // 400x400 box, 200x200 crop window, 1:1 image at scale 1.0 filling box exactly
        val (bmpX, bmpY, bmpSize) = computeCropRegion(
            cropLeft = 100f, cropTop = 100f, cropSizePx = 200f,
            imgLeft = 0f, imgTop = 0f, totalScale = 1f,
            bmpWidth = 400, bmpHeight = 400
        )

        assertEquals(100, bmpX)
        assertEquals(100, bmpY)
        assertEquals(200, bmpSize)
    }

    @Test
    fun computeCropRegion_zoomedIn_returnsSmallBitmapRegion() {
        // Image displayed at 2x scale — crop window sees half the pixels
        val (_, _, bmpSize) = computeCropRegion(
            cropLeft = 100f, cropTop = 100f, cropSizePx = 200f,
            imgLeft = -200f, imgTop = -200f, totalScale = 2f,
            bmpWidth = 400, bmpHeight = 400
        )

        assertEquals(100, bmpSize) // 200px / 2.0 scale = 100 bitmap pixels
    }

    @Test
    fun computeCropRegion_clampsToBitmapBounds() {
        // Crop window extends beyond bitmap edge
        val (bmpX, bmpY, bmpSize) = computeCropRegion(
            cropLeft = 0f, cropTop = 0f, cropSizePx = 300f,
            imgLeft = 0f, imgTop = 0f, totalScale = 1f,
            bmpWidth = 200, bmpHeight = 200
        )

        assertEquals(0, bmpX)
        assertEquals(0, bmpY)
        assertTrue(bmpX + bmpSize <= 200)
        assertTrue(bmpY + bmpSize <= 200)
    }
}
