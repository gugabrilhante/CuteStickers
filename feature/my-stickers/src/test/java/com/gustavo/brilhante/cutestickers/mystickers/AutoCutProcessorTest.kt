package com.gustavo.brilhante.cutestickers.mystickers

import com.gustavo.brilhante.cutestickers.mystickers.data.applyConfidenceToPixels
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoCutProcessorTest {

    @Test
    fun applyConfidenceToPixels_fullConfidence_setsAlphaTo255() {
        val pixels = intArrayOf(0x00FF0000.toInt()) // red, no alpha
        val result = applyConfidenceToPixels(pixels, floatArrayOf(1.0f))
        val alpha = (result[0] ushr 24) and 0xFF
        assertEquals(255, alpha)
    }

    @Test
    fun applyConfidenceToPixels_zeroConfidence_setsAlphaTo0() {
        val pixels = intArrayOf(-0x1) // 0xFFFFFFFF - white, fully opaque
        val result = applyConfidenceToPixels(pixels, floatArrayOf(0.0f))
        val alpha = (result[0] ushr 24) and 0xFF
        assertEquals(0, alpha)
    }

    @Test
    fun applyConfidenceToPixels_halfConfidence_setsAlphaTo127() {
        val pixels = intArrayOf(0x00FFFFFF)
        val result = applyConfidenceToPixels(pixels, floatArrayOf(0.5f))
        val alpha = (result[0] ushr 24) and 0xFF
        assertEquals(127, alpha) // floor(0.5 * 255) = 127
    }

    @Test
    fun applyConfidenceToPixels_aboveOne_clampsAlphaTo255() {
        val pixels = intArrayOf(0x00ABCDEF.toInt())
        val result = applyConfidenceToPixels(pixels, floatArrayOf(1.5f))
        val alpha = (result[0] ushr 24) and 0xFF
        assertEquals(255, alpha)
    }

    @Test
    fun applyConfidenceToPixels_negative_clampsAlphaTo0() {
        val pixels = intArrayOf(-0x1)
        val result = applyConfidenceToPixels(pixels, floatArrayOf(-0.5f))
        val alpha = (result[0] ushr 24) and 0xFF
        assertEquals(0, alpha)
    }

    @Test
    fun applyConfidenceToPixels_preservesRgbChannels() {
        val rgb = 0x00102030 // R=0x10, G=0x20, B=0x30, A=0
        val pixels = intArrayOf(rgb)
        val result = applyConfidenceToPixels(pixels, floatArrayOf(1.0f))
        assertEquals(0xFF102030.toInt(), result[0])
    }

    @Test
    fun applyConfidenceToPixels_handlesMultiplePixelsIndependently() {
        val pixels = intArrayOf(0x00FF0000.toInt(), 0x0000FF00.toInt())
        val mask = floatArrayOf(1.0f, 0.0f)
        val result = applyConfidenceToPixels(pixels, mask)
        assertEquals(255, (result[0] ushr 24) and 0xFF)
        assertEquals(0, (result[1] ushr 24) and 0xFF)
    }

    @Test
    fun applyConfidenceToPixels_outputLengthMatchesInput() {
        val pixels = IntArray(100) { 0x00FFFFFF }
        val mask = FloatArray(100) { 0.8f }
        val result = applyConfidenceToPixels(pixels, mask)
        assertEquals(100, result.size)
    }

    @Test
    fun applyConfidenceToPixels_highConfidence_producesNearOpaqueAlpha() {
        val pixels = intArrayOf(0x00FFFFFF)
        val result = applyConfidenceToPixels(pixels, floatArrayOf(0.99f))
        val alpha = (result[0] ushr 24) and 0xFF
        assertTrue("Expected alpha near 255, got $alpha", alpha >= 250)
    }
}
