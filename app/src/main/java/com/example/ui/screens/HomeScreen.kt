package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.R
import com.example.model.MacroConfig
import com.example.model.MacroEngineState
import com.example.model.PermissionStatus
import com.example.ui.UiFeedbackBanner
import com.example.ui.components.FeedbackBannerCard
import com.example.ui.components.GlowFirstLogoMark
import com.example.ui.components.StatusPill
import com.example.ui.theme.GlowBlack
import com.example.ui.theme.GlowBorderSubtle
import com.example.ui.theme.GlowGreen
import com.example.ui.theme.GlowGreenContainer
import com.example.ui.theme.GlowGreenDeep
import com.example.ui.theme.GlowSurface
import com.example.ui.theme.GlowSurfaceCard
import com.example.ui.theme.GlowSurfaceElevated
import com.example.ui.theme.StatusError
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.TapCoordinateMapper
import java.io.File
import java.util.Locale

@Composable
fun HomeScreen(
    macroConfig: MacroConfig,
    permissionStatus: PermissionStatus,
    engineState: MacroEngineState,
    isImportingScreenshot: Boolean,
    banner: UiFeedbackBanner?,
    onAddMacroClick: () -> Unit,
    onEditKeymapClick: () -> Unit,
    onToggleOverlay: (Boolean) -> Unit,
    onOpenPermissions: () -> Unit,
    onOpenInfo: () -> Unit,
    onDismissBanner: () -> Unit
) {
    val activeError = banner?.message ?: engineState.lastErrorMessage
    val isBannerError = if (banner != null) banner.isError else engineState.lastErrorMessage != null

    Scaffold(
        containerColor = GlowBlack,
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(GlowSurface, GlowBlack, GlowBlack)
                    )
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
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Top Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlowFirstLogoMark(size = 44.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.headlineMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = stringResource(R.string.app_subtitle),
                            style = MaterialTheme.typography.labelSmall,
                            color = GlowGreen
                        )
                    }

                    val statusText = when {
                        macroConfig.overlayActive && permissionStatus.accessibilityGranted -> "ACTIVE"
                        permissionStatus.allCoreGranted -> "READY"
                        else -> "SETUP"
                    }
                    StatusPill(
                        label = statusText,
                        isPositive = permissionStatus.allCoreGranted,
                        isWarning = !permissionStatus.allCoreGranted &&
                            (permissionStatus.overlayGranted || permissionStatus.accessibilityGranted)
                    )

                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = onOpenInfo,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("home_settings_info_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = "App information and settings",
                            tint = TextSecondary
                        )
                    }
                }

                // Error / Feedback Banner
                if (!activeError.isNullOrBlank()) {
                    FeedbackBannerCard(
                        message = activeError,
                        isError = isBannerError,
                        onDismiss = onDismissBanner
                    )
                }

                // Primary Action: ADD MACRO
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = GlowSurfaceElevated,
                    shape = MaterialTheme.shapes.large,
                    border = BorderStroke(1.dp, GlowGreen.copy(alpha = 0.25f))
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = if (macroConfig.isConfigured) {
                                "Replace Screenshot & Keymap"
                            } else {
                                "Create Your Gaming Macro"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = "Pick a screenshot of your game to place the floating Trigger and automated Tap target.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Button(
                            onClick = onAddMacroClick,
                            enabled = !isImportingScreenshot,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .testTag("add_macro_button"),
                            shape = MaterialTheme.shapes.medium,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GlowGreen,
                                contentColor = GlowBlack
                            )
                        ) {
                            if (isImportingScreenshot) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.5.dp,
                                    color = GlowBlack
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "LOADING SCREENSHOT...",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.AddPhotoAlternate,
                                    contentDescription = null,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "ADD MACRO",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                }

                // Single-Profile Macro Status Card
                MacroStatusCard(
                    macroConfig = macroConfig,
                    engineState = engineState,
                    onEditKeymapClick = onEditKeymapClick,
                    onToggleOverlay = onToggleOverlay
                )

                // Permission Status Section
                PermissionSummaryCard(
                    permissionStatus = permissionStatus,
                    onOpenPermissions = onOpenPermissions
                )

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun MacroStatusCard(
    macroConfig: MacroConfig,
    engineState: MacroEngineState,
    onEditKeymapClick: () -> Unit,
    onToggleOverlay: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = GlowSurfaceElevated,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, GlowBorderSubtle)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.TouchApp,
                        contentDescription = null,
                        tint = if (macroConfig.isConfigured) GlowGreen else TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "MACRO PROFILE",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary
                    )
                }
                StatusPill(
                    label = if (macroConfig.isConfigured) "CONFIGURED" else "NOT SET",
                    isPositive = macroConfig.isConfigured,
                    isWarning = !macroConfig.isConfigured
                )
            }

            if (!macroConfig.isConfigured) {
                Text(
                    text = "No macro saved yet. Tap ADD MACRO above to select a game screenshot.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            } else {
                val intervalMs = TapCoordinateMapper.cpsToExactIntervalMs(macroConfig.cps)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .background(GlowSurfaceCard)
                        .border(1.dp, GlowBorderSubtle, MaterialTheme.shapes.medium)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val imgFile = File(macroConfig.screenshotPath)
                    if (imgFile.exists()) {
                        Image(
                            painter = rememberAsyncImagePainter(imgFile),
                            contentDescription = "Configured game screenshot",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(width = 72.dp, height = 50.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, GlowBorderSubtle, RoundedCornerShape(8.dp))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Speed,
                                contentDescription = null,
                                tint = GlowGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${macroConfig.cps} CPS (${String.format(Locale.US, "%.1f", intervalMs)} ms)",
                                style = MaterialTheme.typography.labelMedium,
                                color = GlowGreen
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (macroConfig.isLandscapeScreenshot) {
                                "Landscape keymap · ${macroConfig.screenshotWidth}×${macroConfig.screenshotHeight}"
                            } else {
                                "Portrait keymap · ${macroConfig.screenshotWidth}×${macroConfig.screenshotHeight}"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        if (engineState.sessionTapCount > 0L) {
                            Text(
                                text = "Session taps: ${engineState.sessionTapCount}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                        }
                    }
                }

                // Floating Trigger Overlay Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .background(
                            if (macroConfig.overlayActive) GlowGreenContainer else GlowSurfaceCard
                        )
                        .border(
                            1.dp,
                            if (macroConfig.overlayActive) GlowGreen.copy(alpha = 0.5f) else GlowBorderSubtle,
                            MaterialTheme.shapes.medium
                        )
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Layers,
                        contentDescription = null,
                        tint = if (macroConfig.overlayActive) GlowGreen else TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Floating Trigger",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary
                        )
                        Text(
                            text = if (macroConfig.overlayActive) {
                                "Visible over games · Hold circle to tap"
                            } else {
                                "Hidden · Enable to show Trigger"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    Switch(
                        checked = macroConfig.overlayActive,
                        onCheckedChange = onToggleOverlay,
                        modifier = Modifier.testTag("overlay_toggle_switch"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = GlowBlack,
                            checkedTrackColor = GlowGreen,
                            uncheckedThumbColor = TextSecondary,
                            uncheckedTrackColor = GlowSurface
                        )
                    )
                }

                OutlinedButton(
                    onClick = onEditKeymapClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("home_edit_keymap_button"),
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(1.dp, GlowGreenDeep.copy(alpha = 0.7f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GlowGreen)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("EDIT KEYMAP", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun PermissionSummaryCard(
    permissionStatus: PermissionStatus,
    onOpenPermissions: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = GlowSurfaceElevated,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, GlowBorderSubtle)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Shield,
                        contentDescription = null,
                        tint = if (permissionStatus.allCoreGranted) GlowGreen else Color(0xFFFFB300),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "PERMISSIONS",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary
                    )
                }
                Text(
                    text = if (permissionStatus.allCoreGranted) "ALL GRANTED" else "ACTION NEEDED",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (permissionStatus.allCoreGranted) GlowGreen else Color(0xFFFFB300),
                    fontWeight = FontWeight.Bold
                )
            }

            PermissionStatusLine(
                title = "Display over other apps",
                granted = permissionStatus.overlayGranted
            )
            PermissionStatusLine(
                title = "Accessibility tap service",
                granted = permissionStatus.accessibilityGranted
            )
            PermissionStatusLine(
                title = "Overlay notifications",
                granted = permissionStatus.notificationGranted
            )

            OutlinedButton(
                onClick = onOpenPermissions,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("manage_permissions_button"),
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(1.dp, GlowBorderSubtle)
            ) {
                Text(
                    text = if (permissionStatus.allCoreGranted) {
                        "VIEW PERMISSION GUIDE"
                    } else {
                        "SETUP PERMISSIONS"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = TextPrimary
                )
            }
        }
    }
}

@Composable
private fun PermissionStatusLine(
    title: String,
    granted: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (granted) Icons.Rounded.CheckCircle else Icons.Rounded.ErrorOutline,
                contentDescription = null,
                tint = if (granted) GlowGreen else StatusError,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
        Text(
            text = if (granted) "Granted" else "Not Granted",
            style = MaterialTheme.typography.labelSmall,
            color = if (granted) GlowGreen else StatusError,
            fontWeight = FontWeight.Bold
        )
    }
}
