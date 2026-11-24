package com.example.engine

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.graphics.Path
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.WindowManager
import com.example.R
import com.example.model.MacroConfig
import com.example.model.MacroEngineState
import com.example.service.MacroAccessibilityService
import com.example.util.TapCoordinateMapper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.max

class MacroEngine(private val appContext: Context) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val _state = MutableStateFlow(MacroEngineState())
    val state: StateFlow<MacroEngineState> = _state.asStateFlow()

    @Volatile
    private var latestConfig: MacroConfig = MacroConfig()

    private var isHolding = false
    private var isDispatchInFlight = false
    private var holdSessionToken = 0L
    private var nextTapDeadlineNanos = 0L
    private var consecutiveFailures = 0

    private val fireRunnable = Runnable {
        dispatchNextTapIfActive(holdSessionToken)
    }

    fun updateConfig(config: MacroConfig) {
        latestConfig = config
        if (!config.isConfigured || !config.overlayActive) {
            stopHoldTap()
        }
    }

    fun setAccessibilityConnected(connected: Boolean) {
        _state.update {
            it.copy(
                isAccessibilityConnected = connected,
                lastErrorMessage = if (connected && it.lastErrorMessage == appContext.getString(R.string.error_accessibility_disabled)) {
                    null
                } else {
                    it.lastErrorMessage
                }
            )
        }
        if (!connected) {
            stopHoldTap()
        }
    }

    fun setOverlayServiceRunning(running: Boolean) {
        _state.update { it.copy(isOverlayServiceRunning = running) }
        if (!running) {
            stopHoldTap()
        }
    }

    fun clearError() {
        _state.update { it.copy(lastErrorMessage = null) }
    }

    fun startHoldTap() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post { startHoldTap() }
            return
        }

        val config = latestConfig
        if (!config.isConfigured) {
            _state.update {
                it.copy(
                    isHoldingTrigger = false,
                    lastErrorMessage = appContext.getString(R.string.error_macro_not_configured)
                )
            }
            return
        }

        val service = MacroAccessibilityService.instance
        if (service == null || !MacroAccessibilityService.isConnectedFlow.value) {
            _state.update {
                it.copy(
                    isHoldingTrigger = false,
                    isAccessibilityConnected = false,
                    lastErrorMessage = appContext.getString(R.string.error_accessibility_disabled)
                )
            }
            return
        }

        if (isHolding) return

        holdSessionToken++
        isHolding = true
        isDispatchInFlight = false
        consecutiveFailures = 0
        nextTapDeadlineNanos = System.nanoTime()

        _state.update {
            it.copy(
                isHoldingTrigger = true,
                lastErrorMessage = null
            )
        }

        dispatchNextTapIfActive(holdSessionToken)
    }

    fun stopHoldTap() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post { stopHoldTap() }
            return
        }
        holdSessionToken++
        isHolding = false
        isDispatchInFlight = false
        consecutiveFailures = 0
        mainHandler.removeCallbacks(fireRunnable)
        _state.update { it.copy(isHoldingTrigger = false) }
    }

    private fun dispatchNextTapIfActive(token: Long) {
        if (!isHolding || token != holdSessionToken || isDispatchInFlight) return

        val config = latestConfig
        val service = MacroAccessibilityService.instance
        if (!config.isConfigured || service == null) {
            stopHoldTap()
            if (service == null) {
                _state.update {
                    it.copy(
                        isAccessibilityConnected = false,
                        lastErrorMessage = appContext.getString(R.string.error_accessibility_disabled)
                    )
                }
            }
            return
        }

        val (displayW, displayH) = readRealDisplaySize()
        val tapPoint = TapCoordinateMapper.mapScreenshotToDisplay(
            tapNormX = config.tapNormX,
            tapNormY = config.tapNormY,
            screenshotWidth = config.screenshotWidth,
            screenshotHeight = config.screenshotHeight,
            displayWidth = displayW,
            displayHeight = displayH
        )

        val cps = config.cps
        val intervalNanos = (TapCoordinateMapper.cpsToExactIntervalMs(cps) * 1_000_000.0).toLong()
        val strokeDurationMs = TapCoordinateMapper.cpsToTapStrokeDurationMs(cps)

        val path = Path().apply { moveTo(tapPoint.x, tapPoint.y) }
        val stroke = GestureDescription.StrokeDescription(path, 0L, strokeDurationMs)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()

        val nowNanos = System.nanoTime()
        if (nextTapDeadlineNanos < nowNanos) {
            nextTapDeadlineNanos = nowNanos + intervalNanos
        } else {
            nextTapDeadlineNanos += intervalNanos
        }

        isDispatchInFlight = true
        val accepted = try {
            service.dispatchGesture(
                gesture,
                object : AccessibilityService.GestureResultCallback() {
                    override fun onCompleted(gestureDescription: GestureDescription?) {
                        if (token != holdSessionToken || !isHolding) {
                            isDispatchInFlight = false
                            return
                        }
                        isDispatchInFlight = false
                        consecutiveFailures = 0
                        _state.update { it.copy(sessionTapCount = it.sessionTapCount + 1L) }

                        val delayMs = ((nextTapDeadlineNanos - System.nanoTime()) / 1_000_000L)
                            .coerceAtLeast(0L)
                        mainHandler.removeCallbacks(fireRunnable)
                        mainHandler.postDelayed(fireRunnable, delayMs)
                    }

                    override fun onCancelled(gestureDescription: GestureDescription?) {
                        if (token != holdSessionToken || !isHolding) {
                            isDispatchInFlight = false
                            return
                        }
                        isDispatchInFlight = false
                        // System may cancel a synthetic gesture if simultaneous physical touch
                        // interrupts dispatch; wait one interval before trying again while held.
                        val fallbackDelayMs = max(20L, intervalNanos / 1_000_000L)
                        mainHandler.removeCallbacks(fireRunnable)
                        mainHandler.postDelayed(fireRunnable, fallbackDelayMs)
                    }
                },
                mainHandler
            )
        } catch (t: Throwable) {
            isDispatchInFlight = false
            false
        }

        if (!accepted) {
            isDispatchInFlight = false
            consecutiveFailures++
            if (consecutiveFailures >= 3) {
                stopHoldTap()
                _state.update {
                    it.copy(lastErrorMessage = appContext.getString(R.string.error_gesture_dispatch_failed))
                }
            } else {
                val retryDelayMs = max(25L, intervalNanos / 1_000_000L)
                mainHandler.removeCallbacks(fireRunnable)
                mainHandler.postDelayed(fireRunnable, retryDelayMs)
            }
        }
    }

    private fun readRealDisplaySize(): Pair<Int, Int> {
        val wm = appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = wm.currentWindowMetrics.bounds
            bounds.width() to bounds.height()
        } else {
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            wm.defaultDisplay.getRealMetrics(metrics)
            metrics.widthPixels to metrics.heightPixels
        }
    }
}
