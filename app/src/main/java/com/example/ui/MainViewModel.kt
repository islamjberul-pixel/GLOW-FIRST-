package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.GlowFirstApplication
import com.example.R
import com.example.model.MacroConfig
import com.example.model.MacroConstants
import com.example.model.MacroEngineState
import com.example.model.PermissionStatus
import com.example.service.MacroOverlayService
import com.example.util.PermissionHelper
import com.example.util.ScreenshotStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class UiFeedbackBanner(
    val message: String,
    val isError: Boolean
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as GlowFirstApplication
    private val repository = app.macroRepository
    private val engine = app.macroEngine

    val macroConfig: StateFlow<MacroConfig> = repository.macroConfigFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MacroConfig()
        )

    val engineState: StateFlow<MacroEngineState> = engine.state

    private val _permissionStatus = MutableStateFlow(PermissionHelper.getPermissionStatus(app))
    val permissionStatus: StateFlow<PermissionStatus> = _permissionStatus.asStateFlow()

    private val _isImportingScreenshot = MutableStateFlow(false)
    val isImportingScreenshot: StateFlow<Boolean> = _isImportingScreenshot.asStateFlow()

    private val _feedbackBanner = MutableStateFlow<UiFeedbackBanner?>(null)
    val feedbackBanner: StateFlow<UiFeedbackBanner?> = _feedbackBanner.asStateFlow()

    fun refreshPermissions() {
        val status = PermissionHelper.getPermissionStatus(app)
        _permissionStatus.value = status
        engine.setAccessibilityConnected(status.accessibilityGranted)
        if (!status.overlayGranted && macroConfig.value.overlayActive) {
            viewModelScope.launch {
                repository.setOverlayActive(false)
                MacroOverlayService.stop(app)
            }
        }
    }

    fun markPermissionSetupCompleted() {
        viewModelScope.launch {
            repository.setPermissionSetupCompleted(true)
        }
    }

    fun dismissBanner() {
        _feedbackBanner.value = null
        engine.clearError()
    }

    fun showError(message: String) {
        _feedbackBanner.value = UiFeedbackBanner(message = message, isError = true)
    }

    fun importScreenshot(uri: Uri, onSuccess: () -> Unit) {
        if (_isImportingScreenshot.value) return
        viewModelScope.launch {
            _isImportingScreenshot.value = true
            _feedbackBanner.value = null
            val result = ScreenshotStorage.importScreenshot(app, uri)
            _isImportingScreenshot.value = false
            result.onSuccess { saved ->
                repository.saveScreenshotReference(
                    path = saved.filePath,
                    width = saved.width,
                    height = saved.height
                )
                onSuccess()
            }.onFailure {
                _feedbackBanner.value = UiFeedbackBanner(
                    message = app.getString(R.string.error_screenshot_failed),
                    isError = true
                )
            }
        }
    }

    fun saveKeymapConfiguration(
        triggerNormX: Float,
        triggerNormY: Float,
        triggerSizeDp: Float,
        tapNormX: Float,
        tapNormY: Float,
        tapSizeDp: Float,
        cps: Int,
        onSaved: () -> Unit
    ) {
        val current = macroConfig.value
        if (!ScreenshotStorage.isScreenshotValid(current.screenshotPath)) {
            _feedbackBanner.value = UiFeedbackBanner(
                message = app.getString(R.string.error_screenshot_failed),
                isError = true
            )
            return
        }
        if (cps !in MacroConstants.MIN_CPS..MacroConstants.MAX_CPS) {
            _feedbackBanner.value = UiFeedbackBanner(
                message = app.getString(R.string.error_invalid_cps),
                isError = true
            )
            return
        }

        val permissions = PermissionHelper.getPermissionStatus(app)
        _permissionStatus.value = permissions
        val canShowOverlay = permissions.overlayGranted

        viewModelScope.launch {
            val updated = current.copy(
                isConfigured = true,
                triggerNormX = triggerNormX.coerceIn(0f, 1f),
                triggerNormY = triggerNormY.coerceIn(0f, 1f),
                triggerSizeDp = triggerSizeDp.coerceIn(
                    MacroConstants.MIN_TRIGGER_SIZE_DP,
                    MacroConstants.MAX_TRIGGER_SIZE_DP
                ),
                tapNormX = tapNormX.coerceIn(0f, 1f),
                tapNormY = tapNormY.coerceIn(0f, 1f),
                tapSizeDp = tapSizeDp.coerceIn(
                    MacroConstants.MIN_TAP_SIZE_DP,
                    MacroConstants.MAX_TAP_SIZE_DP
                ),
                cps = cps,
                overlayActive = canShowOverlay
            )
            repository.saveMacroConfiguration(updated)
            engine.updateConfig(updated)

            if (canShowOverlay) {
                MacroOverlayService.start(app)
                if (!permissions.accessibilityGranted) {
                    _feedbackBanner.value = UiFeedbackBanner(
                        message = "Keymap saved & Trigger overlay shown. Enable Accessibility Service to tap.",
                        isError = true
                    )
                } else {
                    _feedbackBanner.value = UiFeedbackBanner(
                        message = "Keymap saved. Floating Trigger is active at $cps CPS.",
                        isError = false
                    )
                }
            } else {
                MacroOverlayService.stop(app)
                _feedbackBanner.value = UiFeedbackBanner(
                    message = app.getString(R.string.error_overlay_permission_missing),
                    isError = true
                )
            }
            onSaved()
        }
    }

    fun setOverlayEnabled(enabled: Boolean) {
        val current = macroConfig.value
        if (enabled) {
            if (!current.isConfigured) {
                showError(app.getString(R.string.error_macro_not_configured))
                return
            }
            val permissions = PermissionHelper.getPermissionStatus(app)
            _permissionStatus.value = permissions
            if (!permissions.overlayGranted) {
                showError(app.getString(R.string.error_overlay_permission_missing))
                return
            }
            if (!permissions.accessibilityGranted) {
                showError(app.getString(R.string.error_accessibility_disabled))
                // Still allow showing overlay or block? Allowing overlay while warning clearly,
                // or blocking until accessibility is enabled — warning clearly is transparent
                // and lets user test overlay placement immediately.
            }
            viewModelScope.launch {
                repository.setOverlayActive(true)
                MacroOverlayService.start(app)
            }
        } else {
            viewModelScope.launch {
                engine.stopHoldTap()
                repository.setOverlayActive(false)
                MacroOverlayService.stop(app)
            }
        }
    }

    fun clearSavedMacro() {
        viewModelScope.launch {
            engine.stopHoldTap()
            MacroOverlayService.stop(app)
            repository.clearMacro()
            _feedbackBanner.value = UiFeedbackBanner(
                message = "Saved macro cleared.",
                isError = false
            )
        }
    }
}
