package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.CurveChannel
import com.example.model.CurvePoint
import com.example.model.FilterCatalog
import com.example.model.HistogramData
import com.example.model.ToneCurvesState
import com.example.ui.theme.CyanPrecision
import com.example.ui.theme.RoseMask
import com.example.ui.theme.StudioElevated
import com.example.ui.theme.StudioHairline
import com.example.ui.theme.StudioPanel
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

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
    var selectedNodeIndex by remember(activeChannel) { mutableIntStateOf(-1) }
    val safeSelectedIndex = if (selectedNodeIndex in points.indices) selectedNodeIndex else -1
    val canDeleteSelected = safeSelectedIndex in 1 until points.lastIndex && points.size > 2

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Channel selector + Remove Node / Reset row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CurveChannel.entries.forEach { channel ->
                    val selected = channel == activeChannel
                    val dotColor = curveChannelColor(channel)
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selected) StudioElevated else StudioPanel)
                            .border(
                                1.dp,
                                if (selected) dotColor.copy(alpha = 0.7f) else StudioHairline,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                selectedNodeIndex = -1
                                onSelectChannel(channel)
                            }
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

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (canDeleteSelected) {
                    Text(
                        text = "Del Pt",
                        style = MaterialTheme.typography.labelMedium,
                        color = RoseMask,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable {
                                val updated = points.toMutableList().apply { removeAt(safeSelectedIndex) }
                                selectedNodeIndex = -1
                                onUpdatePoints(updated, false)
                            }
                            .padding(horizontal = 6.dp, vertical = 5.dp)
                    )
                }
                Text(
                    text = "Reset",
                    style = MaterialTheme.typography.labelMedium,
                    color = CyanPrecision,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable {
                            selectedNodeIndex = -1
                            onResetChannel()
                        }
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                        .testTag("reset_curve_button")
                )
            }
        }

        // Interactive Curve Graph Canvas
        ToneCurveVisualGraph(
            activeChannel = activeChannel,
            points = points,
            histogram = histogram,
            selectedNodeIndex = safeSelectedIndex,
            onSelectNode = { selectedNodeIndex = it },
            onUpdatePoints = onUpdatePoints,
            onBeginDrag = onBeginDrag,
            onEndDrag = onEndDrag,
            graphHeight = 132.dp
        )

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
                        .clickable {
                            selectedNodeIndex = -1
                            onApplyPreset(presetPts)
                        }
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
