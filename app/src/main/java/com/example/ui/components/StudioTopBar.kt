package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.rounded.WorkspacePremium
import com.example.ui.theme.AmberWarm
import com.example.ui.theme.CyanPrecision
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.StudioElevated
import com.example.ui.theme.StudioHairline
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun StudioTopBar(
    canUndo: Boolean,
    canRedo: Boolean,
    showHistogram: Boolean,
    isPremiumAdFree: Boolean,
    onOpenPhotoClick: () -> Unit,
    onCameraClick: () -> Unit,
    onUndoClick: () -> Unit,
    onRedoClick: () -> Unit,
    onToggleHistogram: () -> Unit,
    onResetAll: () -> Unit,
    onRemoveAdsClick: () -> Unit,
    onExportClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(StudioSurface.copy(alpha = 0.94f))
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left: Open Photo + Brand pill
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(StudioElevated)
                    .border(1.dp, StudioHairline, RoundedCornerShape(12.dp))
                    .clickable(onClick = onOpenPhotoClick)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .testTag("open_photo_button"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PhotoLibrary,
                        contentDescription = "Open photo",
                        tint = CyanPrecision,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Open",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextPrimary
                    )
                }
            }

            Box(
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(StudioElevated)
                    .border(1.dp, StudioHairline, RoundedCornerShape(12.dp))
                    .clickable(onClick = onCameraClick)
                    .padding(10.dp)
                    .testTag("open_camera_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.PhotoCamera,
                    contentDescription = "Capture photo with CameraX",
                    tint = CyanPrecision,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Center: Precision action icons (Undo, Redo, Histogram, Reset, Pro/No-Ads)
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            IconButton(
                onClick = onUndoClick,
                enabled = canUndo,
                modifier = Modifier.testTag("undo_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Undo,
                    contentDescription = "Undo",
                    tint = if (canUndo) TextPrimary else TextMuted.copy(alpha = 0.45f),
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(
                onClick = onRedoClick,
                enabled = canRedo,
                modifier = Modifier.testTag("redo_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Redo,
                    contentDescription = "Redo",
                    tint = if (canRedo) TextPrimary else TextMuted.copy(alpha = 0.45f),
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(
                onClick = onToggleHistogram,
                modifier = Modifier.testTag("histogram_toggle_button")
            ) {
                Icon(
                    imageVector = if (showHistogram) Icons.Rounded.BarChart else Icons.Outlined.BarChart,
                    contentDescription = "Toggle histogram",
                    tint = if (showHistogram) CyanPrecision else TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(
                onClick = onResetAll,
                modifier = Modifier.testTag("reset_all_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.RestartAlt,
                    contentDescription = "Reset all edits",
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Box(
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isPremiumAdFree) EmeraldSuccess.copy(alpha = 0.16f)
                        else AmberWarm.copy(alpha = 0.16f)
                    )
                    .border(
                        1.dp,
                        if (isPremiumAdFree) EmeraldSuccess.copy(alpha = 0.6f)
                        else AmberWarm.copy(alpha = 0.55f),
                        RoundedCornerShape(10.dp)
                    )
                    .clickable(onClick = onRemoveAdsClick)
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .testTag(if (isPremiumAdFree) "premium_active_badge" else "remove_ads_button"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.WorkspacePremium,
                        contentDescription = if (isPremiumAdFree) "PhotoStudio Pro active" else "Remove ads",
                        tint = if (isPremiumAdFree) EmeraldSuccess else AmberWarm,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = if (isPremiumAdFree) "PRO" else "No Ads",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (isPremiumAdFree) EmeraldSuccess else AmberWarm
                    )
                }
            }
        }

        // Right: Lossless Export CTA
        Box(
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(CyanPrecision, CyanPrecision.copy(alpha = 0.82f))
                    )
                )
                .clickable(onClick = onExportClick)
                .padding(horizontal = 14.dp, vertical = 9.dp)
                .testTag("export_button"),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Download,
                    contentDescription = "Export photo",
                    tint = ObsidianVoid,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Export",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = ObsidianVoid
                )
            }
        }
    }
}
