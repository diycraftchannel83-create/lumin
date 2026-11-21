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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
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
import com.example.ui.theme.StudioElevated
import com.example.ui.theme.StudioHairline
import com.example.ui.theme.StudioPanel
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.abs
import kotlin.math.hypot

@Composable
fun ToneCurveEditor(
    curvesState: ToneCurvesState,
    histogram: HistogramData,
    onSelectChannel: (CurveChannel) -> Unit,
    onUpdatePoints: (List<CurvePoint>, Boolean) -> Unit,
    onApplyPreset: (List<CurvePoint>) -> Unit,
    onResetChannel: () -> Unit,
    onBeginDrag: () -> Unit,
    onEndDrag: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeChannel = curvesState.activeChannel
    val points = curvesState.pointsFor(activeChannel)
    val channelColor = when (activeChannel) {
        CurveChannel.RGB -> HistogramLuma
        CurveChannel.RED -> HistogramRed
        CurveChannel.GREEN -> HistogramGreen
        CurveChannel.BLUE -> HistogramBlue
    }
    val lut = remember(points) { SplineCurveLut.buildLut(points) }

    var draggingIndex by remember { mutableIntStateOf(-1) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Channel selector + Reset row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CurveChannel.entries.forEach { channel ->
                    val selected = channel == activeChannel
                    val dotColor = when (channel) {
                        CurveChannel.RGB -> HistogramLuma
                        CurveChannel.RED -> HistogramRed
                        CurveChannel.GREEN -> HistogramGreen
                        CurveChannel.BLUE -> HistogramBlue
                    }
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selected) StudioElevated else StudioPanel)
                            .border(
                                1.dp,
                                if (selected) dotColor.copy(alpha = 0.7f) else StudioHairline,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { onSelectChannel(channel) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("curve_channel_${channel.name.lowercase()}"),
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
                            text = channel.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selected) TextPrimary else TextSecondary
                        )
                    }
                }
            }

            Text(
                text = "Reset",
                style = MaterialTheme.typography.labelMedium,
                color = CyanPrecision,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onResetChannel)
                    .padding(horizontal = 8.dp, vertical = 5.dp)
                    .testTag("reset_curve_button")
            )
        }

        // Interactive Curve Graph Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(148.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(ObsidianVoid)
                .border(1.dp, StudioHairline, RoundedCornerShape(12.dp))
                .padding(8.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(132.dp)
                    .pointerInput(activeChannel, points) {
                        detectTapGestures(
                            onTap = { offset ->
                                val w = size.width.toFloat().coerceAtLeast(1f)
                                val h = size.height.toFloat().coerceAtLeast(1f)
                                val nx = (offset.x / w).coerceIn(0.02f, 0.98f)
                                val ny = (1f - offset.y / h).coerceIn(0f, 1f)
                                val nearExisting = points.indexOfFirst {
                                    hypot(it.x - nx, it.y - ny) < 0.08f
                                }
                                if (nearExisting == -1 && points.size < 9) {
                                    val updated = (points + CurvePoint(nx, ny)).sortedBy { it.x }
                                    onUpdatePoints(updated, false)
                                }
                            },
                            onDoubleTap = { offset ->
                                val w = size.width.toFloat().coerceAtLeast(1f)
                                val h = size.height.toFloat().coerceAtLeast(1f)
                                val nx = (offset.x / w).coerceIn(0f, 1f)
                                val ny = (1f - offset.y / h).coerceIn(0f, 1f)
                                val idx = points.indexOfFirst {
                                    hypot(it.x - nx, it.y - ny) < 0.09f
                                }
                                if (idx > 0 && idx < points.lastIndex && points.size > 2) {
                                    val updated = points.toMutableList().apply { removeAt(idx) }
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
                                var bestDist = 0.12f
                                points.forEachIndexed { idx, pt ->
                                    val d = hypot(pt.x - nx, pt.y - ny)
                                    if (d < bestDist) {
                                        bestDist = d
                                        bestIdx = idx
                                    }
                                }
                                if (bestIdx == -1 && points.size < 9) {
                                    val inserted = (points + CurvePoint(nx, ny)).sortedBy { it.x }
                                    bestIdx = inserted.indexOfFirst { abs(it.x - nx) < 0.001f }
                                    onBeginDrag()
                                    onUpdatePoints(inserted, true)
                                } else if (bestIdx != -1) {
                                    onBeginDrag()
                                }
                                draggingIndex = bestIdx
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
                // 4x4 Grid
                val stepX = size.width / 4f
                val stepY = size.height / 4f
                for (i in 0..4) {
                    val x = i * stepX
                    val y = i * stepY
                    drawLine(StudioHairline.copy(alpha = 0.5f), Offset(x, 0f), Offset(x, size.height), 1f)
                    drawLine(StudioHairline.copy(alpha = 0.5f), Offset(0f, y), Offset(size.width, y), 1f)
                }

                // Diagonal reference line
                drawLine(
                    color = StudioHairline,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.2f
                )

                // Background Histogram silhouette
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

                // Smooth Spline Curve Path
                val curvePath = Path()
                for (i in 0..255) {
                    val px = (i / 255f) * size.width
                    val py = (1f - lut[i] / 255f) * size.height
                    if (i == 0) curvePath.moveTo(px, py) else curvePath.lineTo(px, py)
                }
                drawPath(curvePath, color = channelColor, style = Stroke(width = 3.2f))

                // Control Nodes
                points.forEachIndexed { idx, pt ->
                    val cx = pt.x * size.width
                    val cy = (1f - pt.y) * size.height
                    val isDragging = idx == draggingIndex
                    drawCircle(
                        color = if (isDragging) CyanPrecision else channelColor,
                        radius = if (isDragging) 10f else 8f,
                        center = Offset(cx, cy)
                    )
                    drawCircle(
                        color = ObsidianVoid,
                        radius = 4f,
                        center = Offset(cx, cy)
                    )
                }
            }
        }

        // Quick Curve Presets Strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterCatalog.curvePresets.forEach { (label, presetPts) ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(StudioElevated)
                        .border(1.dp, StudioHairline, RoundedCornerShape(8.dp))
                        .clickable { onApplyPreset(presetPts) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        color = TextPrimary
                    )
                }
            }
            Spacer(modifier = Modifier.size(4.dp))
        }
    }
}
