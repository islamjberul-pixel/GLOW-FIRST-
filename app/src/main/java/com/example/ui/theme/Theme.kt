package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val GlowFirstDarkColorScheme = darkColorScheme(
    primary = GlowGreen,
    onPrimary = GlowBlack,
    primaryContainer = GlowGreenContainer,
    onPrimaryContainer = GlowGreenBright,
    secondary = TapAccent,
    onSecondary = GlowBlack,
    secondaryContainer = GlowSurfaceCard,
    onSecondaryContainer = TextPrimary,
    tertiary = GlowGreenBright,
    onTertiary = GlowBlack,
    background = GlowBlack,
    onBackground = TextPrimary,
    surface = GlowSurface,
    onSurface = TextPrimary,
    surfaceVariant = GlowSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLowest = GlowBlack,
    surfaceContainerLow = GlowSurface,
    surfaceContainer = GlowSurfaceElevated,
    surfaceContainerHigh = GlowSurfaceCard,
    outline = GlowBorderSubtle,
    outlineVariant = GlowBorderSubtle,
    error = StatusError,
    onError = GlowBlack,
    errorContainer = StatusErrorContainer,
    onErrorContainer = StatusError
)

private val GlowFirstShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun GlowFirstTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = GlowFirstDarkColorScheme,
        typography = GlowFirstTypography,
        shapes = GlowFirstShapes,
        content = content
    )
}
