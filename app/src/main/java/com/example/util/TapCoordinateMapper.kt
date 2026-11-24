package com.example.util

import com.example.model.MacroConstants
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class RectBounds(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float
) {
    val right: Float get() = left + width
    val bottom: Float get() = top + height
}

data class PointPx(
    val x: Float,
    val y: Float
)

data class OverlayPositionPx(
    val x: Int,
    val y: Int
)

sealed class CpsValidationResult {
    data class Valid(val cps: Int) : CpsValidationResult()
    data class Invalid(val reason: String) : CpsValidationResult()
}

object TapCoordinateMapper {

    /**
     * Computes the exact fitted rectangle of an image inside a container using ContentScale.Fit.
     */
    fun computeFittedImageRect(
        imageWidth: Int,
        imageHeight: Int,
        containerWidth: Float,
        containerHeight: Float
    ): RectBounds {
        if (imageWidth <= 0 || imageHeight <= 0 || containerWidth <= 0f || containerHeight <= 0f) {
            return RectBounds(0f, 0f, containerWidth.coerceAtLeast(1f), containerHeight.coerceAtLeast(1f))
        }
        val imageAspect = imageWidth.toFloat() / imageHeight.toFloat()
        val containerAspect = containerWidth / containerHeight

        val drawnWidth: Float
        val drawnHeight: Float
        if (imageAspect > containerAspect) {
            drawnWidth = containerWidth
            drawnHeight = containerWidth / imageAspect
        } else {
            drawnHeight = containerHeight
            drawnWidth = containerHeight * imageAspect
        }
        val left = (containerWidth - drawnWidth) / 2f
        val top = (containerHeight - drawnHeight) / 2f
        return RectBounds(left = left, top = top, width = drawnWidth, height = drawnHeight)
    }

    /**
     * Converts editor screen coordinates into normalized [0f..1f] coordinates relative to the
     * fitted screenshot bounds.
     */
    fun screenPointToNormalizedScreenshot(
        screenX: Float,
        screenY: Float,
        imageRect: RectBounds
    ): PointPx {
        val safeW = imageRect.width.coerceAtLeast(1f)
        val safeH = imageRect.height.coerceAtLeast(1f)
        val normX = ((screenX - imageRect.left) / safeW).coerceIn(0f, 1f)
        val normY = ((screenY - imageRect.top) / safeH).coerceIn(0f, 1f)
        return PointPx(normX, normY)
    }

    /**
     * Converts normalized [0f..1f] screenshot coordinates back into editor screen coordinates.
     */
    fun normalizedScreenshotToScreenPoint(
        normX: Float,
        normY: Float,
        imageRect: RectBounds
    ): PointPx {
        return PointPx(
            x = imageRect.left + normX.coerceIn(0f, 1f) * imageRect.width,
            y = imageRect.top + normY.coerceIn(0f, 1f) * imageRect.height
        )
    }

    /**
     * Converts normalized screenshot coordinates (saved relative to the screenshot image) into
     * physical display pixel coordinates for AccessibilityService.dispatchGesture.
     *
     * Accounts for:
     * - Screenshot resolution != Device display resolution
     * - Aspect ratio preservation (letterbox/pillarbox fit)
     * - Display rotation (portrait vs landscape)
     */
    fun mapScreenshotToDisplay(
        tapNormX: Float,
        tapNormY: Float,
        screenshotWidth: Int,
        screenshotHeight: Int,
        displayWidth: Int,
        displayHeight: Int
    ): PointPx {
        val safeDisplayW = displayWidth.coerceAtLeast(1).toFloat()
        val safeDisplayH = displayHeight.coerceAtLeast(1).toFloat()

        val clampedX = tapNormX.coerceIn(0f, 1f)
        val clampedY = tapNormY.coerceIn(0f, 1f)

        if (screenshotWidth <= 0 || screenshotHeight <= 0) {
            return PointPx(
                x = (clampedX * safeDisplayW).coerceIn(1f, max(1f, safeDisplayW - 1f)),
                y = (clampedY * safeDisplayH).coerceIn(1f, max(1f, safeDisplayH - 1f))
            )
        }

        val screenshotIsLandscape = screenshotWidth > screenshotHeight
        val displayIsLandscape = displayWidth > displayHeight

        // If the game screenshot orientation matches the active display orientation (the standard
        // case while playing the game), fit the screenshot into the physical display bounds.
        // If orientations differ (e.g. user tests while still in portrait before opening a
        // landscape game), map directly into the fitted aspect rect on the current screen so the
        // tap still targets the visual location safely within display bounds.
        val fittedRect = computeFittedImageRect(
            imageWidth = screenshotWidth,
            imageHeight = screenshotHeight,
            containerWidth = safeDisplayW,
            containerHeight = safeDisplayH
        )

        val mapped = if (screenshotIsLandscape == displayIsLandscape) {
            // If screenshot aspect is within 6% of display aspect, games typically stretch/fill
            // edge-to-edge across cutouts/nav bars, so direct normalized mapping avoids slight
            // inset drift; otherwise preserve exact fitted aspect ratio.
            val ssAspect = screenshotWidth.toFloat() / screenshotHeight.toFloat()
            val dispAspect = safeDisplayW / safeDisplayH
            val aspectRatioDelta = kotlin.math.abs(ssAspect - dispAspect) / max(ssAspect, dispAspect)
            if (aspectRatioDelta <= 0.06f) {
                PointPx(clampedX * safeDisplayW, clampedY * safeDisplayH)
            } else {
                normalizedScreenshotToScreenPoint(clampedX, clampedY, fittedRect)
            }
        } else {
            normalizedScreenshotToScreenPoint(clampedX, clampedY, fittedRect)
        }

        return PointPx(
            x = mapped.x.coerceIn(1f, max(1f, safeDisplayW - 1f)),
            y = mapped.y.coerceIn(1f, max(1f, safeDisplayH - 1f))
        )
    }

    /**
     * Converts normalized Trigger center coordinates (0f..1f) into WindowManager top-left
     * (x, y) pixel coordinates, clamped so the Trigger circle remains fully on-screen.
     */
    fun mapTriggerToOverlayParams(
        triggerNormX: Float,
        triggerNormY: Float,
        overlaySizePx: Int,
        displayWidth: Int,
        displayHeight: Int
    ): OverlayPositionPx {
        val half = overlaySizePx / 2f
        val maxX = max(0, displayWidth - overlaySizePx)
        val maxY = max(0, displayHeight - overlaySizePx)
        val rawX = (triggerNormX.coerceIn(0f, 1f) * displayWidth - half).roundToInt()
        val rawY = (triggerNormY.coerceIn(0f, 1f) * displayHeight - half).roundToInt()
        return OverlayPositionPx(
            x = rawX.coerceIn(0, maxX),
            y = rawY.coerceIn(0, maxY)
        )
    }

    fun validateCps(input: String): CpsValidationResult {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) {
            return CpsValidationResult.Invalid("Enter a CPS value between ${MacroConstants.MIN_CPS} and ${MacroConstants.MAX_CPS}.")
        }
        val parsed = trimmed.toIntOrNull()
            ?: return CpsValidationResult.Invalid("CPS must be a whole number.")
        if (parsed !in MacroConstants.MIN_CPS..MacroConstants.MAX_CPS) {
            return CpsValidationResult.Invalid("CPS must be between ${MacroConstants.MIN_CPS} and ${MacroConstants.MAX_CPS}.")
        }
        return CpsValidationResult.Valid(parsed)
    }

    /**
     * Exact average interval in milliseconds for a given CPS (e.g., 13 CPS -> 76.923 ms).
     */
    fun cpsToExactIntervalMs(cps: Int): Double {
        val safeCps = cps.coerceIn(MacroConstants.MIN_CPS, MacroConstants.MAX_CPS)
        return 1000.0 / safeCps.toDouble()
    }

    /**
     * Safe stroke duration for dispatchGesture so high CPS (up to 30 CPS = 33.3ms period)
     * never overlaps the next tap while still registering reliably as a tap event.
     */
    fun cpsToTapStrokeDurationMs(cps: Int): Long {
        val intervalMs = cpsToExactIntervalMs(cps).toLong()
        return min(22L, max(10L, intervalMs / 2L))
    }
}
