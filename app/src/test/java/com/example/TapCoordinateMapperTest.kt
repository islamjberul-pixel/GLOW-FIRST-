package com.example

import com.example.util.CpsValidationResult
import com.example.util.TapCoordinateMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TapCoordinateMapperTest {

    @Test
    fun `fitted image rect preserves landscape aspect ratio inside portrait container`() {
        val rect = TapCoordinateMapper.computeFittedImageRect(
            imageWidth = 1600,
            imageHeight = 800,
            containerWidth = 800f,
            containerHeight = 1600f
        )
        assertEquals(0f, rect.left, 0.01f)
        assertEquals(800f, rect.width, 0.01f)
        assertEquals(400f, rect.height, 0.01f)
        assertEquals(600f, rect.top, 0.01f)
    }

    @Test
    fun `screenshot to screen round-trip preserves exact center coordinates`() {
        val rect = TapCoordinateMapper.computeFittedImageRect(
            imageWidth = 1600,
            imageHeight = 720,
            containerWidth = 720f,
            containerHeight = 1600f
        )
        val norm = TapCoordinateMapper.screenPointToNormalizedScreenshot(
            screenX = 540f,
            screenY = rect.top + rect.height * 0.25f,
            imageRect = rect
        )
        assertEquals(0.75f, norm.x, 0.001f)
        assertEquals(0.25f, norm.y, 0.001f)

        val restored = TapCoordinateMapper.normalizedScreenshotToScreenPoint(norm.x, norm.y, rect)
        assertEquals(540f, restored.x, 0.5f)
        assertEquals(rect.top + rect.height * 0.25f, restored.y, 0.5f)
    }

    @Test
    fun `mapScreenshotToDisplay scales landscape screenshot to Nokia C30 landscape display`() {
        // Different resolution (1280x720) mapped onto 1600x720 landscape display
        val pt = TapCoordinateMapper.mapScreenshotToDisplay(
            tapNormX = 0.5f,
            tapNormY = 0.5f,
            screenshotWidth = 1280,
            screenshotHeight = 720,
            displayWidth = 1600,
            displayHeight = 720
        )
        assertEquals(800f, pt.x, 1.0f)
        assertEquals(360f, pt.y, 1.0f)
    }

    @Test
    fun `mapTriggerToOverlayParams clamps trigger safely inside display bounds`() {
        val pos = TapCoordinateMapper.mapTriggerToOverlayParams(
            triggerNormX = 1.0f,
            triggerNormY = 1.0f,
            overlaySizePx = 120,
            displayWidth = 1600,
            displayHeight = 720
        )
        assertEquals(1480, pos.x)
        assertEquals(600, pos.y)
    }

    @Test
    fun `cps validation enforces 1 to 30 range`() {
        assertTrue(TapCoordinateMapper.validateCps("1") is CpsValidationResult.Valid)
        assertTrue(TapCoordinateMapper.validateCps("13") is CpsValidationResult.Valid)
        assertTrue(TapCoordinateMapper.validateCps("30") is CpsValidationResult.Valid)
        assertTrue(TapCoordinateMapper.validateCps("0") is CpsValidationResult.Invalid)
        assertTrue(TapCoordinateMapper.validateCps("31") is CpsValidationResult.Invalid)
        assertTrue(TapCoordinateMapper.validateCps("") is CpsValidationResult.Invalid)
        assertTrue(TapCoordinateMapper.validateCps("abc") is CpsValidationResult.Invalid)
    }

    @Test
    fun `13 cps calculates ~76_9ms interval and non-overlapping stroke duration`() {
        val intervalMs = TapCoordinateMapper.cpsToExactIntervalMs(13)
        assertEquals(76.923, intervalMs, 0.01)

        val stroke30 = TapCoordinateMapper.cpsToTapStrokeDurationMs(30)
        assertTrue(stroke30 in 10L..16L)
    }
}
