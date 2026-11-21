package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.engine.SplineCurveLut
import com.example.model.CurveChannel
import com.example.model.CurvePoint
import com.example.model.FilterCatalog
import com.example.model.HistogramData
import com.example.model.ToneCurvesState
import com.example.ui.theme.CyanPrecision
import com.example.ui.theme.HistogramBlue
import com.example.ui.theme.HistogramGreen
import com.example.ui.theme.HistogramLuma
import com.example.ui.theme.HistogramRed
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.RoseMask
import com.example.ui.theme.StudioElevated
import com.example.ui.theme.StudioHairline
import com.example.ui.theme.StudioPanel
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.roundToInt

/**
 * Floating darkroom Tone Curve adjustment card displayed directly inside the editor view
 * so users can drag control points on the luminance / RGB curve while viewing the photo.
 */
@Composable
fun EditorToneCurvePanel(
    curvesState: ToneCurvesState,
    histogram: HistogramData,
    onSelectChannel: (CurveChannel) -> Unit,
    onUpdatePoints: (List<CurvePoint>, Boolean) -> Unit,
    onApplyPreset: (List<CurvePoint>) -> Unit,
    onResetChannel: () -> Unit,
    onBeginDrag: () -> Unit,
    onEndDrag: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeChannel = curvesState.activeChannel
    val points = curvesState.pointsFor(activeChannel)
    var selectedNodeIndex by remember(activeChannel) { mutableIntStateOf(-1) }
    val safeSelectedIndex = if (selectedNodeIndex in points.indices) selectedNodeIndex else -1
    val canDeleteSelected = safeSelectedIndex in 1 until points.lastIndex && points.size > 2

    Column(
        modifier = modifier
            .widthIn(max = 360.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(StudioPanel.copy(alpha = 0.94f))
            .border(1.dp, StudioHairline, RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .testTag("editor_tone_curve_panel"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Header row: Title + Channel pills + Delete/Reset/Close
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Tone Curve",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = TextPrimary
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                if (canDeleteSelected) {
                    IconButton(
                        onClick = {
                            val updated = points.toMutableList().apply { removeAt(safeSelectedIndex) }
                            selectedNodeIndex = -1
                            onUpdatePoints(updated, false)
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("editor_curve_delete_node_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "Remove selected curve point",
                            tint = RoseMask,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                IconButton(
                    onClick = {
                        selectedNodeIndex = -1
                        onResetChannel()
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("editor_curve_reset_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.RestartAlt,
                        contentDescription = "Reset curve channel",
                        tint = CyanPrecision,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("editor_curve_close_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close tone curve overlay",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Channel Selector Strip (Luminance RGB + R/G/B)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CurveChannel.entries.forEach { channel ->
                val selected = channel == activeChannel
                val dotColor = curveChannelColor(channel)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selected) StudioElevated else ObsidianVoid.copy(alpha = 0.65f))
                        .border(
                            1.dp,
                            if (selected) dotColor.copy(alpha = 0.75f) else StudioHairline,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable {
                            selectedNodeIndex = -1
                            onSelectChannel(channel)
                        }
                        .padding(horizontal = 9.dp, vertical = 5.dp)
                        .testTag("editor_curve_channel_${channel.name.lowercase()}"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(dotColor)
                    )
                    Text(
                        text = if (channel == CurveChannel.RGB) "Luma" else channel.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) TextPrimary else TextSecondary
                    )
                }
            }
        }

        // Visual Spline Curve Graph with Draggable Control Points
        ToneCurveVisualGraph(
            activeChannel = activeChannel,
            points = points,
            histogram = histogram,
            selectedNodeIndex = safeSelectedIndex,
            onSelectNode = { selectedNodeIndex = it },
            onUpdatePoints = onUpdatePoints,
            onBeginDrag = onBeginDrag,
            onEndDrag = onEndDrag,
            graphHeight = 148.dp
        )

        // Quick Tone Curve Presets + Interaction Tip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterCatalog.curvePresets.forEach { (label, presetPts) ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(7.dp))
                        .background(StudioElevated)
                        .border(1.dp, StudioHairline, RoundedCornerShape(7.dp))
                        .clickable {
                            selectedNodeIndex = -1
                            onApplyPreset(presetPts)
                        }
                        .padding(horizontal = 9.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextPrimary
                    )
                }
            }
            Spacer(modifier = Modifier.size(2.dp))
        }
    }
}

@Composable
fun ToneCurveVisualGraph(
    activeChannel: CurveChannel,
    points: List<CurvePoint>,
    histogram: HistogramData,
    selectedNodeIndex: Int,
    onSelectNode: (Int) -> Unit,
    onUpdatePoints: (List<CurvePoint>, Boolean) -> Unit,
    onBeginDrag: () -> Unit,
    onEndDrag: () -> Unit,
    graphHeight: Dp = 136.dp,
    modifier: Modifier = Modifier
) {
    val channelColor = curveChannelColor(activeChannel)
    val lut = remember(points) { SplineCurveLut.buildLut(points) }
    var draggingIndex by remember { mutableIntStateOf(-1) }
    val activeNodeIdx = if (draggingIndex in points.indices) draggingIndex else selectedNodeIndex
    val activePoint = points.getOrNull(activeNodeIdx)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(graphHeight + 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(ObsidianVoid)
            .border(1.dp, StudioHairline, RoundedCornerShape(12.dp))
            .padding(8.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("tone_curve_graph_canvas")
                .pointerInput(activeChannel, points) {
                    detectTapGestures(
                        onTap = { offset ->
                            val w = size.width.toFloat().coerceAtLeast(1f)
                            val h = size.height.toFloat().coerceAtLeast(1f)
                            val nx = (offset.x / w).coerceIn(0f, 1f)
                            val ny = (1f - offset.y / h).coerceIn(0f, 1f)
                            val nearExisting = points.indexOfFirst {
                                hypot(it.x - nx, it.y - ny) < 0.09f
                            }
                            if (nearExisting != -1) {
                                onSelectNode(nearExisting)
                            } else if (points.size < 9) {
                                val clampedX = nx.coerceIn(0.03f, 0.97f)
                                val updated = (points + CurvePoint(clampedX, ny)).sortedBy { it.x }
                                val addedIdx = updated.indexOfFirst { abs(it.x - clampedX) < 0.001f }
                                onSelectNode(addedIdx)
                                onUpdatePoints(updated, false)
                            }
                        },
                        onDoubleTap = { offset ->
                            val w = size.width.toFloat().coerceAtLeast(1f)
                            val h = size.height.toFloat().coerceAtLeast(1f)
                            val nx = (offset.x / w).coerceIn(0f, 1f)
                            val ny = (1f - offset.y / h).coerceIn(0f, 1f)
                            val idx = points.indexOfFirst {
                                hypot(it.x - nx, it.y - ny) < 0.10f
                            }
                            if (idx > 0 && idx < points.lastIndex && points.size > 2) {
                                val updated = points.toMutableList().apply { removeAt(idx) }
                                onSelectNode(-1)
                                onUpdatePoints(updated, false)
                            }
                        }
                    )
                }
                .pointerInput(activeChannel, points) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val w = size.width.toFloat().coerceAtLeast(1f)
                            val h = size.height.toFloat().coerceAtLeast(1f)
                            val nx = (offset.x / w).coerceIn(0f, 1f)
                            val ny = (1f - offset.y / h).coerceIn(0f, 1f)
                            var bestIdx = -1
                            var bestDist = 0.13f
                            points.forEachIndexed { idx, pt ->
                                val d = hypot(pt.x - nx, pt.y - ny)
                                if (d < bestDist) {
                                    bestDist = d
                                    bestIdx = idx
                                }
                            }
                            if (bestIdx == -1 && points.size < 9) {
                                val clampedX = nx.coerceIn(0.03f, 0.97f)
                                val inserted = (points + CurvePoint(clampedX, ny)).sortedBy { it.x }
                                bestIdx = inserted.indexOfFirst { abs(it.x - clampedX) < 0.001f }
                                onBeginDrag()
                                onUpdatePoints(inserted, true)
                            } else if (bestIdx != -1) {
                                onBeginDrag()
                            }
                            draggingIndex = bestIdx
                            onSelectNode(bestIdx)
                        },
                        onDragEnd = {
                            draggingIndex = -1
                            onEndDrag()
                        },
                        onDragCancel = {
                            draggingIndex = -1
                            onEndDrag()
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val idx = draggingIndex
                            if (idx !in points.indices) return@detectDragGestures
                            val w = size.width.toFloat().coerceAtLeast(1f)
                            val h = size.height.toFloat().coerceAtLeast(1f)
                            val minX = if (idx == 0) 0f else (points[idx - 1].x + 0.03f)
                            val maxX = if (idx == points.lastIndex) 1f else (points[idx + 1].x - 0.03f)
                            val nx = (change.position.x / w).coerceIn(minX, maxX)
                            val ny = (1f - change.position.y / h).coerceIn(0f, 1f)
                            val updated = points.toMutableList().apply {
                                set(idx, CurvePoint(nx, ny))
                            }
                            onUpdatePoints(updated, true)
                        }
                    )
                }
        ) {
            // 4x4 Grid (Blacks, Shadows, Midtones, Highlights, Whites)
            val stepX = size.width / 4f
            val stepY = size.height / 4f
            for (i in 0..4) {
                val x = i * stepX
                val y = i * stepY
                drawLine(StudioHairline.copy(alpha = 0.45f), Offset(x, 0f), Offset(x, size.height), 1f)
                drawLine(StudioHairline.copy(alpha = 0.45f), Offset(0f, y), Offset(size.width, y), 1f)
            }

            // Dashed 1:1 Linear Reference Line
            drawLine(
                color = StudioHairline.copy(alpha = 0.85f),
                start = Offset(0f, size.height),
                end = Offset(size.width, 0f),
                strokeWidth = 1.4f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
            )

            // Background Histogram Silhouette
            val histBins = when (activeChannel) {
                CurveChannel.RGB -> histogram.luma
                CurveChannel.RED -> histogram.red
                CurveChannel.GREEN -> histogram.green
                CurveChannel.BLUE -> histogram.blue
            }
            if (histBins.isNotEmpty()) {
                val hPath = Path()
                val hStep = size.width / (histBins.size - 1).coerceAtLeast(1)
                hPath.moveTo(0f, size.height)
                for (i in histBins.indices) {
                    val x = i * hStep
                    val y = size.height - histBins[i] * size.height * 0.72f
                    hPath.lineTo(x, y)
                }
                hPath.lineTo(size.width, size.height)
                hPath.close()
                drawPath(hPath, color = channelColor.copy(alpha = 0.16f))
            }

            // Smooth Spline Curve Path + subtle fill under curve
            val curvePath = Path()
            val fillPath = Path()
            fillPath.moveTo(0f, size.height)
            for (i in 0..255) {
                val px = (i / 255f) * size.width
                val py = (1f - lut[i] / 255f) * size.height
                if (i == 0) curvePath.moveTo(px, py) else curvePath.lineTo(px, py)
                fillPath.lineTo(px, py)
            }
            fillPath.lineTo(size.width, size.height)
            fillPath.close()

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        channelColor.copy(alpha = 0.18f),
                        Color.Transparent
                    )
                )
            )
            drawPath(
                path = curvePath,
                color = channelColor,
                style = Stroke(width = 3.4f, cap = StrokeCap.Round)
            )

            // Draggable Control Points
            points.forEachIndexed { idx, pt ->
                val cx = pt.x * size.width
                val cy = (1f - pt.y) * size.height
                val isActive = idx == activeNodeIdx
                if (isActive) {
                    drawCircle(
                        color = CyanPrecision.copy(alpha = 0.28f),
                        radius = 18f,
                        center = Offset(cx, cy)
                    )
                }
                drawCircle(
                    color = if (isActive) CyanPrecision else channelColor,
                    radius = if (isActive) 10.5f else 8.5f,
                    center = Offset(cx, cy)
                )
                drawCircle(
                    color = ObsidianVoid,
                    radius = 4.2f,
                    center = Offset(cx, cy)
                )
            }
        }

        // Live Luminance Mapping Coordinate Badge (IN -> OUT in 0..255)
        val badgeText = if (activePoint != null) {
            val inVal = (activePoint.x * 255f).roundToInt().coerceIn(0, 255)
            val outVal = (activePoint.y * 255f).roundToInt().coerceIn(0, 255)
            "IN $inVal → OUT $outVal"
        } else {
            "Tap to add • Drag node • 2×Tap removes"
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .clip(RoundedCornerShape(6.dp))
                .background(StudioPanel.copy(alpha = 0.82f))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = badgeText,
                style = MaterialTheme.typography.labelSmall,
                color = if (activePoint != null) CyanPrecision else TextMuted
            )
        }
    }
}

fun curveChannelColor(channel: CurveChannel): Color = when (channel) {
    CurveChannel.RGB -> HistogramLuma
    CurveChannel.RED -> HistogramRed
    CurveChannel.GREEN -> HistogramGreen
    CurveChannel.BLUE -> HistogramBlue
}
