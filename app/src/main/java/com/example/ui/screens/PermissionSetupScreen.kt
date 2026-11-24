package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AccessibilityNew
import androidx.compose.material.icons.rounded.BatterySaver
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.PermissionStatus
import com.example.ui.components.StatusPill
import com.example.ui.theme.GlowBlack
import com.example.ui.theme.GlowBorderSubtle
import com.example.ui.theme.GlowGreen
import com.example.ui.theme.GlowSurface
import com.example.ui.theme.GlowSurfaceElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.PermissionHelper

@Composable
fun PermissionSetupScreen(
    permissionStatus: PermissionStatus,
    isFirstLaunchOnboarding: Boolean,
    onRefreshPermissions: () -> Unit,
    onContinue: () -> Unit,
    onBack: (() -> Unit)?
) {
    val context = LocalContext.current
    var askedNotificationOnce by remember { mutableStateOf(false) }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        onRefreshPermissions()
        if (!granted && askedNotificationOnce) {
            runCatching {
                context.startActivity(PermissionHelper.buildNotificationSettingsIntent(context))
            }
        }
        askedNotificationOnce = true
    }

    Scaffold(
        containerColor = GlowBlack,
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(listOf(GlowSurface, GlowBlack, GlowBlack))
                )
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .widthIn(max = 600.dp)
                    .align(Alignment.TopCenter),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onBack != null && !isFirstLaunchOnboarding) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("permissions_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Back",
                                tint = TextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "PERMISSION SETUP",
                            style = MaterialTheme.typography.headlineMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = "Grant each permission below so GLOW FIRST can display the Trigger and perform taps.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                // 1. Display over other apps
                PermissionItemCard(
                    icon = Icons.Rounded.Layers,
                    title = "1. Display Over Other Apps",
                    description = "Required to float the small circular Trigger button over your game screen without blocking other touches.",
                    isGranted = permissionStatus.overlayGranted,
                    actionLabel = if (permissionStatus.overlayGranted) "Granted" else "Open Overlay Settings",
                    buttonTestTag = "grant_overlay_button",
                    onActionClick = {
                        runCatching {
                            context.startActivity(PermissionHelper.buildOverlaySettingsIntent(context))
                        }
                    }
                )

                // 2. Accessibility Service
                PermissionItemCard(
                    icon = Icons.Rounded.AccessibilityNew,
                    title = "2. Accessibility Service",
                    description = "Required by Android to dispatch real taps at your saved coordinates. Find \"GLOW FIRST Tap Engine\" in Accessibility Settings and enable it manually.",
                    isGranted = permissionStatus.accessibilityGranted,
                    actionLabel = if (permissionStatus.accessibilityGranted) "Granted" else "Open Accessibility",
                    buttonTestTag = "grant_accessibility_button",
                    onActionClick = {
                        runCatching {
                            context.startActivity(PermissionHelper.buildAccessibilitySettingsIntent())
                        }
                    }
                )

                // 3. Notifications (Android 13+ / Foreground Service)
                PermissionItemCard(
                    icon = Icons.Rounded.NotificationsActive,
                    title = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        "3. Notifications (Android 13+)"
                    } else {
                        "3. Foreground Service Notification"
                    },
                    description = "Keeps the active Trigger overlay service running reliably while gaming and gives you a one-tap Stop button in your notification shade.",
                    isGranted = permissionStatus.notificationGranted,
                    actionLabel = if (permissionStatus.notificationGranted) "Granted" else "Allow Notifications",
                    buttonTestTag = "grant_notification_button",
                    onActionClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !askedNotificationOnce) {
                            askedNotificationOnce = true
                            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            runCatching {
                                context.startActivity(PermissionHelper.buildNotificationSettingsIntent(context))
                            }
                        }
                    }
                )

                // 4. Battery optimization guidance
                PermissionItemCard(
                    icon = Icons.Rounded.BatterySaver,
                    title = "4. Battery Optimization (Recommended)",
                    description = "On Android 11 devices like Nokia C30, setting GLOW FIRST to Unrestricted / Don't Optimize prevents the OS from stopping the macro mid-match.",
                    isGranted = permissionStatus.batteryOptimizationIgnored,
                    positiveLabel = "Unrestricted",
                    negativeLabel = "Standard",
                    isWarningWhenNotGranted = true,
                    actionLabel = "Open Battery Settings",
                    buttonTestTag = "battery_settings_button",
                    onActionClick = {
                        runCatching {
                            context.startActivity(PermissionHelper.buildBatteryOptimizationSettingsIntent())
                        }
                    }
                )

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = onContinue,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("continue_permissions_button"),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GlowGreen,
                        contentColor = GlowBlack
                    )
                ) {
                    if (permissionStatus.allCoreGranted) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = if (permissionStatus.allCoreGranted) {
                            "CONTINUE TO GLOW FIRST"
                        } else {
                            "CONTINUE ANYWAY"
                        },
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun PermissionItemCard(
    icon: ImageVector,
    title: String,
    description: String,
    isGranted: Boolean,
    actionLabel: String,
    buttonTestTag: String,
    positiveLabel: String = "Granted",
    negativeLabel: String = "Not Granted",
    isWarningWhenNotGranted: Boolean = false,
    onActionClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = GlowSurfaceElevated,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(
            1.dp,
            if (isGranted) GlowGreen.copy(alpha = 0.35f) else GlowBorderSubtle
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isGranted) GlowGreen else TextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                StatusPill(
                    label = if (isGranted) positiveLabel else negativeLabel,
                    isPositive = isGranted,
                    isWarning = !isGranted && isWarningWhenNotGranted
                )
            }

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            OutlinedButton(
                onClick = onActionClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag(buttonTestTag),
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(
                    1.dp,
                    if (isGranted) GlowGreen.copy(alpha = 0.4f) else GlowBorderSubtle
                )
            ) {
                Icon(
                    imageVector = Icons.Rounded.OpenInNew,
                    contentDescription = null,
                    tint = if (isGranted) GlowGreen else TextPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isGranted) GlowGreen else TextPrimary
                )
            }
        }
    }
}
