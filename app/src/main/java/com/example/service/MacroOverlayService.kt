package com.example.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.example.GlowFirstApplication
import com.example.MainActivity
import com.example.R
import com.example.model.MacroConfig
import com.example.util.TapCoordinateMapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class MacroOverlayService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var configJob: Job? = null

    private var windowManager: WindowManager? = null
    private var triggerView: TriggerCircleView? = null
    private var overlayParams: WindowManager.LayoutParams? = null
    private var latestConfig: MacroConfig = MacroConfig()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val app = applicationContext as GlowFirstApplication
        app.macroEngine.setOverlayServiceRunning(true)

        startForegroundSafely(latestConfig.cps)

        configJob = serviceScope.launch {
            app.macroRepository.macroConfigFlow.collectLatest { config ->
                latestConfig = config
                app.macroEngine.updateConfig(config)
                if (!config.isConfigured || !config.overlayActive || !Settings.canDrawOverlays(this@MacroOverlayService)) {
                    stopSelfAndDisableOverlay()
                } else {
                    ensureOrUpdateTriggerOverlay(config)
                    updateNotification(config.cps)
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_OVERLAY) {
            stopSelfAndDisableOverlay()
            return START_NOT_STICKY
        }

        if (!Settings.canDrawOverlays(this)) {
            stopSelfAndDisableOverlay()
            return START_NOT_STICKY
        }

        startForegroundSafely(latestConfig.cps)
        return START_STICKY
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (latestConfig.isConfigured && latestConfig.overlayActive) {
            ensureOrUpdateTriggerOverlay(latestConfig)
        }
    }

    override fun onDestroy() {
        val app = applicationContext as GlowFirstApplication
        app.macroEngine.stopHoldTap()
        app.macroEngine.setOverlayServiceRunning(false)
        removeTriggerView()
        configJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun stopSelfAndDisableOverlay() {
        val app = applicationContext as GlowFirstApplication
        app.macroEngine.stopHoldTap()
        serviceScope.launch {
            app.macroRepository.setOverlayActive(false)
            stopSelf()
        }
    }

    private fun ensureOrUpdateTriggerOverlay(config: MacroConfig) {
        val wm = windowManager ?: return
        val density = resources.displayMetrics.density
        val sizePx = (config.triggerSizeDp * density).roundToInt().coerceAtLeast(48)
        val (displayW, displayH) = readRealDisplaySize(wm)
        val pos = TapCoordinateMapper.mapTriggerToOverlayParams(
            triggerNormX = config.triggerNormX,
            triggerNormY = config.triggerNormY,
            overlaySizePx = sizePx,
            displayWidth = displayW,
            displayHeight = displayH
        )

        val existingView = triggerView
        val existingParams = overlayParams
        if (existingView == null || existingParams == null) {
            val windowType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            val params = WindowManager.LayoutParams(
                sizePx,
                sizePx,
                windowType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = pos.x
                y = pos.y
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    layoutInDisplayCutoutMode =
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                }
            }

            val app = applicationContext as GlowFirstApplication
            val view = TriggerCircleView(
                context = this,
                onHoldStart = { app.macroEngine.startHoldTap() },
                onHoldStop = { app.macroEngine.stopHoldTap() }
            )

            try {
                wm.addView(view, params)
                triggerView = view
                overlayParams = params
            } catch (_: Throwable) {
                stopSelfAndDisableOverlay()
            }
        } else {
            val changed = existingParams.width != sizePx ||
                existingParams.height != sizePx ||
                existingParams.x != pos.x ||
                existingParams.y != pos.y
            if (changed) {
                existingParams.width = sizePx
                existingParams.height = sizePx
                existingParams.x = pos.x
                existingParams.y = pos.y
                runCatching { wm.updateViewLayout(existingView, existingParams) }
            }
        }
    }

    private fun removeTriggerView() {
        val view = triggerView ?: return
        val wm = windowManager ?: return
        runCatching { wm.removeViewImmediate(view) }
        triggerView = null
        overlayParams = null
    }

    private fun readRealDisplaySize(wm: WindowManager): Pair<Int, Int> {
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

    private fun startForegroundSafely(cps: Int) {
        val notification = buildNotification(cps)
        val fgsType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, fgsType)
    }

    private fun updateNotification(cps: Int) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        nm.notify(NOTIFICATION_ID, buildNotification(cps))
    }

    private fun buildNotification(cps: Int): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, MacroOverlayService::class.java).setAction(ACTION_STOP_OVERLAY),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, GlowFirstApplication.CHANNEL_OVERLAY)
            .setSmallIcon(R.drawable.ic_stat_glow)
            .setContentTitle(getString(R.string.notification_overlay_title))
            .setContentText(getString(R.string.notification_overlay_text, cps))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(openAppIntent)
            .addAction(0, getString(R.string.action_stop_overlay), stopIntent)
            .build()
    }

    companion object {
        const val NOTIFICATION_ID = 1001
        const val ACTION_STOP_OVERLAY = "com.example.action.STOP_OVERLAY"

        fun start(context: Context) {
            val intent = Intent(context, MacroOverlayService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, MacroOverlayService::class.java)
                .setAction(ACTION_STOP_OVERLAY)
            runCatching { context.startService(intent) }
            runCatching { context.stopService(Intent(context, MacroOverlayService::class.java)) }
        }
    }
}
