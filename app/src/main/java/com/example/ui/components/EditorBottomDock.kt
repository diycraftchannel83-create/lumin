package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.AutoAwesomeMotion
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.Crop
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.AutoAwesomeMotion
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.EditorToolTab
import com.example.ui.theme.CyanPrecision
import com.example.ui.theme.StudioElevated
import com.example.ui.theme.StudioHairline
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.TextMuted
import com.example.ui.viewmodel.EditorScreenUiState
import com.example.ui.viewmodel.PhotoEditorViewModel

@Composable
fun EditorBottomDock(
    uiState: EditorScreenUiState,
    viewModel: PhotoEditorViewModel,
    onImportShapeFromGallery: (replaceSelected: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(StudioSurface)
            .navigationBarsPadding()
    ) {
        HorizontalDivider(color = StudioHairline, thickness = 1.dp)

        // Active Tool Panel Deck
        when (uiState.activeTab) {
            EditorToolTab.FILTERS -> {
                FiltersPanel(
                    selectedPresetId = uiState.snapshot.selectedPresetId,
                    intensity = uiState.snapshot.filterIntensity,
                    thumbnails = uiState.filterThumbnails,
                    onSelectPreset = viewModel::selectPreset,
                    onIntensityChange = { viewModel.updateFilterIntensity(it, continuous = true) },
                    onIntensityFinished = viewModel::endContinuousEdit
                )
            }
            EditorToolTab.CROP -> {
                CropPanel(
                    cropState = uiState.snapshot.crop,
                    onSelectAspect = viewModel::selectAspectPreset,
                    onStraightenChange = { viewModel.updateStraighten(it, continuous = true) },
                    onStraightenFinished = viewModel::endContinuousEdit,
                    onRotateQuarter = viewModel::rotateQuarterTurn,
                    onFlipHorizontal = viewModel::toggleFlipHorizontal,
                    onFlipVertical = viewModel::toggleFlipVertical,
                    onResetCrop = viewModel::resetCrop
                )
            }
            EditorToolTab.ADJUST -> {
                AdjustPanel(
                    adjustments = uiState.snapshot.adjustments,
                    subTab = uiState.adjustSubTab,
                    onSelectSubTab = viewModel::selectAdjustSubTab,
                    onUpdateAdjustments = viewModel::updateAdjustments,
                    onEndContinuous = viewModel::endContinuousEdit
                )
            }
            EditorToolTab.CURVES -> {
                ToneCurveEditor(
                    curvesState = uiState.snapshot.curves,
                    histogram = uiState.histogram,
                    onSelectChannel = viewModel::selectCurveChannel,
                    onUpdatePoints = viewModel::updateActiveCurvePoints,
                    onApplyPreset = viewModel::applyCurvePreset,
                    onResetChannel = viewModel::resetActiveCurveChannel,
                    onBeginDrag = viewModel::beginContinuousEdit,
                    onEndDrag = viewModel::endContinuousEdit
                )
            }
            EditorToolTab.MASKING -> {
                MaskingPanel(
                    masks = uiState.snapshot.masks,
                    selectedMaskId = uiState.snapshot.selectedMaskId,
                    showMaskOverlay = uiState.showMaskOverlay,
                    onAddMask = viewModel::addMask,
                    onSelectMask = viewModel::selectMask,
                    onDeleteMask = viewModel::deleteMask,
                    onToggleOverlay = viewModel::toggleMaskOverlay,
                    onUpdateSelectedMask = viewModel::updateSelectedMask,
                    onEndContinuous = viewModel::endContinuousEdit
                )
            }
            EditorToolTab.BLEND -> {
                BlendLayerPanel(
                    layers = uiState.snapshot.blendLayers,
                    selectedLayerId = uiState.snapshot.selectedBlendLayerId,
                    onAddLayer = viewModel::addBlendLayer,
                    onImportShapeFromGallery = onImportShapeFromGallery,
                    onSelectLayer = viewModel::selectBlendLayer,
                    onToggleVisibility = viewModel::toggleBlendLayerVisibility,
                    onDeleteLayer = viewModel::deleteBlendLayer,
                    onUpdateSelectedLayer = viewModel::updateSelectedBlendLayer,
                    onEndContinuous = viewModel::endContinuousEdit
                )
            }
            EditorToolTab.AI_STUDIO -> {
                AiStudioPanel(
                    aiState = uiState.aiState,
                    aiOperationState = uiState.aiOperationState,
                    canRevertAiBase = uiState.canRevertAiBase,
                    onSelectMode = viewModel::selectAiMode,
                    onBrushSizeChange = viewModel::updateAiBrushSize,
                    onClearBrush = viewModel::clearAiEraserStrokes,
                    onRunObjectRemoval = viewModel::runAiObjectRemoval,
                    onRunBackgroundRemoval = viewModel::runOneClickBackgroundRemoval,
                    onRunAiEnhance = viewModel::runAiEnhance,
                    onSelectBackgroundStyle = viewModel::selectBackgroundReplacement,
                    onRevertAi = viewModel::revertAiEdits
                )
            }
        }

        HorizontalDivider(color = StudioHairline.copy(alpha = 0.6f), thickness = 1.dp)

        // Bottom Studio Navigation Strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            EditorToolTab.entries.forEach { tab ->
                val selected = tab == uiState.activeTab
                val icon = when (tab) {
                    EditorToolTab.FILTERS -> if (selected) Icons.Rounded.AutoFixHigh else Icons.Outlined.AutoFixHigh
                    EditorToolTab.CROP -> if (selected) Icons.Rounded.Crop else Icons.Outlined.Crop
                    EditorToolTab.ADJUST -> if (selected) Icons.Rounded.Tune else Icons.Outlined.Tune
                    EditorToolTab.CURVES -> if (selected) Icons.AutoMirrored.Rounded.ShowChart else Icons.AutoMirrored.Outlined.ShowChart
                    EditorToolTab.MASKING -> if (selected) Icons.Rounded.Layers else Icons.Outlined.Layers
                    EditorToolTab.BLEND -> if (selected) Icons.Rounded.AutoAwesomeMotion else Icons.Outlined.AutoAwesomeMotion
                    EditorToolTab.AI_STUDIO -> if (selected) Icons.Rounded.AutoAwesome else Icons.Outlined.AutoAwesome
                }

                Column(
                    modifier = Modifier
                        .widthIn(min = 56.dp)
                        .minimumInteractiveComponentSize()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selected) StudioElevated else StudioSurface)
                        .clickable { viewModel.selectToolTab(tab) }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                        .testTag("tool_tab_${tab.name.lowercase()}"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = tab.label,
                        tint = if (selected) CyanPrecision else TextMuted,
                        modifier = Modifier.size(19.dp)
                    )
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) CyanPrecision else TextMuted,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
