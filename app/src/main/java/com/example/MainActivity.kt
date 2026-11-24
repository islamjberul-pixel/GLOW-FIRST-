package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.MainViewModel
import com.example.ui.components.AppInfoDialog
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.KeymapEditorScreen
import com.example.ui.screens.PermissionSetupScreen
import com.example.ui.screens.ScreenshotPreviewScreen
import com.example.ui.theme.GlowFirstTheme

private object Routes {
    const val HOME = "home"
    const val PERMISSIONS = "permissions"
    const val PREVIEW = "preview"
    const val EDITOR = "editor"
}

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GlowFirstTheme {
                GlowFirstAppRoot(viewModel = viewModel)
            }
        }
    }
}

@Composable
private fun GlowFirstAppRoot(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val lifecycleOwner = LocalLifecycleOwner.current

    val macroConfig by viewModel.macroConfig.collectAsStateWithLifecycle()
    val permissionStatus by viewModel.permissionStatus.collectAsStateWithLifecycle()
    val engineState by viewModel.engineState.collectAsStateWithLifecycle()
    val isImporting by viewModel.isImportingScreenshot.collectAsStateWithLifecycle()
    val feedbackBanner by viewModel.feedbackBanner.collectAsStateWithLifecycle()

    var showInfoDialog by remember { mutableStateOf(false) }
    var checkedFirstLaunch by remember { mutableStateOf(false) }

    // Automatically re-check permissions whenever returning from Android Settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // First-launch permission setup gate
    LaunchedEffect(macroConfig.hasCompletedPermissionSetup, permissionStatus.allCoreGranted) {
        if (!checkedFirstLaunch) {
            checkedFirstLaunch = true
            if (!macroConfig.hasCompletedPermissionSetup && !permissionStatus.allCoreGranted) {
                navController.navigate(Routes.PERMISSIONS) {
                    launchSingleTop = true
                }
            }
        }
    }

    // Zero-permission Android Photo Picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.importScreenshot(uri) {
                navController.navigate(Routes.PREVIEW) {
                    launchSingleTop = true
                }
            }
        }
    }

    val launchPhotoPicker = {
        photoPickerLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    NavHost(
        navController = navController,
        startDestination = Routes.HOME
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                macroConfig = macroConfig,
                permissionStatus = permissionStatus,
                engineState = engineState,
                isImportingScreenshot = isImporting,
                banner = feedbackBanner,
                onAddMacroClick = launchPhotoPicker,
                onEditKeymapClick = {
                    if (macroConfig.isConfigured && macroConfig.screenshotPath.isNotBlank()) {
                        navController.navigate(Routes.EDITOR) {
                            launchSingleTop = true
                        }
                    } else {
                        viewModel.showError("No saved macro found. Select a screenshot first.")
                    }
                },
                onToggleOverlay = { enabled ->
                    viewModel.setOverlayEnabled(enabled)
                },
                onOpenPermissions = {
                    navController.navigate(Routes.PERMISSIONS) {
                        launchSingleTop = true
                    }
                },
                onOpenInfo = { showInfoDialog = true },
                onDismissBanner = { viewModel.dismissBanner() }
            )
        }

        composable(Routes.PERMISSIONS) {
            PermissionSetupScreen(
                permissionStatus = permissionStatus,
                isFirstLaunchOnboarding = !macroConfig.hasCompletedPermissionSetup,
                onRefreshPermissions = { viewModel.refreshPermissions() },
                onContinue = {
                    viewModel.markPermissionSetupCompleted()
                    if (!navController.popBackStack()) {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.HOME) { inclusive = true }
                        }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.PREVIEW) {
            ScreenshotPreviewScreen(
                macroConfig = macroConfig,
                onEditKeymap = {
                    navController.navigate(Routes.EDITOR) {
                        launchSingleTop = true
                    }
                },
                onChooseAnother = launchPhotoPicker,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.EDITOR) {
            KeymapEditorScreen(
                macroConfig = macroConfig,
                onSave = { tX, tY, tSize, tapX, tapY, tapSize, cps ->
                    viewModel.saveKeymapConfiguration(
                        triggerNormX = tX,
                        triggerNormY = tY,
                        triggerSizeDp = tSize,
                        tapNormX = tapX,
                        tapNormY = tapY,
                        tapSizeDp = tapSize,
                        cps = cps,
                        onSaved = {
                            navController.popBackStack(Routes.HOME, inclusive = false)
                        }
                    )
                },
                onCancel = { navController.popBackStack() }
            )
        }
    }

    if (showInfoDialog) {
        AppInfoDialog(
            isMacroConfigured = macroConfig.isConfigured,
            onOpenPermissions = {
                navController.navigate(Routes.PERMISSIONS) {
                    launchSingleTop = true
                }
            },
            onClearMacro = { viewModel.clearSavedMacro() },
            onDismiss = { showInfoDialog = false }
        )
    }
}
