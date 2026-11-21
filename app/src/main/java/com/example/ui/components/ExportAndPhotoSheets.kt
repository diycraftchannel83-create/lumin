package com.example.ui.components

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ads.AdMobCoordinator
import com.example.model.ExportFormat
import com.example.model.ExportResolution
import com.example.model.ImageMetadataInfo
import com.example.ui.theme.AmberWarm
import com.example.ui.theme.CyanPrecision
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.RoseMask
import com.example.ui.theme.StudioElevated
import com.example.ui.theme.StudioHairline
import com.example.ui.theme.StudioPanel
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.ExportUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportModalSheet(
    metadata: ImageMetadataInfo,
    hasTransparentAlpha: Boolean,
    isPremiumAdFree: Boolean = false,
    exportUiState: ExportUiState,
    onExport: (ExportFormat, ExportResolution) -> Unit,
    onRemoveAdsClick: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedFormat by remember(hasTransparentAlpha) {
        mutableStateOf(if (hasTransparentAlpha) ExportFormat.PNG_LOSSLESS else ExportFormat.PNG_LOSSLESS)
    }
    var selectedResolution by remember { mutableStateOf(ExportResolution.ORIGINAL) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = StudioSurface,
        contentColor = TextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Lossless High-Res Export",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                    Text(
                        text = "Source: ${metadata.width} × ${metadata.height} px (${metadata.megapixels})",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }

            // Format Selection Cards
            Text(
                text = "FORMAT & COMPRESSION",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ExportFormat.entries.forEach { format ->
                    val selected = format == selectedFormat
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) StudioElevated else StudioPanel)
                            .border(
                                1.5.dp,
                                if (selected) CyanPrecision else StudioHairline,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { selectedFormat = format }
                            .padding(12.dp)
                            .testTag("export_format_${format.name.lowercase()}"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = format.label,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TextPrimary
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(
                                            if (selected) CyanPrecision.copy(alpha = 0.2f)
                                            else StudioHairline.copy(alpha = 0.5f)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = format.badge,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (selected) CyanPrecision else TextSecondary
                                    )
                                }
                            }
                            Text(
                                text = format.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            // Resolution Chips
            Text(
                text = "OUTPUT RESOLUTION",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ExportResolution.entries.forEach { res ->
                    val selected = res == selectedResolution
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) CyanPrecision.copy(alpha = 0.16f) else StudioPanel)
                            .border(
                                1.dp,
                                if (selected) CyanPrecision else StudioHairline,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { selectedResolution = res }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = res.label.substringBefore(" "),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selected) CyanPrecision else TextSecondary
                        )
                    }
                }
            }

            // Export Status / Action
            when (exportUiState) {
                is ExportUiState.Exporting -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(StudioPanel)
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            color = CyanPrecision,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Rendering full-resolution ${selectedFormat.label}...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary
                        )
                    }
                }
                is ExportUiState.Success -> {
                    val res = exportUiState.result
                    val sizeMb = String.format("%.2f MB", res.fileSizeBytes / (1024f * 1024f))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(EmeraldSuccess.copy(alpha = 0.12f))
                            .border(1.dp, EmeraldSuccess.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = EmeraldSuccess
                            )
                            Text(
                                text = "Saved (${res.width}×${res.height} • $sizeMb)",
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary
                            )
                        }
                        Text(
                            text = res.displayPath,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    AdMobCoordinator.suppressNextAppOpen()
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = res.mimeType
                                        putExtra(Intent.EXTRA_STREAM, res.shareUri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(
                                        Intent.createChooser(shareIntent, "Share Exported Photo")
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Share,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Share File")
                            }
                            Button(
                                onClick = { onExport(selectedFormat, selectedResolution) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CyanPrecision,
                                    contentColor = ObsidianVoid
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Export Again")
                            }
                        }
                    }
                }
                is ExportUiState.Error -> {
                    Text(
                        text = exportUiState.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = RoseMask
                    )
                }
                is ExportUiState.Idle -> Unit
            }

            if (exportUiState !is ExportUiState.Exporting && exportUiState !is ExportUiState.Success) {
                Button(
                    onClick = { onExport(selectedFormat, selectedResolution) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyanPrecision,
                        contentColor = ObsidianVoid
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("confirm_export_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Download,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Export ${selectedFormat.label}",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            if (!isPremiumAdFree && onRemoveAdsClick != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(AmberWarm.copy(alpha = 0.12f))
                        .border(1.dp, AmberWarm.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                        .clickable {
                            onDismiss()
                            onRemoveAdsClick()
                        }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.WorkspacePremium,
                            contentDescription = null,
                            tint = AmberWarm,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Go Ad-Free with PhotoStudio Pro",
                            style = MaterialTheme.typography.labelLarge,
                            color = TextPrimary
                        )
                    }
                    Text(
                        text = "Upgrade",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = AmberWarm
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoSourceModalSheet(
    onPickFromGallery: () -> Unit,
    onCaptureCamera: () -> Unit,
    onSelectSample: (Int, String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = StudioSurface,
        contentColor = TextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 26.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Open Photograph",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        onDismiss()
                        onPickFromGallery()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyanPrecision,
                        contentColor = ObsidianVoid
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("gallery_picker_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PhotoLibrary,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Gallery",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }

                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onCaptureCamera()
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("sheet_camera_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PhotoCamera,
                        contentDescription = null,
                        tint = AmberWarm,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "CameraX",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                }
            }

            Text(
                text = "STUDIO SAMPLE RAW ASSETS",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SamplePhotoCard(
                    drawableRes = R.drawable.img_sample_landscape,
                    title = "Fjord Golden Hour",
                    subtitle = "Landscape • HDR",
                    onClick = {
                        onSelectSample(R.drawable.img_sample_landscape, "Fjord Golden Hour")
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f)
                )
                SamplePhotoCard(
                    drawableRes = R.drawable.img_sample_studio,
                    title = "Vintage Rangefinder",
                    subtitle = "Studio • Subject",
                    onClick = {
                        onSelectSample(R.drawable.img_sample_studio, "Vintage Rangefinder")
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SamplePhotoCard(
    drawableRes: Int,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(StudioPanel)
            .border(1.dp, StudioHairline, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Image(
            painter = painterResource(id = drawableRes),
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(92.dp)
        )
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = AmberWarm
            )
        }
    }
}
