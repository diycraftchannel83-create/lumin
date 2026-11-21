package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.RotateLeft
import androidx.compose.material.icons.automirrored.rounded.RotateRight
import androidx.compose.material.icons.rounded.Flip
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.AdjustSubTab
import com.example.model.AspectRatioPreset
import com.example.model.CropGeometryState
import com.example.model.FilterCatalog
import com.example.model.PhotoAdjustments
import com.example.ui.theme.AmberWarm
import com.example.ui.theme.CyanPrecision
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.StudioElevated
import com.example.ui.theme.StudioHairline
import com.example.ui.theme.StudioPanel
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.abs

@Composable
fun PrecisionSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    formattedValue: String,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    onResetToDefault: () -> Unit,
    accentColor: Color = CyanPrecision,
    testTagName: String? = null,
    modifier: Modifier = Modifier
) {
    val isModified = abs(value) > 0.01f
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isModified) TextPrimary else TextSecondary,
                modifier = Modifier.clickable(onClick = onResetToDefault)
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isModified) accentColor.copy(alpha = 0.16f) else StudioPanel)
                    .clickable(onClick = onResetToDefault)
                    .padding(horizontal = 7.dp, vertical = 2.dp)
            ) {
                Text(
                    text = formattedValue,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isModified) accentColor else TextMuted
                )
            }
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = TextPrimary,
                activeTrackColor = accentColor,
                inactiveTrackColor = StudioHairline
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp)
                .then(if (testTagName != null) Modifier.testTag(testTagName) else Modifier)
        )
    }
}

@Composable
fun FiltersPanel(
    selectedPresetId: String,
    intensity: Float,
    thumbnails: Map<String, Bitmap>,
    onSelectPreset: (String) -> Unit,
    onIntensityChange: (Float) -> Unit,
    onIntensityFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(FilterCatalog.presets, key = { it.id }) { preset ->
                val isSelected = preset.id == selectedPresetId
                val thumb = thumbnails[preset.id]
                Column(
                    modifier = Modifier
                        .width(74.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) StudioElevated else StudioPanel)
                        .border(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) CyanPrecision else StudioHairline,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable { onSelectPreset(preset.id) }
                        .padding(4.dp)
                        .testTag("filter_preset_${preset.id}"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(ObsidianVoid),
                        contentAlignment = Alignment.Center
                    ) {
                        if (thumb != null) {
                            Image(
                                bitmap = thumb.asImageBitmap(),
                                contentDescription = preset.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.matchParentSize()
                            )
                        }
                    }
                    Text(
                        text = preset.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) CyanPrecision else TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        if (selectedPresetId != "original") {
            PrecisionSlider(
                label = "Filter Intensity",
                value = intensity * 100f,
                valueRange = 0f..100f,
                formattedValue = "${(intensity * 100).toInt()}%",
                onValueChange = { onIntensityChange(it / 100f) },
                onValueChangeFinished = onIntensityFinished,
                onResetToDefault = {
                    onIntensityChange(1f)
                    onIntensityFinished()
                },
                modifier = Modifier.padding(horizontal = 14.dp)
            )
        }
    }
}

@Composable
fun CropPanel(
    cropState: CropGeometryState,
    onSelectAspect: (AspectRatioPreset) -> Unit,
    onStraightenChange: (Float) -> Unit,
    onStraightenFinished: () -> Unit,
    onRotateQuarter: (Int) -> Unit,
    onFlipHorizontal: () -> Unit,
    onFlipVertical: () -> Unit,
    onResetCrop: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Aspect Ratio Presets
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AspectRatioPreset.entries.forEach { preset ->
                val selected = preset == cropState.aspectPreset
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selected) CyanPrecision.copy(alpha = 0.18f) else StudioPanel)
                        .border(
                            1.dp,
                            if (selected) CyanPrecision else StudioHairline,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { onSelectAspect(preset) }
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                        .testTag("aspect_${preset.name.lowercase()}")
                ) {
                    Text(
                        text = preset.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (selected) CyanPrecision else TextSecondary
                    )
                }
            }
        }

        // Straighten Slider
        PrecisionSlider(
            label = "Straighten Angle",
            value = cropState.straightenDegrees,
            valueRange = -45f..45f,
            formattedValue = String.format("%+.1f°", cropState.straightenDegrees),
            onValueChange = onStraightenChange,
            onValueChangeFinished = onStraightenFinished,
            onResetToDefault = {
                onStraightenChange(0f)
                onStraightenFinished()
            },
            accentColor = AmberWarm
        )

        // Geometry Transform Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GeometryActionChip(
                    icon = Icons.AutoMirrored.Rounded.RotateLeft,
                    label = "-90°",
                    onClick = { onRotateQuarter(-1) }
                )
                GeometryActionChip(
                    icon = Icons.AutoMirrored.Rounded.RotateRight,
                    label = "+90°",
                    onClick = { onRotateQuarter(1) }
                )
                GeometryActionChip(
                    icon = Icons.Rounded.Flip,
                    label = "Flip H",
                    active = cropState.flipHorizontal,
                    onClick = onFlipHorizontal
                )
            }

            IconButton(
                onClick = onResetCrop,
                modifier = Modifier.testTag("reset_crop_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.RestartAlt,
                    contentDescription = "Reset crop",
                    tint = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun GeometryActionChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(9.dp))
            .background(if (active) CyanPrecision.copy(alpha = 0.16f) else StudioElevated)
            .border(
                1.dp,
                if (active) CyanPrecision else StudioHairline,
                RoundedCornerShape(9.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (active) CyanPrecision else TextPrimary,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (active) CyanPrecision else TextPrimary
        )
    }
}

@Composable
fun AdjustPanel(
    adjustments: PhotoAdjustments,
    subTab: AdjustSubTab,
    onSelectSubTab: (AdjustSubTab) -> Unit,
    onUpdateAdjustments: (Boolean, (PhotoAdjustments) -> PhotoAdjustments) -> Unit,
    onEndContinuous: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Sub-tab Pills: Light | Color | Effects
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(ObsidianVoid)
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            AdjustSubTab.entries.forEach { tab ->
                val selected = tab == subTab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selected) StudioElevated else Color.Transparent)
                        .clickable { onSelectSubTab(tab) }
                        .padding(vertical = 7.dp)
                        .testTag("adjust_subtab_${tab.name.lowercase()}"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                        ),
                        color = if (selected) CyanPrecision else TextSecondary
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(168.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            when (subTab) {
                AdjustSubTab.LIGHT -> {
                    PrecisionSlider(
                        label = "Exposure",
                        value = adjustments.exposure,
                        valueRange = -2f..2f,
                        formattedValue = String.format("%+.2f EV", adjustments.exposure),
                        onValueChange = { v -> onUpdateAdjustments(true) { it.copy(exposure = v) } },
                        onValueChangeFinished = onEndContinuous,
                        onResetToDefault = { onUpdateAdjustments(false) { it.copy(exposure = 0f) } },
                        testTagName = "slider_exposure"
                    )
                    PrecisionSlider(
                        label = "Contrast",
                        value = adjustments.contrast,
                        valueRange = -100f..100f,
                        formattedValue = String.format("%+.0f", adjustments.contrast),
                        onValueChange = { v -> onUpdateAdjustments(true) { it.copy(contrast = v) } },
                        onValueChangeFinished = onEndContinuous,
                        onResetToDefault = { onUpdateAdjustments(false) { it.copy(contrast = 0f) } },
                        testTagName = "slider_contrast"
                    )
                    PrecisionSlider(
                        label = "Highlights",
                        value = adjustments.highlights,
                        valueRange = -100f..100f,
                        formattedValue = String.format("%+.0f", adjustments.highlights),
                        onValueChange = { v -> onUpdateAdjustments(true) { it.copy(highlights = v) } },
                        onValueChangeFinished = onEndContinuous,
                        onResetToDefault = { onUpdateAdjustments(false) { it.copy(highlights = 0f) } }
                    )
                    PrecisionSlider(
                        label = "Shadows",
                        value = adjustments.shadows,
                        valueRange = -100f..100f,
                        formattedValue = String.format("%+.0f", adjustments.shadows),
                        onValueChange = { v -> onUpdateAdjustments(true) { it.copy(shadows = v) } },
                        onValueChangeFinished = onEndContinuous,
                        onResetToDefault = { onUpdateAdjustments(false) { it.copy(shadows = 0f) } }
                    )
                    PrecisionSlider(
                        label = "Whites",
                        value = adjustments.whites,
                        valueRange = -100f..100f,
                        formattedValue = String.format("%+.0f", adjustments.whites),
                        onValueChange = { v -> onUpdateAdjustments(true) { it.copy(whites = v) } },
                        onValueChangeFinished = onEndContinuous,
                        onResetToDefault = { onUpdateAdjustments(false) { it.copy(whites = 0f) } }
                    )
                    PrecisionSlider(
                        label = "Blacks",
                        value = adjustments.blacks,
                        valueRange = -100f..100f,
                        formattedValue = String.format("%+.0f", adjustments.blacks),
                        onValueChange = { v -> onUpdateAdjustments(true) { it.copy(blacks = v) } },
                        onValueChangeFinished = onEndContinuous,
                        onResetToDefault = { onUpdateAdjustments(false) { it.copy(blacks = 0f) } }
                    )
                }
                AdjustSubTab.COLOR -> {
                    PrecisionSlider(
                        label = "Temperature",
                        value = adjustments.temperature,
                        valueRange = -100f..100f,
                        formattedValue = String.format("%+.0f", adjustments.temperature),
                        onValueChange = { v -> onUpdateAdjustments(true) { it.copy(temperature = v) } },
                        onValueChangeFinished = onEndContinuous,
                        onResetToDefault = { onUpdateAdjustments(false) { it.copy(temperature = 0f) } },
                        accentColor = AmberWarm
                    )
                    PrecisionSlider(
                        label = "Tint",
                        value = adjustments.tint,
                        valueRange = -100f..100f,
                        formattedValue = String.format("%+.0f", adjustments.tint),
                        onValueChange = { v -> onUpdateAdjustments(true) { it.copy(tint = v) } },
                        onValueChangeFinished = onEndContinuous,
                        onResetToDefault = { onUpdateAdjustments(false) { it.copy(tint = 0f) } }
                    )
                    PrecisionSlider(
                        label = "Vibrance",
                        value = adjustments.vibrance,
                        valueRange = -100f..100f,
                        formattedValue = String.format("%+.0f", adjustments.vibrance),
                        onValueChange = { v -> onUpdateAdjustments(true) { it.copy(vibrance = v) } },
                        onValueChangeFinished = onEndContinuous,
                        onResetToDefault = { onUpdateAdjustments(false) { it.copy(vibrance = 0f) } }
                    )
                    PrecisionSlider(
                        label = "Saturation",
                        value = adjustments.saturation,
                        valueRange = -100f..100f,
                        formattedValue = String.format("%+.0f", adjustments.saturation),
                        onValueChange = { v -> onUpdateAdjustments(true) { it.copy(saturation = v) } },
                        onValueChangeFinished = onEndContinuous,
                        onResetToDefault = { onUpdateAdjustments(false) { it.copy(saturation = 0f) } },
                        testTagName = "slider_saturation"
                    )
                }
                AdjustSubTab.DETAIL -> {
                    PrecisionSlider(
                        label = "Clarity & Texture",
                        value = adjustments.clarity,
                        valueRange = 0f..100f,
                        formattedValue = String.format("%.0f", adjustments.clarity),
                        onValueChange = { v -> onUpdateAdjustments(true) { it.copy(clarity = v) } },
                        onValueChangeFinished = onEndContinuous,
                        onResetToDefault = { onUpdateAdjustments(false) { it.copy(clarity = 0f) } }
                    )
                    PrecisionSlider(
                        label = "Vignette",
                        value = adjustments.vignette,
                        valueRange = -100f..100f,
                        formattedValue = String.format("%+.0f", adjustments.vignette),
                        onValueChange = { v -> onUpdateAdjustments(true) { it.copy(vignette = v) } },
                        onValueChangeFinished = onEndContinuous,
                        onResetToDefault = { onUpdateAdjustments(false) { it.copy(vignette = 0f) } }
                    )
                    PrecisionSlider(
                        label = "Film Grain",
                        value = adjustments.grain,
                        valueRange = 0f..100f,
                        formattedValue = String.format("%.0f", adjustments.grain),
                        onValueChange = { v -> onUpdateAdjustments(true) { it.copy(grain = v) } },
                        onValueChangeFinished = onEndContinuous,
                        onResetToDefault = { onUpdateAdjustments(false) { it.copy(grain = 0f) } }
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}
