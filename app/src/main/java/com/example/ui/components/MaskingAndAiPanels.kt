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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FlipToBack
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.PersonSearch
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.AiStudioState
import com.example.model.AiToolMode
import com.example.model.BackgroundReplacement
import com.example.model.MaskLayer
import com.example.model.MaskType
import com.example.ui.theme.AmberWarm
import com.example.ui.theme.CyanPrecision
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.RoseMask
import com.example.ui.theme.StudioElevated
import com.example.ui.theme.StudioHairline
import com.example.ui.theme.StudioPanel
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.AiOperationUiState

@Composable
fun MaskingPanel(
    masks: List<MaskLayer>,
    selectedMaskId: String?,
    showMaskOverlay: Boolean,
    onAddMask: (MaskType) -> Unit,
    onSelectMask: (String) -> Unit,
    onDeleteMask: (String) -> Unit,
    onToggleOverlay: () -> Unit,
    onUpdateSelectedMask: (Boolean, (MaskLayer) -> MaskLayer) -> Unit,
    onEndContinuous: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedMask = masks.find { it.id == selectedMaskId }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Add Mask + Overlay Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MaskType.entries.forEach { type ->
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(StudioElevated)
                            .border(1.dp, StudioHairline, RoundedCornerShape(8.dp))
                            .clickable { onAddMask(type) }
                            .padding(horizontal = 9.dp, vertical = 6.dp)
                            .testTag("add_mask_${type.name.lowercase()}"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "Add ${type.label} mask",
                            tint = CyanPrecision,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = type.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = TextPrimary
                        )
                    }
                }
            }

            IconButton(
                onClick = onToggleOverlay,
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    imageVector = if (showMaskOverlay) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                    contentDescription = "Toggle mask overlay",
                    tint = if (showMaskOverlay) RoseMask else TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        if (masks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(126.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(StudioPanel)
                    .border(1.dp, StudioHairline, RoundedCornerShape(12.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Layers,
                        contentDescription = null,
                        tint = CyanPrecision,
                        modifier = Modifier.size(26.dp)
                    )
                    Text(
                        text = "Tap + Radial, + Linear, or + Brush above",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary
                    )
                    Text(
                        text = "Selectively adjust exposure, color & clarity on localized regions.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        } else {
            // Mask Layer Selector Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                masks.forEach { mask ->
                    val isSel = mask.id == selectedMaskId
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSel) RoseMask.copy(alpha = 0.16f) else StudioPanel)
                            .border(
                                1.dp,
                                if (isSel) RoseMask else StudioHairline,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { onSelectMask(mask.id) }
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = mask.name,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isSel) RoseMask else TextPrimary
                        )
                    }
                }
            }

            // Selected Mask Properties & Local Adjustments
            if (selectedMask != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(114.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(7.dp))
                                    .background(
                                        if (selectedMask.inverted) CyanPrecision.copy(alpha = 0.2f)
                                        else StudioElevated
                                    )
                                    .clickable {
                                        onUpdateSelectedMask(false) { it.copy(inverted = !it.inverted) }
                                    }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.FlipToBack,
                                        contentDescription = "Invert mask",
                                        tint = if (selectedMask.inverted) CyanPrecision else TextSecondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = if (selectedMask.inverted) "Inverted" else "Invert",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (selectedMask.inverted) CyanPrecision else TextSecondary
                                    )
                                }
                            }
                            if (selectedMask.type == MaskType.BRUSH) {
                                Text(
                                    text = "Paint directly on photo",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AmberWarm,
                                    modifier = Modifier.align(Alignment.CenterVertically)
                                )
                            }
                        }

                        IconButton(
                            onClick = { onDeleteMask(selectedMask.id) },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteOutline,
                                contentDescription = "Delete mask",
                                tint = RoseMask,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    PrecisionSlider(
                        label = "Feather",
                        value = selectedMask.feather * 100f,
                        valueRange = 5f..100f,
                        formattedValue = "${(selectedMask.feather * 100).toInt()}%",
                        onValueChange = { v -> onUpdateSelectedMask(true) { it.copy(feather = v / 100f) } },
                        onValueChangeFinished = onEndContinuous,
                        onResetToDefault = { onUpdateSelectedMask(false) { it.copy(feather = 0.45f) } },
                        accentColor = RoseMask
                    )
                    PrecisionSlider(
                        label = "Local Exposure",
                        value = selectedMask.adjustments.exposure,
                        valueRange = -2f..2f,
                        formattedValue = String.format("%+.2f EV", selectedMask.adjustments.exposure),
                        onValueChange = { v ->
                            onUpdateSelectedMask(true) {
                                it.copy(adjustments = it.adjustments.copy(exposure = v))
                            }
                        },
                        onValueChangeFinished = onEndContinuous,
                        onResetToDefault = {
                            onUpdateSelectedMask(false) {
                                it.copy(adjustments = it.adjustments.copy(exposure = 0f))
                            }
                        }
                    )
                    PrecisionSlider(
                        label = "Local Contrast",
                        value = selectedMask.adjustments.contrast,
                        valueRange = -100f..100f,
                        formattedValue = String.format("%+.0f", selectedMask.adjustments.contrast),
                        onValueChange = { v ->
                            onUpdateSelectedMask(true) {
                                it.copy(adjustments = it.adjustments.copy(contrast = v))
                            }
                        },
                        onValueChangeFinished = onEndContinuous,
                        onResetToDefault = {
                            onUpdateSelectedMask(false) {
                                it.copy(adjustments = it.adjustments.copy(contrast = 0f))
                            }
                        }
                    )
                    PrecisionSlider(
                        label = "Local Saturation",
                        value = selectedMask.adjustments.saturation,
                        valueRange = -100f..100f,
                        formattedValue = String.format("%+.0f", selectedMask.adjustments.saturation),
                        onValueChange = { v ->
                            onUpdateSelectedMask(true) {
                                it.copy(adjustments = it.adjustments.copy(saturation = v))
                            }
                        },
                        onValueChangeFinished = onEndContinuous,
                        onResetToDefault = {
                            onUpdateSelectedMask(false) {
                                it.copy(adjustments = it.adjustments.copy(saturation = 0f))
                            }
                        }
                    )
                    PrecisionSlider(
                        label = "Local Temperature",
                        value = selectedMask.adjustments.temperature,
                        valueRange = -100f..100f,
                        formattedValue = String.format("%+.0f", selectedMask.adjustments.temperature),
                        onValueChange = { v ->
                            onUpdateSelectedMask(true) {
                                it.copy(adjustments = it.adjustments.copy(temperature = v))
                            }
                        },
                        onValueChangeFinished = onEndContinuous,
                        onResetToDefault = {
                            onUpdateSelectedMask(false) {
                                it.copy(adjustments = it.adjustments.copy(temperature = 0f))
                            }
                        },
                        accentColor = AmberWarm
                    )
                    PrecisionSlider(
                        label = "Local Clarity",
                        value = selectedMask.adjustments.clarity,
                        valueRange = 0f..100f,
                        formattedValue = String.format("%.0f", selectedMask.adjustments.clarity),
                        onValueChange = { v ->
                            onUpdateSelectedMask(true) {
                                it.copy(adjustments = it.adjustments.copy(clarity = v))
                            }
                        },
                        onValueChangeFinished = onEndContinuous,
                        onResetToDefault = {
                            onUpdateSelectedMask(false) {
                                it.copy(adjustments = it.adjustments.copy(clarity = 0f))
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AiStudioPanel(
    aiState: AiStudioState,
    aiOperationState: AiOperationUiState,
    canRevertAiBase: Boolean,
    onSelectMode: (AiToolMode) -> Unit,
    onBrushSizeChange: (Float) -> Unit,
    onClearBrush: () -> Unit,
    onRunObjectRemoval: () -> Unit,
    onRunBackgroundRemoval: () -> Unit,
    onSelectBackgroundStyle: (BackgroundReplacement) -> Unit,
    onRevertAi: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Mode Switcher: Object Eraser | 1-Click BG Remover
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(ObsidianVoid)
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            AiToolMode.entries.forEach { mode ->
                val selected = mode == aiState.activeMode
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selected) StudioElevated else Color.Transparent)
                        .clickable { onSelectMode(mode) }
                        .padding(vertical = 7.dp)
                        .testTag("ai_mode_${mode.name.lowercase()}"),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (mode == AiToolMode.OBJECT_ERASER) Icons.Rounded.Brush
                        else Icons.Rounded.PersonSearch,
                        contentDescription = null,
                        tint = if (selected) CyanPrecision else TextSecondary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = mode.label,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                        ),
                        color = if (selected) CyanPrecision else TextSecondary
                    )
                }
            }
        }

        // Status / Error Banner
        when (aiOperationState) {
            is AiOperationUiState.Error -> {
                Text(
                    text = aiOperationState.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = RoseMask,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(RoseMask.copy(alpha = 0.12f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
            is AiOperationUiState.Success -> {
                Text(
                    text = aiOperationState.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = EmeraldSuccess,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(EmeraldSuccess.copy(alpha = 0.12f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
            else -> Unit
        }

        when (aiState.activeMode) {
            AiToolMode.OBJECT_ERASER -> {
                PrecisionSlider(
                    label = "Eraser Brush Size (paint on photo)",
                    value = aiState.eraserBrushSize * 100f,
                    valueRange = 2f..14f,
                    formattedValue = "${(aiState.eraserBrushSize * 100).toInt()}%",
                    onValueChange = { onBrushSizeChange(it / 100f) },
                    onValueChangeFinished = {},
                    onResetToDefault = { onBrushSizeChange(0.05f) },
                    accentColor = RoseMask
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = if (aiState.eraserStrokes.isNotEmpty()) onClearBrush else onRevertAi,
                        enabled = aiState.eraserStrokes.isNotEmpty() || canRevertAiBase,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(0.9f)
                    ) {
                        Text(
                            text = if (aiState.eraserStrokes.isNotEmpty()) "Clear Brush" else "Revert AI",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }

                    Button(
                        onClick = onRunObjectRemoval,
                        enabled = aiOperationState !is AiOperationUiState.Loading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyanPrecision,
                            contentColor = ObsidianVoid
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1.4f)
                            .testTag("run_ai_object_erase_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoFixHigh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Erase Object (AI)",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }

            AiToolMode.BACKGROUND_REMOVER -> {
                if (aiState.hasBackgroundRemoved) {
                    Text(
                        text = "BACKDROP",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        BackgroundReplacement.entries.forEach { bg ->
                            val sel = bg == aiState.backgroundStyle
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (sel) CyanPrecision.copy(alpha = 0.18f) else StudioPanel)
                                    .border(
                                        1.dp,
                                        if (sel) CyanPrecision else StudioHairline,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { onSelectBackgroundStyle(bg) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = bg.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (sel) CyanPrecision else TextSecondary
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (canRevertAiBase) {
                        OutlinedButton(
                            onClick = onRevertAi,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(0.8f)
                        ) {
                            Text("Revert AI", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    Button(
                        onClick = onRunBackgroundRemoval,
                        enabled = aiOperationState !is AiOperationUiState.Loading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyanPrecision,
                            contentColor = ObsidianVoid
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1.5f)
                            .testTag("run_ai_bg_remove_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "1-Click Remove BG",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}
