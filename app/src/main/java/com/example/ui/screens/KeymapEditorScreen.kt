package com.example.ui.screens

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.model.MacroConfig
import com.example.model.MacroConstants
import com.example.ui.theme.GlowBlack
import com.example.ui.theme.GlowBorderSubtle
import com.example.ui.theme.GlowGreen
import com.example.ui.theme.GlowGreenContainer
import com.example.ui.theme.GlowSurfaceCard
import com.example.ui.theme.GlowSurfaceElevated
import com.example.ui.theme.StatusError
import com.example.ui.theme.TapAccent
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TriggerAccent
import com.example.util.CpsValidationResult
import com.example.util.TapCoordinateMapper
import java.io.File
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun KeymapEditorScreen(
    macroConfig: MacroConfig,
    onSave: (
        triggerNormX: Float,
        triggerNormY: Float,
        triggerSizeDp: Float,
        tapNormX: Float,
        tapNormY: Float,
        tapSizeDp: Float,
        cps: Int
    ) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val density = LocalDensity.current

    // Match the editor orientation to the game screenshot so landscape games fill the screen
    DisposableEffect(macroConfig.isLandscapeScreenshot) {
        val previous = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.requestedOrientation = if (macroConfig.isLandscapeScreenshot) {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        onDispose {
            activity?.requestedOrientation = previous
        }
    }

    BackHandler { onCancel() }

    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    var triggerNormX by remember(macroConfig) { mutableFloatStateOf(macroConfig.triggerNormX) }
    var triggerNormY by remember(macroConfig) { mutableFloatStateOf(macroConfig.triggerNormY) }
    var triggerSizeDp by remember(macroConfig) { mutableFloatStateOf(macroConfig.triggerSizeDp) }

    var tapNormX by remember(macroConfig) { mutableFloatStateOf(macroConfig.tapNormX) }
    var tapNormY by remember(macroConfig) { mutableFloatStateOf(macroConfig.tapNormY) }
    var tapSizeDp by remember(macroConfig) { mutableFloatStateOf(macroConfig.tapSizeDp) }

    var cps by remember(macroConfig) { mutableIntStateOf(macroConfig.cps) }
    var showSpeedDialog by remember { mutableStateOf(false) }

    val imageRect = remember(containerSize, macroConfig.screenshotWidth, macroConfig.screenshotHeight) {
        TapCoordinateMapper.computeFittedImageRect(
            imageWidth = macroConfig.screenshotWidth,
            imageHeight = macroConfig.screenshotHeight,
            containerWidth = containerSize.width.toFloat(),
            containerHeight = containerSize.height.toFloat()
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GlowBlack)
            .onSizeChanged { containerSize = it }
    ) {
        val file = File(macroConfig.screenshotPath)
        if (file.exists()) {
            Image(
                painter = rememberAsyncImagePainter(file),
                contentDescription = "Game screenshot editor background",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Subtle dark scrim so components pop cleanly over bright game screenshots
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.22f))
        )

        if (containerSize.width > 0 && containerSize.height > 0) {
            val triggerSizePx = with(density) { triggerSizeDp.dp.toPx() }
            val tapSizePx = with(density) { tapSizeDp.dp.toPx() }

            val triggerCenterX = (triggerNormX * containerSize.width)
                .coerceIn(triggerSizePx / 2f, containerSize.width - triggerSizePx / 2f)
            val triggerCenterY = (triggerNormY * containerSize.height)
                .coerceIn(triggerSizePx / 2f, containerSize.height - triggerSizePx / 2f)

            val tapScreenPoint = TapCoordinateMapper.normalizedScreenshotToScreenPoint(
                normX = tapNormX,
                normY = tapNormY,
                imageRect = imageRect
            )

            // COMPONENT 1: TRIGGER AREA (Draggable & Resizable)
            EditableTriggerNode(
                centerX = triggerCenterX,
                centerY = triggerCenterY,
                sizeDp = triggerSizeDp,
                sizePx = triggerSizePx,
                onDragBy = { dx, dy ->
                    val nextX = (triggerCenterX + dx).coerceIn(
                        triggerSizePx / 2f,
                        containerSize.width - triggerSizePx / 2f
                    )
                    val nextY = (triggerCenterY + dy).coerceIn(
                        triggerSizePx / 2f,
                        containerSize.height - triggerSizePx / 2f
                    )
                    triggerNormX = (nextX / containerSize.width.coerceAtLeast(1)).coerceIn(0f, 1f)
                    triggerNormY = (nextY / containerSize.height.coerceAtLeast(1)).coerceIn(0f, 1f)
                },
                onResizeByPx = { deltaPx ->
                    val deltaDp = with(density) { deltaPx.toDp().value }
                    triggerSizeDp = (triggerSizeDp + deltaDp).coerceIn(
                        MacroConstants.MIN_TRIGGER_SIZE_DP,
                        MacroConstants.MAX_TRIGGER_SIZE_DP
                    )
                }
            )

            // COMPONENT 2: TAP BUTTON (Draggable, Resizable, with Corner Settings Icon)
            EditableTapNode(
                centerX = tapScreenPoint.x,
                centerY = tapScreenPoint.y,
                sizeDp = tapSizeDp,
                sizePx = tapSizePx,
                cps = cps,
                onDragBy = { dx, dy ->
                    val nextScreenX = (tapScreenPoint.x + dx).coerceIn(imageRect.left, imageRect.right)
                    val nextScreenY = (tapScreenPoint.y + dy).coerceIn(imageRect.top, imageRect.bottom)
                    val norm = TapCoordinateMapper.screenPointToNormalizedScreenshot(
                        screenX = nextScreenX,
                        screenY = nextScreenY,
                        imageRect = imageRect
                    )
                    tapNormX = norm.x
                    tapNormY = norm.y
                },
                onResizeByPx = { deltaPx ->
                    val deltaDp = with(density) { deltaPx.toDp().value }
                    tapSizeDp = (tapSizeDp + deltaDp).coerceIn(
                        MacroConstants.MIN_TAP_SIZE_DP,
                        MacroConstants.MAX_TAP_SIZE_DP
                    )
                },
                onOpenSpeedSettings = { showSpeedDialog = true }
            )
        }

        // Compact Top Control Bar (Cancel + CPS indicator + Prominent SAVE)
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                color = GlowBlack.copy(alpha = 0.82f),
                shape = CircleShape,
                border = BorderStroke(1.dp, GlowBorderSubtle)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(end = 12.dp)
                ) {
                    IconButton(
                        onClick = onCancel,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("editor_cancel_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Cancel editing",
                            tint = TextPrimary
                        )
                    }
                    Text(
                        text = "EDIT KEYMAP",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextPrimary
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = GlowBlack.copy(alpha = 0.82f),
                    shape = CircleShape,
                    border = BorderStroke(1.dp, TapAccent.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { showSpeedDialog = true }
                ) {
                    Text(
                        text = "$cps CPS",
                        style = MaterialTheme.typography.labelMedium,
                        color = TapAccent,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }

                Button(
                    onClick = {
                        onSave(
                            triggerNormX,
                            triggerNormY,
                            triggerSizeDp,
                            tapNormX,
                            tapNormY,
                            tapSizeDp,
                            cps
                        )
                    },
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("save_keymap_button"),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GlowGreen,
                        contentColor = GlowBlack
                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("SAVE", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }

    if (showSpeedDialog) {
        TapSpeedDialog(
            initialCps = cps,
            onApply = { newCps ->
                cps = newCps
                showSpeedDialog = false
            },
            onDismiss = { showSpeedDialog = false }
        )
    }
}

@Composable
private fun EditableTriggerNode(
    centerX: Float,
    centerY: Float,
    sizeDp: Float,
    sizePx: Float,
    onDragBy: (Float, Float) -> Unit,
    onResizeByPx: (Float) -> Unit
) {
    val latestOnDrag by rememberUpdatedState(onDragBy)
    val latestOnResize by rememberUpdatedState(onResizeByPx)
    val handleExtraPx = with(LocalDensity.current) { 14.dp.toPx() }
    val boxSizeDp = sizeDp.dp + 28.dp

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    x = (centerX - sizePx / 2f - handleExtraPx).roundToInt(),
                    y = (centerY - sizePx / 2f - handleExtraPx).roundToInt()
                )
            }
            .size(boxSizeDp)
            .testTag("editor_trigger_component")
    ) {
        // Main circular Trigger body
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(sizeDp.dp)
                .clip(CircleShape)
                .background(TriggerAccent.copy(alpha = 0.24f))
                .border(2.5.dp, TriggerAccent, CircleShape)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        latestOnDrag(dragAmount.x, dragAmount.y)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "TRIGGER",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${sizeDp.roundToInt()}dp",
                    style = MaterialTheme.typography.labelSmall,
                    color = TriggerAccent
                )
            }
        }

        // Resize handle on bottom-right rim
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(32.dp)
                .clip(CircleShape)
                .background(GlowBlack)
                .border(1.5.dp, TriggerAccent, CircleShape)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        latestOnResize((dragAmount.x + dragAmount.y) * 0.7f)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.OpenInFull,
                contentDescription = "Resize Trigger",
                tint = TriggerAccent,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun EditableTapNode(
    centerX: Float,
    centerY: Float,
    sizeDp: Float,
    sizePx: Float,
    cps: Int,
    onDragBy: (Float, Float) -> Unit,
    onResizeByPx: (Float) -> Unit,
    onOpenSpeedSettings: () -> Unit
) {
    val latestOnDrag by rememberUpdatedState(onDragBy)
    val latestOnResize by rememberUpdatedState(onResizeByPx)
    val handleExtraPx = with(LocalDensity.current) { 14.dp.toPx() }
    val boxSizeDp = sizeDp.dp + 28.dp

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    x = (centerX - sizePx / 2f - handleExtraPx).roundToInt(),
                    y = (centerY - sizePx / 2f - handleExtraPx).roundToInt()
                )
            }
            .size(boxSizeDp)
            .testTag("editor_tap_component")
    ) {
        // Main circular Tap target body
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(sizeDp.dp)
                .clip(CircleShape)
                .background(TapAccent.copy(alpha = 0.24f))
                .border(2.5.dp, TapAccent, CircleShape)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        latestOnDrag(dragAmount.x, dragAmount.y)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "TAP",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${cps}cps",
                    style = MaterialTheme.typography.labelSmall,
                    color = TapAccent
                )
            }
        }

        // Small Settings icon on top-right corner of TAP component
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(34.dp)
                .clip(CircleShape)
                .background(GlowBlack)
                .border(1.5.dp, TapAccent, CircleShape)
                .clickable(onClick = onOpenSpeedSettings)
                .testTag("tap_speed_settings_button"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Settings,
                contentDescription = "Tap Speed Settings",
                tint = TapAccent,
                modifier = Modifier.size(18.dp)
            )
        }

        // Resize handle on bottom-right corner of TAP component
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(32.dp)
                .clip(CircleShape)
                .background(GlowBlack)
                .border(1.5.dp, TapAccent, CircleShape)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        latestOnResize((dragAmount.x + dragAmount.y) * 0.7f)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.OpenInFull,
                contentDescription = "Resize Tap Target",
                tint = TapAccent,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TapSpeedDialog(
    initialCps: Int,
    onApply: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var inputText by remember { mutableStateOf(initialCps.toString()) }
    val validation = TapCoordinateMapper.validateCps(inputText)
    val validCps = (validation as? CpsValidationResult.Valid)?.cps

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = GlowSurfaceElevated,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = {
            Text(
                text = "TAP SPEED",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "CLICKS PER SECOND (CPS)",
                    style = MaterialTheme.typography.labelMedium,
                    color = GlowGreen
                )

                OutlinedTextField(
                    value = inputText,
                    onValueChange = { raw ->
                        if (raw.length <= 3 && raw.all { it.isDigit() }) {
                            inputText = raw
                        }
                    },
                    singleLine = true,
                    isError = validation is CpsValidationResult.Invalid,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    label = { Text("Custom CPS (1–30)") },
                    supportingText = {
                        when (validation) {
                            is CpsValidationResult.Valid -> {
                                val ms = TapCoordinateMapper.cpsToExactIntervalMs(validation.cps)
                                Text(
                                    text = "Interval: ~${String.format(Locale.US, "%.1f", ms)} ms per tap",
                                    color = GlowGreen
                                )
                            }
                            is CpsValidationResult.Invalid -> {
                                Text(text = validation.reason, color = StatusError)
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GlowGreen,
                        unfocusedBorderColor = GlowBorderSubtle,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = GlowGreen
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("cps_numeric_input")
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    MacroConstants.CPS_PRESETS.forEach { preset ->
                        val selected = validCps == preset
                        FilterChip(
                            selected = selected,
                            onClick = { inputText = preset.toString() },
                            label = {
                                Text(
                                    text = "$preset CPS",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GlowGreenContainer,
                                selectedLabelColor = GlowGreen,
                                containerColor = GlowSurfaceCard,
                                labelColor = TextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selected,
                                borderColor = GlowBorderSubtle,
                                selectedBorderColor = GlowGreen
                            ),
                            modifier = Modifier.testTag("cps_preset_$preset")
                        )
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        confirmButton = {
            Button(
                onClick = { if (validCps != null) onApply(validCps) },
                enabled = validCps != null,
                colors = ButtonDefaults.buttonColors(
                    containerColor = GlowGreen,
                    contentColor = GlowBlack
                ),
                modifier = Modifier.testTag("apply_cps_button")
            ) {
                Text("APPLY", style = MaterialTheme.typography.labelLarge)
            }
        }
    )
}
