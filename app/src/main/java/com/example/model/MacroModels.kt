package com.example.model

object MacroConstants {
    const val MIN_CPS = 1
    const val MAX_CPS = 30
    const val DEFAULT_CPS = 13

    const val MIN_TRIGGER_SIZE_DP = 48f
    const val MAX_TRIGGER_SIZE_DP = 132f
    const val DEFAULT_TRIGGER_SIZE_DP = 68f

    const val MIN_TAP_SIZE_DP = 40f
    const val MAX_TAP_SIZE_DP = 120f
    const val DEFAULT_TAP_SIZE_DP = 56f

    val CPS_PRESETS = listOf(5, 10, 13, 20)
}

data class MacroConfig(
    val isConfigured: Boolean = false,
    val screenshotPath: String = "",
    val screenshotWidth: Int = 0,
    val screenshotHeight: Int = 0,
    // Trigger position normalized (0f..1f) relative to the editor/game screen
    val triggerNormX: Float = 0.16f,
    val triggerNormY: Float = 0.72f,
    val triggerSizeDp: Float = MacroConstants.DEFAULT_TRIGGER_SIZE_DP,
    // Tap center position normalized (0f..1f) relative to the selected screenshot
    val tapNormX: Float = 0.78f,
    val tapNormY: Float = 0.65f,
    val tapSizeDp: Float = MacroConstants.DEFAULT_TAP_SIZE_DP,
    val cps: Int = MacroConstants.DEFAULT_CPS,
    val overlayActive: Boolean = false,
    val hasCompletedPermissionSetup: Boolean = false
) {
    val isLandscapeScreenshot: Boolean
        get() = screenshotWidth > screenshotHeight && screenshotWidth > 0
}

data class PermissionStatus(
    val overlayGranted: Boolean = false,
    val accessibilityGranted: Boolean = false,
    val notificationGranted: Boolean = false,
    val batteryOptimizationIgnored: Boolean = false,
    val needsNotificationRuntimePermission: Boolean = false
) {
    val allCoreGranted: Boolean
        get() = overlayGranted && accessibilityGranted && notificationGranted
}

data class MacroEngineState(
    val isHoldingTrigger: Boolean = false,
    val isAccessibilityConnected: Boolean = false,
    val isOverlayServiceRunning: Boolean = false,
    val sessionTapCount: Long = 0L,
    val lastErrorMessage: String? = null
)
