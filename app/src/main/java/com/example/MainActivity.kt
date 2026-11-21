package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ads.AdMobCoordinator
import com.example.ads.findActivity
import com.example.ai.configureAppCheck
import com.example.model.BackgroundReplacement
import com.example.ui.components.CameraViewfinderScreen
import com.example.ui.components.CanvasStage
import com.example.ui.components.EditorBottomDock
import com.example.ui.components.ExportModalSheet
import com.example.ui.components.PhotoSourceModalSheet
import com.example.ui.components.RemoveAdsModalSheet
import com.example.ui.components.StudioTopBar
import com.example.ui.theme.PhotoStudioTheme
import com.example.ui.theme.ObsidianVoid
import com.example.ui.viewmodel.AiOperationUiState
import com.example.ui.viewmodel.ExportUiState
import com.example.ui.viewmodel.PhotoEditorViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure Firebase App Check with launch intent before UI or Firebase operations
        configureAppCheck(this, intent)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )

        setContent {
            PhotoStudioTheme {
                PhotoStudioScreen()
            }
        }
    }
}

@Composable
fun PhotoStudioScreen(
    viewModel: PhotoEditorViewModel = viewModel()
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val luminaApp = remember(context) { context.applicationContext as? LuminaApplication }
    val billingStateFlow = luminaApp?.premiumBillingManager?.state
    val billingState by if (billingStateFlow != null) {
        billingStateFlow.collectAsStateWithLifecycle()
    } else {
        remember { mutableStateOf(com.example.billing.PremiumBillingState()) }
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showPhotoSourceSheet by remember { mutableStateOf(false) }
    var showExportSheet by remember { mutableStateOf(false) }
    var showCameraViewfinder by remember { mutableStateOf(false) }
    var showRemoveAdsSheet by remember { mutableStateOf(false) }
    var replaceSelectedGalleryShape by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.exportUiState, billingState.isPremiumAdFree) {
        if (!billingState.isPremiumAdFree && uiState.exportUiState is ExportUiState.Success && activity != null) {
            luminaApp?.showInterstitialIfAvailable(activity)
        }
    }

    LaunchedEffect(uiState.aiOperationState, billingState.isPremiumAdFree) {
        if (!billingState.isPremiumAdFree && uiState.aiOperationState is AiOperationUiState.Success && activity != null) {
            luminaApp?.showInterstitialIfAvailable(activity)
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                viewModel.loadPhotoFromUri(uri)
            }
        }
    )

    val galleryShapePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                viewModel.addGalleryShapeBlendLayer(
                    uri = uri,
                    replaceSelected = replaceSelectedGalleryShape
                )
            }
            replaceSelectedGalleryShape = false
        }
    )

    val launchGalleryShapePicker: (Boolean) -> Unit = { replaceSelected ->
        replaceSelectedGalleryShape = replaceSelected
        AdMobCoordinator.suppressNextAppOpen()
        galleryShapePickerLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    if (showCameraViewfinder) {
        CameraViewfinderScreen(
            onPhotoCaptured = { uri ->
                showCameraViewfinder = false
                viewModel.loadPhotoFromUri(uri, title = "Camera Capture")
            },
            onClose = { showCameraViewfinder = false }
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianVoid)
    ) {
        StudioTopBar(
            canUndo = uiState.canUndo,
            canRedo = uiState.canRedo,
            showHistogram = uiState.showHistogram,
            isPremiumAdFree = billingState.isPremiumAdFree,
            onOpenPhotoClick = { showPhotoSourceSheet = true },
            onCameraClick = { showCameraViewfinder = true },
            onUndoClick = viewModel::undo,
            onRedoClick = viewModel::redo,
            onToggleHistogram = viewModel::toggleHistogram,
            onResetAll = viewModel::resetAllEdits,
            onRemoveAdsClick = {
                luminaApp?.premiumBillingManager?.clearMessages()
                showRemoveAdsSheet = true
            },
            onExportClick = {
                viewModel.resetExportState()
                showExportSheet = true
            }
        )

        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            val isWideScreen = maxWidth >= 840.dp
            if (isWideScreen) {
                // Adaptive Expanded Layout: Canvas + Side Inspector Pane
                Row(modifier = Modifier.fillMaxSize()) {
                    CanvasStage(
                        uiState = uiState,
                        onCropRectChange = { viewModel.updateCropRect(it, continuous = true) },
                        onBeginContinuousEdit = viewModel::beginContinuousEdit,
                        onEndContinuousEdit = viewModel::endContinuousEdit,
                        onUpdateSelectedMask = { transform ->
                            viewModel.updateSelectedMask(continuous = true, transform = transform)
                        },
                        onMaskBrushPoint = { pt, isNew ->
                            viewModel.addBrushPointToSelectedMask(pt, isNew)
                        },
                        onMoveBlendLayer = viewModel::moveSelectedBlendLayer,
                        onAiEraserPoint = viewModel::addAiEraserPoint,
                        onCompareChange = viewModel::setComparing,
                        onExposureChange = { ev ->
                            viewModel.updateAdjustments(continuous = true) { it.copy(exposure = ev) }
                        },
                        onExposureChangeFinished = viewModel::endContinuousEdit,
                        onResetExposure = {
                            viewModel.updateAdjustments(continuous = false) { it.copy(exposure = 0f) }
                        },
                        onSelectCurveChannel = viewModel::selectCurveChannel,
                        onUpdateCurvePoints = viewModel::updateActiveCurvePoints,
                        onApplyCurvePreset = viewModel::applyCurvePreset,
                        onResetCurveChannel = viewModel::resetActiveCurveChannel,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                    EditorBottomDock(
                        uiState = uiState,
                        viewModel = viewModel,
                        onImportShapeFromGallery = launchGalleryShapePicker,
                        modifier = Modifier
                            .width(380.dp)
                            .fillMaxHeight()
                    )
                }
            } else {
                // Standard Handheld Darkroom Layout: Full-bleed Canvas + Precision Bottom Dock
                Column(modifier = Modifier.fillMaxSize()) {
                    CanvasStage(
                        uiState = uiState,
                        onCropRectChange = { viewModel.updateCropRect(it, continuous = true) },
                        onBeginContinuousEdit = viewModel::beginContinuousEdit,
                        onEndContinuousEdit = viewModel::endContinuousEdit,
                        onUpdateSelectedMask = { transform ->
                            viewModel.updateSelectedMask(continuous = true, transform = transform)
                        },
                        onMaskBrushPoint = { pt, isNew ->
                            viewModel.addBrushPointToSelectedMask(pt, isNew)
                        },
                        onMoveBlendLayer = viewModel::moveSelectedBlendLayer,
                        onAiEraserPoint = viewModel::addAiEraserPoint,
                        onCompareChange = viewModel::setComparing,
                        onExposureChange = { ev ->
                            viewModel.updateAdjustments(continuous = true) { it.copy(exposure = ev) }
                        },
                        onExposureChangeFinished = viewModel::endContinuousEdit,
                        onResetExposure = {
                            viewModel.updateAdjustments(continuous = false) { it.copy(exposure = 0f) }
                        },
                        onSelectCurveChannel = viewModel::selectCurveChannel,
                        onUpdateCurvePoints = viewModel::updateActiveCurvePoints,
                        onApplyCurvePreset = viewModel::applyCurvePreset,
                        onResetCurveChannel = viewModel::resetActiveCurveChannel,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    )
                    EditorBottomDock(
                        uiState = uiState,
                        viewModel = viewModel,
                        onImportShapeFromGallery = launchGalleryShapePicker
                    )
                }
            }
        }
    }

    if (showPhotoSourceSheet) {
        PhotoSourceModalSheet(
            onPickFromGallery = {
                AdMobCoordinator.suppressNextAppOpen()
                photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onCaptureCamera = { showCameraViewfinder = true },
            onSelectSample = { resId, title ->
                viewModel.loadSampleDrawable(resId, title)
            },
            onDismiss = { showPhotoSourceSheet = false }
        )
    }

    if (showExportSheet) {
        ExportModalSheet(
            metadata = uiState.metadata,
            hasTransparentAlpha = uiState.aiState.hasBackgroundRemoved &&
                uiState.aiState.backgroundStyle == BackgroundReplacement.TRANSPARENT,
            isPremiumAdFree = billingState.isPremiumAdFree,
            exportUiState = uiState.exportUiState,
            onExport = viewModel::exportHighRes,
            onRemoveAdsClick = {
                luminaApp?.premiumBillingManager?.clearMessages()
                showRemoveAdsSheet = true
            },
            onDismiss = { showExportSheet = false }
        )
    }

    if (showRemoveAdsSheet) {
        RemoveAdsModalSheet(
            billingState = billingState,
            onPurchaseClick = {
                if (activity != null) {
                    luminaApp?.premiumBillingManager?.launchRemoveAdsPurchase(activity)
                }
            },
            onRestoreClick = {
                luminaApp?.premiumBillingManager?.restorePurchases()
            },
            onDismiss = { showRemoveAdsSheet = false }
        )
    }
}
