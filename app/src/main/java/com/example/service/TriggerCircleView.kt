package com.example.service

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.MotionEvent
import android.view.View
import kotlin.math.hypot
import kotlin.math.min

/**
 * Lightweight custom View for the floating circular Trigger overlay.
 *
 * Uses direct Canvas drawing (no Compose overhead inside WindowManager) for optimal RAM/CPU
 * efficiency on entry-level Android 11 hardware such as Nokia C30.
 *
 * Touch behavior:
 * - Bounded strictly to this circular window; outside areas pass touches directly to the game.
 * - ACTION_DOWN inside the circle starts hold-to-tap immediately.
 * - ACTION_UP / ACTION_CANCEL / detachment stops hold-to-tap immediately.
 */
@SuppressLint("ViewConstructor")
class TriggerCircleView(
    context: Context,
    private val onHoldStart: () -> Unit,
    private val onHoldStop: () -> Unit
) : View(context) {

    private var isTriggerHeld = false

    private val idleFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb(42, 255, 255, 255) // Subtle translucent white
    }

    private val activeFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb(64, 0, 230, 118) // Subtle emerald glow while held
    }

    private val idleBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dpToPx(1.75f)
        color = Color.argb(185, 255, 255, 255)
    }

    private val activeBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dpToPx(2.5f)
        color = Color.rgb(0, 230, 118)
    }

    private val centerDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb(145, 255, 255, 255)
    }

    private val activeDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.rgb(0, 230, 118)
    }

    init {
        isClickable = false
        isFocusable = false
        contentDescription = "GLOW FIRST Floating Trigger"
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val borderPaint = if (isTriggerHeld) activeBorderPaint else idleBorderPaint
        val fillPaint = if (isTriggerHeld) activeFillPaint else idleFillPaint
        val dotPaint = if (isTriggerHeld) activeDotPaint else centerDotPaint
        val radius = (min(width, height) / 2f) - borderPaint.strokeWidth

        if (radius <= 0f) return

        canvas.drawCircle(cx, cy, radius, fillPaint)
        canvas.drawCircle(cx, cy, radius, borderPaint)
        canvas.drawCircle(cx, cy, dpToPx(3.5f), dotPaint)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val cx = width / 2f
                val cy = height / 2f
                val radius = min(width, height) / 2f
                val dist = hypot((event.x - cx).toDouble(), (event.y - cy).toDouble()).toFloat()
                if (dist <= radius) {
                    setHeldState(true)
                    onHoldStart()
                    return true
                }
                return false
            }

            MotionEvent.ACTION_MOVE -> {
                return isTriggerHeld
            }

            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                if (isTriggerHeld) {
                    setHeldState(false)
                    onHoldStop()
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (visibility != VISIBLE && isTriggerHeld) {
            setHeldState(false)
            onHoldStop()
        }
    }

    override fun onDetachedFromWindow() {
        if (isTriggerHeld) {
            setHeldState(false)
            onHoldStop()
        }
        super.onDetachedFromWindow()
    }

    private fun setHeldState(held: Boolean) {
        if (isTriggerHeld != held) {
            isTriggerHeld = held
            invalidate()
        }
    }

    private fun dpToPx(dp: Float): Float {
        return dp * resources.displayMetrics.density
    }
}
