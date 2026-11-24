package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.MacroConfig
import com.example.model.MacroConstants
import com.example.util.ScreenshotStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.macroDataStore: DataStore<Preferences> by preferencesDataStore(name = "glow_first_macro")

class MacroRepository(private val context: Context) {

    private object Keys {
        val IS_CONFIGURED = booleanPreferencesKey("is_configured")
        val SCREENSHOT_PATH = stringPreferencesKey("screenshot_path")
        val SCREENSHOT_WIDTH = intPreferencesKey("screenshot_width")
        val SCREENSHOT_HEIGHT = intPreferencesKey("screenshot_height")
        val TRIGGER_NORM_X = floatPreferencesKey("trigger_norm_x")
        val TRIGGER_NORM_Y = floatPreferencesKey("trigger_norm_y")
        val TRIGGER_SIZE_DP = floatPreferencesKey("trigger_size_dp")
        val TAP_NORM_X = floatPreferencesKey("tap_norm_x")
        val TAP_NORM_Y = floatPreferencesKey("tap_norm_y")
        val TAP_SIZE_DP = floatPreferencesKey("tap_size_dp")
        val CPS = intPreferencesKey("cps")
        val OVERLAY_ACTIVE = booleanPreferencesKey("overlay_active")
        val HAS_COMPLETED_PERMISSION_SETUP = booleanPreferencesKey("has_completed_permission_setup")
    }

    val macroConfigFlow: Flow<MacroConfig> = context.macroDataStore.data.map { prefs ->
        val savedPath = prefs[Keys.SCREENSHOT_PATH] ?: ""
        val validScreenshot = ScreenshotStorage.isScreenshotValid(savedPath)
        val configured = (prefs[Keys.IS_CONFIGURED] ?: false) && validScreenshot
        MacroConfig(
            isConfigured = configured,
            screenshotPath = if (validScreenshot) savedPath else "",
            screenshotWidth = if (validScreenshot) (prefs[Keys.SCREENSHOT_WIDTH] ?: 0) else 0,
            screenshotHeight = if (validScreenshot) (prefs[Keys.SCREENSHOT_HEIGHT] ?: 0) else 0,
            triggerNormX = (prefs[Keys.TRIGGER_NORM_X] ?: 0.16f).coerceIn(0f, 1f),
            triggerNormY = (prefs[Keys.TRIGGER_NORM_Y] ?: 0.72f).coerceIn(0f, 1f),
            triggerSizeDp = (prefs[Keys.TRIGGER_SIZE_DP] ?: MacroConstants.DEFAULT_TRIGGER_SIZE_DP)
                .coerceIn(MacroConstants.MIN_TRIGGER_SIZE_DP, MacroConstants.MAX_TRIGGER_SIZE_DP),
            tapNormX = (prefs[Keys.TAP_NORM_X] ?: 0.78f).coerceIn(0f, 1f),
            tapNormY = (prefs[Keys.TAP_NORM_Y] ?: 0.65f).coerceIn(0f, 1f),
            tapSizeDp = (prefs[Keys.TAP_SIZE_DP] ?: MacroConstants.DEFAULT_TAP_SIZE_DP)
                .coerceIn(MacroConstants.MIN_TAP_SIZE_DP, MacroConstants.MAX_TAP_SIZE_DP),
            cps = (prefs[Keys.CPS] ?: MacroConstants.DEFAULT_CPS)
                .coerceIn(MacroConstants.MIN_CPS, MacroConstants.MAX_CPS),
            overlayActive = (prefs[Keys.OVERLAY_ACTIVE] ?: false) && configured,
            hasCompletedPermissionSetup = prefs[Keys.HAS_COMPLETED_PERMISSION_SETUP] ?: false
        )
    }

    suspend fun saveScreenshotReference(path: String, width: Int, height: Int) {
        context.macroDataStore.edit { prefs ->
            prefs[Keys.SCREENSHOT_PATH] = path
            prefs[Keys.SCREENSHOT_WIDTH] = width
            prefs[Keys.SCREENSHOT_HEIGHT] = height
        }
    }

    suspend fun saveMacroConfiguration(config: MacroConfig) {
        context.macroDataStore.edit { prefs ->
            prefs[Keys.IS_CONFIGURED] = true
            prefs[Keys.SCREENSHOT_PATH] = config.screenshotPath
            prefs[Keys.SCREENSHOT_WIDTH] = config.screenshotWidth
            prefs[Keys.SCREENSHOT_HEIGHT] = config.screenshotHeight
            prefs[Keys.TRIGGER_NORM_X] = config.triggerNormX.coerceIn(0f, 1f)
            prefs[Keys.TRIGGER_NORM_Y] = config.triggerNormY.coerceIn(0f, 1f)
            prefs[Keys.TRIGGER_SIZE_DP] = config.triggerSizeDp.coerceIn(
                MacroConstants.MIN_TRIGGER_SIZE_DP,
                MacroConstants.MAX_TRIGGER_SIZE_DP
            )
            prefs[Keys.TAP_NORM_X] = config.tapNormX.coerceIn(0f, 1f)
            prefs[Keys.TAP_NORM_Y] = config.tapNormY.coerceIn(0f, 1f)
            prefs[Keys.TAP_SIZE_DP] = config.tapSizeDp.coerceIn(
                MacroConstants.MIN_TAP_SIZE_DP,
                MacroConstants.MAX_TAP_SIZE_DP
            )
            prefs[Keys.CPS] = config.cps.coerceIn(MacroConstants.MIN_CPS, MacroConstants.MAX_CPS)
            prefs[Keys.OVERLAY_ACTIVE] = config.overlayActive
        }
    }

    suspend fun setOverlayActive(active: Boolean) {
        context.macroDataStore.edit { prefs ->
            prefs[Keys.OVERLAY_ACTIVE] = active
        }
    }

    suspend fun setPermissionSetupCompleted(completed: Boolean) {
        context.macroDataStore.edit { prefs ->
            prefs[Keys.HAS_COMPLETED_PERMISSION_SETUP] = completed
        }
    }

    suspend fun clearMacro() {
        context.macroDataStore.edit { prefs ->
            val currentPath = prefs[Keys.SCREENSHOT_PATH]
            if (!currentPath.isNullOrBlank()) {
                runCatching { java.io.File(currentPath).delete() }
            }
            prefs[Keys.IS_CONFIGURED] = false
            prefs[Keys.SCREENSHOT_PATH] = ""
            prefs[Keys.SCREENSHOT_WIDTH] = 0
            prefs[Keys.SCREENSHOT_HEIGHT] = 0
            prefs[Keys.OVERLAY_ACTIVE] = false
        }
    }
}
