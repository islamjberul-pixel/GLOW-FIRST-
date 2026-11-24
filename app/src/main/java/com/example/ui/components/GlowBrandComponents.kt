package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.GlowBorderSubtle
import com.example.ui.theme.GlowGreen
import com.example.ui.theme.GlowGreenContainer
import com.example.ui.theme.GlowSurfaceCard
import com.example.ui.theme.GlowSurfaceElevated
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusErrorContainer
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.StatusWarningContainer
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun GlowFirstLogoMark(
    modifier: Modifier = Modifier,
    size: Dp = 42.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(GlowGreenContainer)
            .border(1.dp, GlowGreen.copy(alpha = 0.55f), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size * 0.68f)) {
            val c = Offset(this.size.width / 2f, this.size.height / 2f)
            val rOuter = this.size.minDimension / 2f - 2f
            val rInner = rOuter * 0.56f
            drawCircle(
                color = GlowGreen,
                radius = rOuter,
                center = c,
                style = Stroke(width = 3.5f)
            )
            drawCircle(
                color = GlowGreen.copy(alpha = 0.7f),
                radius = rInner,
                center = c,
                style = Stroke(width = 2.2f)
            )
            drawCircle(
                color = GlowGreen,
                radius = rOuter * 0.22f,
                center = c
            )
            val tickLen = rOuter * 0.26f
            drawLine(GlowGreen, Offset(c.x, 0f), Offset(c.x, tickLen), strokeWidth = 3f)
            drawLine(GlowGreen, Offset(c.x, this.size.height - tickLen), Offset(c.x, this.size.height), strokeWidth = 3f)
            drawLine(GlowGreen, Offset(0f, c.y), Offset(tickLen, c.y), strokeWidth = 3f)
            drawLine(GlowGreen, Offset(this.size.width - tickLen, c.y), Offset(this.size.width, c.y), strokeWidth = 3f)
        }
    }
}

@Composable
fun StatusPill(
    label: String,
    isPositive: Boolean,
    isWarning: Boolean = false,
    modifier: Modifier = Modifier
) {
    val bgColor = when {
        isPositive -> GlowGreenContainer
        isWarning -> StatusWarningContainer
        else -> StatusErrorContainer
    }
    val fgColor = when {
        isPositive -> GlowGreen
        isWarning -> StatusWarning
        else -> StatusError
    }
    Surface(
        modifier = modifier,
        color = bgColor,
        shape = CircleShape,
        border = BorderStroke(1.dp, fgColor.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(fgColor)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = fgColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun FeedbackBannerCard(
    message: String,
    isError: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (isError) StatusErrorContainer else GlowGreenContainer
    val accent = if (isError) StatusError else GlowGreen
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = bg,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.55f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isError) Icons.Rounded.WarningAmber else Icons.Rounded.Info,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Dismiss message",
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun AppInfoDialog(
    isMacroConfigured: Boolean,
    onOpenPermissions: () -> Unit,
    onClearMacro: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = GlowSurfaceElevated,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GlowFirstLogoMark(size = 34.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("GLOW FIRST", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "v1.0 · Single-Profile Macro Engine",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    color = GlowSurfaceCard,
                    shape = MaterialTheme.shapes.small,
                    border = BorderStroke(1.dp, GlowBorderSubtle)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            "How Hold-to-Tap Works",
                            style = MaterialTheme.typography.titleSmall,
                            color = GlowGreen
                        )
                        Text(
                            "Press and hold the small floating Trigger circle while in your game. GLOW FIRST dispatches taps at your saved screenshot coordinates and stops immediately when you release your finger.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
                Surface(
                    color = GlowSurfaceCard,
                    shape = MaterialTheme.shapes.small,
                    border = BorderStroke(1.dp, GlowBorderSubtle)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            "Android Multi-Touch Note",
                            style = MaterialTheme.typography.titleSmall,
                            color = StatusWarning
                        )
                        Text(
                            "Touches outside the small Trigger circle pass directly to your game. However, Android AccessibilityService dispatchGesture can occasionally be interrupted by simultaneous physical multi-touch on some Android 11 devices.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onOpenPermissions()
                    },
                    modifier = Modifier.fillMaxWidth().testTag("info_open_permissions_button"),
                    border = BorderStroke(1.dp, GlowBorderSubtle)
                ) {
                    Icon(Icons.Rounded.Security, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Permission Setup Guide")
                }
                if (isMacroConfigured) {
                    Button(
                        onClick = {
                            onClearMacro()
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StatusErrorContainer,
                            contentColor = StatusError
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("info_clear_macro_button")
                    ) {
                        Icon(Icons.Rounded.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Clear Saved Macro")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = GlowGreen)
            }
        }
    )
}
