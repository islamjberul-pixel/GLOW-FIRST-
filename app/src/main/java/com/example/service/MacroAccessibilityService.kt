package com.example.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.example.GlowFirstApplication
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MacroAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        _isConnectedFlow.value = true
        (applicationContext as? GlowFirstApplication)?.macroEngine?.setAccessibilityConnected(true)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Event-driven architecture: we do not inspect window contents or poll UI trees.
    }

    override fun onInterrupt() {
        (applicationContext as? GlowFirstApplication)?.macroEngine?.stopHoldTap()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        disconnectEngine()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        disconnectEngine()
        super.onDestroy()
    }

    private fun disconnectEngine() {
        if (instance === this) {
            instance = null
        }
        _isConnectedFlow.value = false
        (applicationContext as? GlowFirstApplication)?.macroEngine?.setAccessibilityConnected(false)
    }

    companion object {
        @Volatile
        var instance: MacroAccessibilityService? = null
            private set

        private val _isConnectedFlow = MutableStateFlow(false)
        val isConnectedFlow: StateFlow<Boolean> = _isConnectedFlow.asStateFlow()
    }
}
