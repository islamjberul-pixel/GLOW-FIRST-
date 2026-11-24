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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Tune
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.model.MacroConfig
import com.example.ui.components.StatusPill
import com.example.ui.theme.GlowBlack
import com.example.ui.theme.GlowBorderSubtle
import com.example.ui.theme.GlowGreen
import com.example.ui.theme.GlowSurfaceElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.io.File

@Composable
fun ScreenshotPreviewScreen(
    macroConfig: MacroConfig,
    onEditKeymap: () -> Unit,
    onChooseAnother: () -> Unit,
    onBack: () -> Unit
) {
    val file = File(macroConfig.screenshotPath)
    val aspect = if (macroConfig.screenshotWidth > 0 && macroConfig.screenshotHeight > 0) {
        macroConfig.screenshotWidth.toFloat() / macroConfig.screenshotHeight.toFloat()
    } else {
        16f / 9f
    }

    Scaffold(
        containerColor = GlowBlack,
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .widthIn(max = 600.dp)
                    .align(Alignment.TopCenter),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("preview_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back to Home",
                            tint = TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "GAME SCREENSHOT",
                            style = MaterialTheme.typography.headlineSmall,
                            color = TextPrimary
                        )
                        Text(
                            text = "Coordinate reference loaded",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    StatusPill(
                        label = if (macroConfig.isLandscapeScreenshot) "LANDSCAPE" else "PORTRAIT",
                        isPositive = true
                    )
                }

                // Aspect-preserved Screenshot Container
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    color = GlowSurfaceElevated,
                    shape = MaterialTheme.shapes.large,
                    border = BorderStroke(1.dp, GlowBorderSubtle)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (file.exists()) {
                            Image(
                                painter = rememberAsyncImagePainter(file),
                                contentDescription = "Selected game screenshot preview",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(aspect, matchHeightConstraintsFirst = aspect < 1f)
                                    .clip(MaterialTheme.shapes.medium)
                                    .border(
                                        1.dp,
                                        GlowGreen.copy(alpha = 0.35f),
                                        MaterialTheme.shapes.medium
                                    )
                            )
                        } else {
                            Text(
                                text = "Screenshot missing. Please select another screenshot.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                }

                if (macroConfig.screenshotWidth > 0 && macroConfig.screenshotHeight > 0) {
                    Text(
                        text = "Resolution: ${macroConfig.screenshotWidth} × ${macroConfig.screenshotHeight} px · Original aspect ratio preserved",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }

                // Actions
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onEditKeymap,
                        enabled = file.exists(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("preview_edit_keymap_button"),
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GlowGreen,
                            contentColor = GlowBlack
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Tune,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("EDIT KEYMAP", style = MaterialTheme.typography.labelLarge)
                    }

                    OutlinedButton(
                        onClick = onChooseAnother,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("preview_choose_another_button"),
                        shape = MaterialTheme.shapes.medium,
                        border = BorderStroke(1.dp, GlowBorderSubtle)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AddPhotoAlternate,
                            contentDescription = null,
                            tint = TextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Choose Another Screenshot", color = TextPrimary)
                    }
                }
            }
        }
    }
}
