package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Compare
import androidx.compose.material.icons.rounded.Exposure
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.engine.ExposureColorMatrix
import com.example.model.AiToolMode
import com.example.model.BackgroundReplacement
import com.example.model.BrushPoint
import com.example.model.BrushStroke
import com.example.model.CurveChannel
import com.example.model.CurvePoint
import com.example.model.EditorToolTab
import com.example.model.HistogramData
import com.example.model.ImageMetadataInfo
import com.example.model.MaskLayer
import com.example.model.MaskType
import com.example.model.NormalizedRect
import com.example.ui.theme.AmberWarm
import com.example.ui.theme.CyanPrecision
import com.example.ui.theme.HistogramBlue
import com.example.ui.theme.HistogramGreen
import com.example.ui.theme.HistogramLuma
import com.example.ui.theme.HistogramRed
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.RoseMask
import com.example.ui.theme.StudioHairline
import com.example.ui.theme.StudioPanel
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.AiOperationUiState
import com.example.ui.viewmodel.EditorScreenUiState
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.roundToInt

private enum class CropDragHandle {
    NONE, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, BODY
}

private enum class MaskDragTarget {
    NONE, CENTER, EDGE, START, END
}

@Composable
fun CanvasStage(
    uiState: EditorScreenUiState,
    onCropRectChange: (NormalizedRect) -> Unit,
    onBeginContinuousEdit: () -> Unit,
    onEndContinuousEdit: () -> Unit,
    onUpdateSelectedMask: ((MaskLayer) -> MaskLayer) -> Unit,
    onMaskBrushPoint: (BrushPoint, Boolean) -> Unit,
    onMoveBlendLayer: (Float, Float) -> Unit,
    onAiEraserPoint: (BrushPoint, Boolean) -> Unit,
    onCompareChange: (Boolean) -> Unit,
    onExposureChange: (Float) -> Unit,
    onExposureChangeFinished: () -> Unit,
    onResetExposure: () -> Unit,
    onSelectCurveChannel: (CurveChannel) -> Unit,
    onUpdateCurvePoints: (List<CurvePoint>, Boolean) -> Unit,
    onApplyCurvePreset: (List<CurvePoint>) -> Unit,
    onResetCurveChannel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeBitmap = if (uiState.isComparing) {
        uiState.originalPreview ?: uiState.processedPreview
    } else {
        uiState.processedPreview
    }

    var zoomScale by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var showToneCurveOverlay by remember { mutableStateOf(false) }

    // Reset zoom when switching to Crop, Masking, Blend dragging, AI Eraser, or Editor Tone Curve overlay
    val allowZoomPan = !showToneCurveOverlay &&
        uiState.activeTab != EditorToolTab.CROP &&
        !(uiState.activeTab == EditorToolTab.MASKING && uiState.snapshot.selectedMaskId != null) &&
        !(uiState.activeTab == EditorToolTab.BLEND && uiState.snapshot.selectedBlendLayerId != null) &&
        !(uiState.activeTab == EditorToolTab.AI_STUDIO && uiState.aiState.activeMode == AiToolMode.OBJECT_ERASER)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .background(ObsidianVoid)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        val containerW = constraints.maxWidth.toFloat()
        val containerH = constraints.maxHeight.toFloat()

        if (activeBitmap != null) {
            val bmpW = activeBitmap.width.toFloat().coerceAtLeast(1f)
            val bmpH = activeBitmap.height.toFloat().coerceAtLeast(1f)
            val fitScale = min(containerW / bmpW, containerH / bmpH)
            val drawnW = bmpW * fitScale
            val drawnH = bmpH * fitScale
            val drawnLeft = (containerW - drawnW) / 2f
            val drawnTop = (containerH - drawnH) / 2f
            val imageBounds = Rect(drawnLeft, drawnTop, drawnLeft + drawnW, drawnTop + drawnH)

            val showCheckerboard = uiState.aiState.hasBackgroundRemoved &&
                uiState.aiState.backgroundStyle == BackgroundReplacement.TRANSPARENT

            val density = LocalDensity.current
            val drawnWdp = with(density) { drawnW.toDp() }
            val drawnHdp = with(density) { drawnH.toDp() }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (allowZoomPan) {
                            Modifier
                                .pointerInput(Unit) {
                                    detectTransformGestures { _, pan, zoom, _ ->
                                        val nextScale = (zoomScale * zoom).coerceIn(1f, 4f)
                                        zoomScale = nextScale
                                        panOffset = if (nextScale <= 1.02f) {
                                            Offset.Zero
                                        } else {
                                            Offset(
                                                (panOffset.x + pan.x).coerceIn(-drawnW * 0.5f, drawnW * 0.5f),
                                                (panOffset.y + pan.y).coerceIn(-drawnH * 0.5f, drawnH * 0.5f)
                                            )
                                        }
                                    }
                                }
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onDoubleTap = {
                                            zoomScale = 1f
                                            panOffset = Offset.Zero
                                        }
                                    )
                                }
                        } else {
                            Modifier
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(drawnWdp, drawnHdp)
                        .graphicsLayer {
                            if (allowZoomPan) {
                                scaleX = zoomScale
                                scaleY = zoomScale
                                translationX = panOffset.x
                                translationY = panOffset.y
                            }
                        }
                ) {
                    // Checkerboard behind transparent cutouts
                    if (showCheckerboard) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawCheckerboard()
                        }
                    }

                    val activeExposureEv = if (uiState.isComparing) 0f else uiState.snapshot.adjustments.exposure
                    val exposureColorFilter = remember(activeExposureEv) {
                        ColorFilter.colorMatrix(ExposureColorMatrix.createComposeMatrix(activeExposureEv))
                    }

                    Image(
                        bitmap = activeBitmap.asImageBitmap(),
                        contentDescription = "Edited photograph preview",
                        contentScale = ContentScale.FillBounds,
                        colorFilter = exposureColorFilter,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Interactive Overlays matching exact image bounds
                    when (uiState.activeTab) {
                        EditorToolTab.CROP -> {
                            CropInteractiveOverlay(
                                cropRect = uiState.snapshot.crop.cropRect,
                                onCropChange = onCropRectChange,
                                onBeginDrag = onBeginContinuousEdit,
                                onEndDrag = onEndContinuousEdit
                            )
                        }
                        EditorToolTab.MASKING -> {
                            val selectedMask = uiState.snapshot.masks.find {
                                it.id == uiState.snapshot.selectedMaskId
                            }
                            if (selectedMask != null) {
                                MaskInteractiveOverlay(
                                    mask = selectedMask,
                                    onBeginEdit = onBeginContinuousEdit,
                                    onEndEdit = onEndContinuousEdit,
                                    onUpdateMask = onUpdateSelectedMask,
                                    onBrushPoint = onMaskBrushPoint
                                )
                            }
                        }
                        EditorToolTab.BLEND -> {
                            val selectedBlend = uiState.snapshot.blendLayers.find {
                                it.id == uiState.snapshot.selectedBlendLayerId
                            }
                            if (selectedBlend != null) {
                                BlendLayerDragOverlay(
                                    posX = selectedBlend.posX,
                                    posY = selectedBlend.posY,
                                    onBeginDrag = onBeginContinuousEdit,
                                    onDragDelta = onMoveBlendLayer,
                                    onEndDrag = onEndContinuousEdit
                                )
                            }
                        }
                        EditorToolTab.AI_STUDIO -> {
                            if (uiState.aiState.activeMode == AiToolMode.OBJECT_ERASER) {
                                AiEraserOverlay(
                                    strokes = uiState.aiState.eraserStrokes,
                                    brushSize = uiState.aiState.eraserBrushSize,
                                    onBrushPoint = onAiEraserPoint
                                )
                            }
                        }
                        else -> Unit
                    }
                }
            }

            // Unused variable suppression / clarity
            imageBounds.width
        } else {
            // Branded empty workspace shown before a photo is opened.
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.photostudio_icon),
                    contentDescription = "PhotoStudio",
                    modifier = Modifier.size(112.dp),
                    contentScale = ContentScale.Fit
                )
                Text(
                    text = "PhotoStudio",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
                Text(
                    text = "Your photos, your studio.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Text(
                    text = "Open a photo to start creating",
                    style = MaterialTheme.typography.labelLarge,
                    color = CyanPrecision
                )
            }
        }

        // Top-Left: Metadata Badge
        MetadataPill(
            metadata = uiState.metadata,
            isComparing = uiState.isComparing,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(4.dp)
        )

        // Top-Right: Live RGB + Luminance Histogram Overlay
        AnimatedVisibility(
            visible = uiState.showHistogram,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
        ) {
            HistogramOverlayCard(histogram = uiState.histogram)
        }

        // Floating Editor Tone Curve Graph Overlay + Bottom HUD Row
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedVisibility(
                visible = showToneCurveOverlay,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it / 3 }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 3 })
            ) {
                EditorToneCurvePanel(
                    curvesState = uiState.snapshot.curves,
                    histogram = uiState.histogram,
                    onSelectChannel = onSelectCurveChannel,
                    onUpdatePoints = onUpdateCurvePoints,
                    onApplyPreset = onApplyCurvePreset,
                    onResetChannel = onResetCurveChannel,
                    onBeginDrag = onBeginContinuousEdit,
                    onEndDrag = onEndContinuousEdit,
                    onClose = { showToneCurveOverlay = false }
                )
            }

            val isCurveModified = !uiState.snapshot.curves.isLinear
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (uiState.isComparing) AmberWarm else StudioPanel.copy(alpha = 0.86f)
                        )
                        .border(
                            1.dp,
                            if (uiState.isComparing) AmberWarm else StudioHairline,
                            RoundedCornerShape(10.dp)
                        )
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    onCompareChange(true)
                                    tryAwaitRelease()
                                    onCompareChange(false)
                                }
                            )
                        }
                        .padding(horizontal = 9.dp, vertical = 9.dp)
                        .testTag("compare_before_after_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Compare,
                            contentDescription = "Hold to compare original",
                            tint = if (uiState.isComparing) ObsidianVoid else TextPrimary,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = if (uiState.isComparing) "ORIG" else "Compare",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (uiState.isComparing) ObsidianVoid else TextPrimary
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            when {
                                showToneCurveOverlay -> CyanPrecision.copy(alpha = 0.22f)
                                isCurveModified -> CyanPrecision.copy(alpha = 0.14f)
                                else -> StudioPanel.copy(alpha = 0.86f)
                            }
                        )
                        .border(
                            1.dp,
                            if (showToneCurveOverlay || isCurveModified) CyanPrecision else StudioHairline,
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { showToneCurveOverlay = !showToneCurveOverlay }
                        .padding(horizontal = 9.dp, vertical = 9.dp)
                        .testTag("toggle_editor_tone_curve_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ShowChart,
                            contentDescription = "Toggle custom tone curve graph",
                            tint = if (showToneCurveOverlay || isCurveModified) CyanPrecision else TextPrimary,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "Curve",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (showToneCurveOverlay || isCurveModified) CyanPrecision else TextPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f, fill = false))

                val curExposure = uiState.snapshot.adjustments.exposure
                val isExposureEdited = kotlin.math.abs(curExposure) > 0.01f
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .widthIn(max = 240.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(StudioPanel.copy(alpha = 0.88f))
                        .border(
                            1.dp,
                            if (isExposureEdited) CyanPrecision.copy(alpha = 0.6f) else StudioHairline,
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .testTag("editor_exposure_control"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Exposure,
                        contentDescription = "Reset exposure",
                        tint = if (isExposureEdited) CyanPrecision else TextSecondary,
                        modifier = Modifier
                            .size(17.dp)
                            .clickable(onClick = onResetExposure)
                    )
                    Slider(
                        value = curExposure,
                        onValueChange = onExposureChange,
                        onValueChangeFinished = onExposureChangeFinished,
                        valueRange = -2.0f..2.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = TextPrimary,
                            activeTrackColor = CyanPrecision,
                            inactiveTrackColor = StudioHairline
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(30.dp)
                            .testTag("editor_exposure_slider")
                    )
                    Text(
                        text = String.format("%+.2f", curExposure),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isExposureEdited) CyanPrecision else TextSecondary,
                        modifier = Modifier.clickable(onClick = onResetExposure)
                    )
                }
            }
        }

        // Active AI Loading Overlay
        if (uiState.aiOperationState is AiOperationUiState.Loading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ObsidianVoid.copy(alpha = 0.72f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(StudioPanel)
                        .border(1.dp, CyanPrecision.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(
                        color = CyanPrecision,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(36.dp)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = AmberWarm,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = uiState.aiOperationState.statusText,
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary
                        )
                    }
                    Text(
                        text = "High-precision neural rendering may take 15–45s",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun MetadataPill(
    metadata: ImageMetadataInfo,
    isComparing: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(StudioPanel.copy(alpha = 0.78f))
            .border(1.dp, StudioHairline.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = if (isComparing) "BEFORE" else "${metadata.width}×${metadata.height}",
            style = MaterialTheme.typography.labelSmall,
            color = if (isComparing) AmberWarm else TextPrimary
        )
        Text(
            text = "• ${metadata.megapixels}",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )
    }
}

@Composable
fun HistogramOverlayCard(
    histogram: HistogramData,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(124.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(StudioPanel.copy(alpha = 0.82f))
            .border(1.dp, StudioHairline.copy(alpha = 0.8f), RoundedCornerShape(10.dp))
            .padding(6.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
        ) {
            drawHistogramCurve(histogram.red, HistogramRed.copy(alpha = 0.45f), HistogramRed)
            drawHistogramCurve(histogram.green, HistogramGreen.copy(alpha = 0.40f), HistogramGreen)
            drawHistogramCurve(histogram.blue, HistogramBlue.copy(alpha = 0.45f), HistogramBlue)
            drawHistogramCurve(histogram.luma, HistogramLuma.copy(alpha = 0.28f), HistogramLuma.copy(alpha = 0.85f))
        }
    }
}

private fun DrawScope.drawHistogramCurve(
    bins: FloatArray,
    fillColor: Color,
    strokeColor: Color
) {
    if (bins.isEmpty()) return
    val fillPath = Path()
    val linePath = Path()
    val stepX = size.width / (bins.size - 1).coerceAtLeast(1)
    fillPath.moveTo(0f, size.height)
    for (i in bins.indices) {
        val x = i * stepX
        val y = size.height - (bins[i].coerceIn(0f, 1f) * size.height * 0.92f)
        fillPath.lineTo(x, y)
        if (i == 0) linePath.moveTo(x, y) else linePath.lineTo(x, y)
    }
    fillPath.lineTo(size.width, size.height)
    fillPath.close()
    drawPath(fillPath, color = fillColor)
    drawPath(linePath, color = strokeColor, style = Stroke(width = 1.4f))
}

private fun DrawScope.drawCheckerboard() {
    val cell = 18f
    val cols = (size.width / cell).toInt() + 1
    val rows = (size.height / cell).toInt() + 1
    val light = Color(0xFF2B303B)
    val dark = Color(0xFF1C2029)
    for (r in 0..rows) {
        for (c in 0..cols) {
            val isEven = (r + c) % 2 == 0
            drawRect(
                color = if (isEven) light else dark,
                topLeft = Offset(c * cell, r * cell),
                size = Size(cell, cell)
            )
        }
    }
}

@Composable
private fun CropInteractiveOverlay(
    cropRect: NormalizedRect,
    onCropChange: (NormalizedRect) -> Unit,
    onBeginDrag: () -> Unit,
    onEndDrag: () -> Unit
) {
    var activeHandle by remember { mutableStateOf(CropDragHandle.NONE) }
    var workingRect by remember(cropRect) { mutableStateOf(cropRect) }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(cropRect) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val w = size.width.toFloat().coerceAtLeast(1f)
                        val h = size.height.toFloat().coerceAtLeast(1f)
                        val nx = offset.x / w
                        val ny = offset.y / h
                        val threshold = 0.08f
                        activeHandle = when {
                            hypot(nx - cropRect.left, ny - cropRect.top) < threshold -> CropDragHandle.TOP_LEFT
                            hypot(nx - cropRect.right, ny - cropRect.top) < threshold -> CropDragHandle.TOP_RIGHT
                            hypot(nx - cropRect.left, ny - cropRect.bottom) < threshold -> CropDragHandle.BOTTOM_LEFT
                            hypot(nx - cropRect.right, ny - cropRect.bottom) < threshold -> CropDragHandle.BOTTOM_RIGHT
                            nx in cropRect.left..cropRect.right && ny in cropRect.top..cropRect.bottom -> CropDragHandle.BODY
                            else -> CropDragHandle.NONE
                        }
                        if (activeHandle != CropDragHandle.NONE) onBeginDrag()
                    },
                    onDragEnd = {
                        activeHandle = CropDragHandle.NONE
                        onEndDrag()
                    },
                    onDragCancel = {
                        activeHandle = CropDragHandle.NONE
                        onEndDrag()
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val w = size.width.toFloat().coerceAtLeast(1f)
                        val h = size.height.toFloat().coerceAtLeast(1f)
                        val dx = dragAmount.x / w
                        val dy = dragAmount.y / h
                        val minSize = 0.14f

                        val next = when (activeHandle) {
                            CropDragHandle.TOP_LEFT -> workingRect.copy(
                                left = (workingRect.left + dx).coerceIn(0f, workingRect.right - minSize),
                                top = (workingRect.top + dy).coerceIn(0f, workingRect.bottom - minSize)
                            )
                            CropDragHandle.TOP_RIGHT -> workingRect.copy(
                                right = (workingRect.right + dx).coerceIn(workingRect.left + minSize, 1f),
                                top = (workingRect.top + dy).coerceIn(0f, workingRect.bottom - minSize)
                            )
                            CropDragHandle.BOTTOM_LEFT -> workingRect.copy(
                                left = (workingRect.left + dx).coerceIn(0f, workingRect.right - minSize),
                                bottom = (workingRect.bottom + dy).coerceIn(workingRect.top + minSize, 1f)
                            )
                            CropDragHandle.BOTTOM_RIGHT -> workingRect.copy(
                                right = (workingRect.right + dx).coerceIn(workingRect.left + minSize, 1f),
                                bottom = (workingRect.bottom + dy).coerceIn(workingRect.top + minSize, 1f)
                            )
                            CropDragHandle.BODY -> {
                                val rw = workingRect.width
                                val rh = workingRect.height
                                val nl = (workingRect.left + dx).coerceIn(0f, 1f - rw)
                                val nt = (workingRect.top + dy).coerceIn(0f, 1f - rh)
                                NormalizedRect(nl, nt, nl + rw, nt + rh)
                            }
                            CropDragHandle.NONE -> workingRect
                        }
                        workingRect = next
                        onCropChange(next)
                    }
                )
            }
    ) {
        val l = workingRect.left * size.width
        val t = workingRect.top * size.height
        val r = workingRect.right * size.width
        val b = workingRect.bottom * size.height
        val cw = r - l
        val ch = b - t

        // Dim outside crop area
        val scrim = Color.Black.copy(alpha = 0.58f)
        drawRect(scrim, Offset.Zero, Size(size.width, t))
        drawRect(scrim, Offset(0f, b), Size(size.width, size.height - b))
        drawRect(scrim, Offset(0f, t), Size(l, ch))
        drawRect(scrim, Offset(r, t), Size(size.width - r, ch))

        // Crop border
        drawRect(
            color = Color.White,
            topLeft = Offset(l, t),
            size = Size(cw, ch),
            style = Stroke(width = 2.5f)
        )

        // Rule of Thirds Grid
        val thirdW = cw / 3f
        val thirdH = ch / 3f
        val gridColor = Color.White.copy(alpha = 0.35f)
        for (i in 1..2) {
            drawLine(gridColor, Offset(l + thirdW * i, t), Offset(l + thirdW * i, b), strokeWidth = 1.2f)
            drawLine(gridColor, Offset(l, t + thirdH * i), Offset(r, t + thirdH * i), strokeWidth = 1.2f)
        }

        // Cyan precision corner brackets
        val arm = 22f
        val strokeW = 6f
        // Top-left
        drawLine(CyanPrecision, Offset(l, t), Offset(l + arm, t), strokeW, StrokeCap.Round)
        drawLine(CyanPrecision, Offset(l, t), Offset(l, t + arm), strokeW, StrokeCap.Round)
        // Top-right
        drawLine(CyanPrecision, Offset(r, t), Offset(r - arm, t), strokeW, StrokeCap.Round)
        drawLine(CyanPrecision, Offset(r, t), Offset(r, t + arm), strokeW, StrokeCap.Round)
        // Bottom-left
        drawLine(CyanPrecision, Offset(l, b), Offset(l + arm, b), strokeW, StrokeCap.Round)
        drawLine(CyanPrecision, Offset(l, b), Offset(l, b - arm), strokeW, StrokeCap.Round)
        // Bottom-right
        drawLine(CyanPrecision, Offset(r, b), Offset(r - arm, b), strokeW, StrokeCap.Round)
        drawLine(CyanPrecision, Offset(r, b), Offset(r, b - arm), strokeW, StrokeCap.Round)
    }
}

@Composable
private fun MaskInteractiveOverlay(
    mask: MaskLayer,
    onBeginEdit: () -> Unit,
    onEndEdit: () -> Unit,
    onUpdateMask: ((MaskLayer) -> MaskLayer) -> Unit,
    onBrushPoint: (BrushPoint, Boolean) -> Unit
) {
    var dragTarget by remember { mutableStateOf(MaskDragTarget.NONE) }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(mask.id, mask.type) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val w = size.width.toFloat().coerceAtLeast(1f)
                        val h = size.height.toFloat().coerceAtLeast(1f)
                        val nx = (offset.x / w).coerceIn(0f, 1f)
                        val ny = (offset.y / h).coerceIn(0f, 1f)
                        when (mask.type) {
                            MaskType.BRUSH -> {
                                onBrushPoint(BrushPoint(nx, ny), true)
                            }
                            MaskType.RADIAL -> {
                                val distCenter = hypot(nx - mask.centerX, ny - mask.centerY)
                                val edgeX = (mask.centerX + mask.radiusX).coerceIn(0f, 1f)
                                val distEdge = hypot(nx - edgeX, ny - mask.centerY)
                                dragTarget = when {
                                    distEdge < 0.09f -> MaskDragTarget.EDGE
                                    distCenter < 0.14f -> MaskDragTarget.CENTER
                                    else -> MaskDragTarget.CENTER
                                }
                                onBeginEdit()
                            }
                            MaskType.LINEAR -> {
                                val dStart = hypot(nx - mask.startX, ny - mask.startY)
                                val dEnd = hypot(nx - mask.endX, ny - mask.endY)
                                dragTarget = if (dStart <= dEnd) MaskDragTarget.START else MaskDragTarget.END
                                onBeginEdit()
                            }
                        }
                    },
                    onDragEnd = {
                        dragTarget = MaskDragTarget.NONE
                        onEndEdit()
                    },
                    onDragCancel = {
                        dragTarget = MaskDragTarget.NONE
                        onEndEdit()
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val w = size.width.toFloat().coerceAtLeast(1f)
                        val h = size.height.toFloat().coerceAtLeast(1f)
                        val nx = (change.position.x / w).coerceIn(0f, 1f)
                        val ny = (change.position.y / h).coerceIn(0f, 1f)
                        val dx = dragAmount.x / w
                        val dy = dragAmount.y / h

                        when (mask.type) {
                            MaskType.BRUSH -> {
                                onBrushPoint(BrushPoint(nx, ny), false)
                            }
                            MaskType.RADIAL -> {
                                onUpdateMask { cur ->
                                    if (dragTarget == MaskDragTarget.EDGE) {
                                        cur.copy(
                                            radiusX = hypot(nx - cur.centerX, ny - cur.centerY)
                                                .coerceIn(0.08f, 0.85f),
                                            radiusY = (cur.radiusY + dy * 0.5f).coerceIn(0.08f, 0.85f)
                                        )
                                    } else {
                                        cur.copy(
                                            centerX = (cur.centerX + dx).coerceIn(0.05f, 0.95f),
                                            centerY = (cur.centerY + dy).coerceIn(0.05f, 0.95f)
                                        )
                                    }
                                }
                            }
                            MaskType.LINEAR -> {
                                onUpdateMask { cur ->
                                    if (dragTarget == MaskDragTarget.START) {
                                        cur.copy(startX = nx, startY = ny)
                                    } else {
                                        cur.copy(endX = nx, endY = ny)
                                    }
                                }
                            }
                        }
                    }
                )
            }
    ) {
        val dash = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
        when (mask.type) {
            MaskType.RADIAL -> {
                val cx = mask.centerX * size.width
                val cy = mask.centerY * size.height
                val rx = mask.radiusX * size.width
                val ry = mask.radiusY * size.height
                drawOval(
                    color = CyanPrecision,
                    topLeft = Offset(cx - rx, cy - ry),
                    size = Size(rx * 2f, ry * 2f),
                    style = Stroke(width = 2.5f, pathEffect = dash)
                )
                // Center handle
                drawCircle(CyanPrecision, radius = 11f, center = Offset(cx, cy))
                drawCircle(ObsidianVoid, radius = 5f, center = Offset(cx, cy))
                // Radius handle
                val edgeHandle = Offset((cx + rx).coerceIn(0f, size.width), cy)
                drawCircle(AmberWarm, radius = 10f, center = edgeHandle)
            }
            MaskType.LINEAR -> {
                val p1 = Offset(mask.startX * size.width, mask.startY * size.height)
                val p2 = Offset(mask.endX * size.width, mask.endY * size.height)
                drawLine(
                    color = CyanPrecision,
                    start = p1,
                    end = p2,
                    strokeWidth = 2.5f,
                    pathEffect = dash
                )
                drawCircle(CyanPrecision, radius = 11f, center = p1)
                drawCircle(ObsidianVoid, radius = 5f, center = p1)
                drawCircle(AmberWarm, radius = 11f, center = p2)
                drawCircle(ObsidianVoid, radius = 5f, center = p2)
            }
            MaskType.BRUSH -> Unit // Rubylith overlay already shows exact painted pixels!
        }
    }
}

@Composable
private fun AiEraserOverlay(
    strokes: List<BrushStroke>,
    brushSize: Float,
    onBrushPoint: (BrushPoint, Boolean) -> Unit
) {
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(brushSize) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val nx = (offset.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f)
                        val ny = (offset.y / size.height.coerceAtLeast(1)).coerceIn(0f, 1f)
                        onBrushPoint(BrushPoint(nx, ny), true)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val nx = (change.position.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f)
                        val ny = (change.position.y / size.height.coerceAtLeast(1)).coerceIn(0f, 1f)
                        onBrushPoint(BrushPoint(nx, ny), false)
                    }
                )
            }
    ) {
        val minDim = min(size.width, size.height)
        for (stroke in strokes) {
            val rPx = (stroke.radius * minDim).coerceAtLeast(8f)
            for (pt in stroke.points) {
                drawCircle(
                    color = RoseMask.copy(alpha = 0.45f),
                    radius = rPx,
                    center = Offset(pt.x * size.width, pt.y * size.height)
                )
            }
        }
    }
}

@Composable
private fun BlendLayerDragOverlay(
    posX: Float,
    posY: Float,
    onBeginDrag: () -> Unit,
    onDragDelta: (Float, Float) -> Unit,
    onEndDrag: () -> Unit
) {
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { onBeginDrag() },
                    onDragEnd = onEndDrag,
                    onDragCancel = onEndDrag,
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val w = size.width.toFloat().coerceAtLeast(1f)
                        val h = size.height.toFloat().coerceAtLeast(1f)
                        onDragDelta(dragAmount.x / w, dragAmount.y / h)
                    }
                )
            }
    ) {
        val cx = posX * size.width
        val cy = posY * size.height
        val arm = 18f
        drawCircle(
            color = CyanPrecision.copy(alpha = 0.7f),
            radius = 18f,
            center = Offset(cx, cy),
            style = Stroke(width = 2.2f)
        )
        drawLine(CyanPrecision, Offset(cx - arm, cy), Offset(cx + arm, cy), strokeWidth = 1.6f)
        drawLine(CyanPrecision, Offset(cx, cy - arm), Offset(cx, cy + arm), strokeWidth = 1.6f)
    }
}
