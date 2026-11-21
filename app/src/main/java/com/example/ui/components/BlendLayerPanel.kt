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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Title
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.BlendFontStyle
import com.example.model.BlendLayer
import com.example.model.BlendLayerKind
import com.example.model.BlendShapeType
import com.example.model.GalleryShapeCutout
import com.example.model.LayerBlendMode
import com.example.ui.theme.AmberWarm
import com.example.ui.theme.CyanPrecision
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.RoseMask
import com.example.ui.theme.StudioElevated
import com.example.ui.theme.StudioHairline
import com.example.ui.theme.StudioPanel
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

private val blendColorSwatches: List<Int> = listOf(
    0xFFFFFFFF.toInt(),
    0xFF0B0D11.toInt(),
    0xFF38BDF8.toInt(),
    0xFFF59E0B.toInt(),
    0xFFFB7185.toInt(),
    0xFF10B981.toInt(),
    0xFFFDE68A.toInt(),
    0xFFA78BFA.toInt()
)

@Composable
fun BlendLayerPanel(
    layers: List<BlendLayer>,
    selectedLayerId: String?,
    onAddLayer: (BlendLayerKind) -> Unit,
    onImportShapeFromGallery: (replaceSelected: Boolean) -> Unit,
    onSelectLayer: (String) -> Unit,
    onToggleVisibility: (String) -> Unit,
    onDeleteLayer: (String) -> Unit,
    onUpdateSelectedLayer: (Boolean, (BlendLayer) -> BlendLayer) -> Unit,
    onEndContinuous: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedLayer = layers.find { it.id == selectedLayerId }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Add Text / Add Shape / Import Shape from Gallery row
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
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(StudioElevated)
                        .border(1.dp, StudioHairline, RoundedCornerShape(8.dp))
                        .clickable { onAddLayer(BlendLayerKind.TEXT) }
                        .padding(horizontal = 9.dp, vertical = 6.dp)
                        .testTag("add_blend_text_button"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        tint = CyanPrecision,
                        modifier = Modifier.size(14.dp)
                    )
                    Icon(
                        imageVector = Icons.Rounded.Title,
                        contentDescription = "Add text blend layer",
                        tint = TextPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Text",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextPrimary
                    )
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(StudioElevated)
                        .border(1.dp, StudioHairline, RoundedCornerShape(8.dp))
                        .clickable { onAddLayer(BlendLayerKind.SHAPE) }
                        .padding(horizontal = 9.dp, vertical = 6.dp)
                        .testTag("add_blend_shape_button"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        tint = AmberWarm,
                        modifier = Modifier.size(14.dp)
                    )
                    Icon(
                        imageVector = Icons.Rounded.Category,
                        contentDescription = "Add shape blend layer",
                        tint = TextPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Shape",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextPrimary
                    )
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(StudioElevated)
                        .border(1.dp, EmeraldSuccess.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
                        .clickable { onImportShapeFromGallery(false) }
                        .padding(horizontal = 9.dp, vertical = 6.dp)
                        .testTag("import_blend_shape_button"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AddPhotoAlternate,
                        contentDescription = stringResource(R.string.import_shape_from_gallery),
                        tint = EmeraldSuccess,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = stringResource(R.string.import_shape_short),
                        style = MaterialTheme.typography.labelMedium,
                        color = TextPrimary
                    )
                }
            }

            if (selectedLayer != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onToggleVisibility(selectedLayer.id) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (selectedLayer.visible) Icons.Rounded.Visibility
                            else Icons.Rounded.VisibilityOff,
                            contentDescription = "Toggle layer visibility",
                            tint = if (selectedLayer.visible) CyanPrecision else TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = { onDeleteLayer(selectedLayer.id) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "Delete blend layer",
                            tint = RoseMask,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        if (layers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(126.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(StudioPanel)
                    .border(1.dp, StudioHairline, RoundedCornerShape(12.dp))
                    .padding(14.dp),
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
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Blend Text, Shapes & Gallery PNGs",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary
                    )
                    Text(
                        text = "Add typography, vector shapes, or import a shape from your gallery with Overlay, Multiply & Screen.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        } else {
            // Layer Stack Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                layers.forEach { layer ->
                    val isSel = layer.id == selectedLayerId
                    val chipText = when (layer.kind) {
                        BlendLayerKind.TEXT -> "${layer.name}: ${layer.text.take(10)}"
                        BlendLayerKind.SHAPE -> "${layer.name}: ${layer.shapeType.label}"
                        BlendLayerKind.GALLERY_SHAPE -> "${layer.name}: ${layer.galleryShapeLabel.take(12)}"
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSel) CyanPrecision.copy(alpha = 0.18f) else StudioPanel)
                            .border(
                                1.dp,
                                if (isSel) CyanPrecision else StudioHairline,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { onSelectLayer(layer.id) }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = chipText,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isSel) CyanPrecision else TextPrimary
                        )
                    }
                }
            }

            if (selectedLayer != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(128.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Text, Built-in Shape, OR Imported Gallery Shape controls
                    when (selectedLayer.kind) {
                        BlendLayerKind.TEXT -> {
                            OutlinedTextField(
                                value = selectedLayer.text,
                                onValueChange = { txt ->
                                    onUpdateSelectedLayer(false) { it.copy(text = txt) }
                                },
                                label = { Text("Overlay Text") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CyanPrecision,
                                    unfocusedBorderColor = StudioHairline,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("blend_text_input")
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                BlendFontStyle.entries.forEach { font ->
                                    val sel = font == selectedLayer.fontStyle
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(7.dp))
                                            .background(if (sel) StudioElevated else StudioPanel)
                                            .border(
                                                1.dp,
                                                if (sel) CyanPrecision else StudioHairline,
                                                RoundedCornerShape(7.dp)
                                            )
                                            .clickable {
                                                onUpdateSelectedLayer(false) { it.copy(fontStyle = font) }
                                            }
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Text(
                                            text = font.label,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (sel) CyanPrecision else TextSecondary
                                        )
                                    }
                                }
                            }
                        }

                        BlendLayerKind.SHAPE -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                BlendShapeType.entries.forEach { shape ->
                                    val sel = shape == selectedLayer.shapeType
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(7.dp))
                                            .background(if (sel) StudioElevated else StudioPanel)
                                            .border(
                                                1.dp,
                                                if (sel) AmberWarm else StudioHairline,
                                                RoundedCornerShape(7.dp)
                                            )
                                            .clickable {
                                                onUpdateSelectedLayer(false) { it.copy(shapeType = shape) }
                                            }
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Text(
                                            text = shape.label,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (sel) AmberWarm else TextSecondary
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(7.dp))
                                        .background(
                                            if (selectedLayer.strokeOnly) CyanPrecision.copy(alpha = 0.2f)
                                            else StudioPanel
                                        )
                                        .border(1.dp, StudioHairline, RoundedCornerShape(7.dp))
                                        .clickable {
                                            onUpdateSelectedLayer(false) { it.copy(strokeOnly = !it.strokeOnly) }
                                        }
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = if (selectedLayer.strokeOnly) "Outline" else "Filled",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (selectedLayer.strokeOnly) CyanPrecision else TextPrimary
                                    )
                                }
                            }
                        }

                        BlendLayerKind.GALLERY_SHAPE -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(7.dp))
                                        .background(StudioElevated)
                                        .border(1.dp, EmeraldSuccess.copy(alpha = 0.6f), RoundedCornerShape(7.dp))
                                        .clickable { onImportShapeFromGallery(true) }
                                        .padding(horizontal = 9.dp, vertical = 5.dp)
                                        .testTag("replace_gallery_shape_button"),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.AddPhotoAlternate,
                                        contentDescription = "Replace gallery shape",
                                        tint = EmeraldSuccess,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Replace",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextPrimary
                                    )
                                }

                                GalleryShapeCutout.entries.forEach { cutout ->
                                    val sel = cutout == selectedLayer.galleryCutout
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(7.dp))
                                            .background(if (sel) StudioElevated else StudioPanel)
                                            .border(
                                                1.dp,
                                                if (sel) AmberWarm else StudioHairline,
                                                RoundedCornerShape(7.dp)
                                            )
                                            .clickable {
                                                onUpdateSelectedLayer(false) { it.copy(galleryCutout = cutout) }
                                            }
                                            .padding(horizontal = 9.dp, vertical = 5.dp)
                                            .testTag("cutout_${cutout.name.lowercase()}")
                                    ) {
                                        Text(
                                            text = cutout.label,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (sel) AmberWarm else TextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Blend Mode Selector
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        LayerBlendMode.entries.forEach { mode ->
                            val sel = mode == selectedLayer.blendMode
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(7.dp))
                                    .background(if (sel) CyanPrecision.copy(alpha = 0.2f) else StudioPanel)
                                    .border(
                                        1.dp,
                                        if (sel) CyanPrecision else StudioHairline,
                                        RoundedCornerShape(7.dp)
                                    )
                                    .clickable {
                                        onUpdateSelectedLayer(false) { it.copy(blendMode = mode) }
                                    }
                                    .padding(horizontal = 9.dp, vertical = 5.dp)
                                    .testTag("blend_mode_${mode.name.lowercase()}")
                            ) {
                                Text(
                                    text = mode.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (sel) CyanPrecision else TextSecondary
                                )
                            }
                        }
                    }

                    // Color Swatches (with Original Colors toggle for Gallery Shape)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Tint",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                        if (selectedLayer.kind == BlendLayerKind.GALLERY_SHAPE) {
                            val origColors = !selectedLayer.tintGalleryShape
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(7.dp))
                                    .background(if (origColors) CyanPrecision.copy(alpha = 0.2f) else StudioPanel)
                                    .border(
                                        1.dp,
                                        if (origColors) CyanPrecision else StudioHairline,
                                        RoundedCornerShape(7.dp)
                                    )
                                    .clickable {
                                        onUpdateSelectedLayer(false) {
                                            it.copy(tintGalleryShape = !it.tintGalleryShape)
                                        }
                                    }
                                    .padding(horizontal = 9.dp, vertical = 4.dp)
                                    .testTag("gallery_shape_tint_toggle")
                            ) {
                                Text(
                                    text = if (origColors) "Original Colors" else "Tinted",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (origColors) CyanPrecision else AmberWarm
                                )
                            }
                        }
                        blendColorSwatches.forEach { argb ->
                            val isActiveTint = selectedLayer.kind != BlendLayerKind.GALLERY_SHAPE || selectedLayer.tintGalleryShape
                            val sel = isActiveTint && argb == selectedLayer.colorArgb
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(Color(argb))
                                    .border(
                                        width = if (sel) 2.5.dp else 1.dp,
                                        color = if (sel) CyanPrecision else StudioHairline,
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        onUpdateSelectedLayer(false) {
                                            it.copy(
                                                colorArgb = argb,
                                                tintGalleryShape = if (it.kind == BlendLayerKind.GALLERY_SHAPE) true else it.tintGalleryShape
                                            )
                                        }
                                    }
                            )
                        }
                        Spacer(modifier = Modifier.size(4.dp))
                    }

                    // Opacity, Scale, Rotation Sliders
                    PrecisionSlider(
                        label = "Blend Opacity (drag layer on photo to move)",
                        value = selectedLayer.opacity * 100f,
                        valueRange = 5f..100f,
                        formattedValue = "${(selectedLayer.opacity * 100).toInt()}%",
                        onValueChange = { v -> onUpdateSelectedLayer(true) { it.copy(opacity = v / 100f) } },
                        onValueChangeFinished = onEndContinuous,
                        onResetToDefault = { onUpdateSelectedLayer(false) { it.copy(opacity = 0.85f) } }
                    )
                    PrecisionSlider(
                        label = "Layer Scale",
                        value = selectedLayer.scale * 100f,
                        valueRange = 25f..280f,
                        formattedValue = "${(selectedLayer.scale * 100).toInt()}%",
                        onValueChange = { v -> onUpdateSelectedLayer(true) { it.copy(scale = v / 100f) } },
                        onValueChangeFinished = onEndContinuous,
                        onResetToDefault = { onUpdateSelectedLayer(false) { it.copy(scale = 1f) } },
                        accentColor = AmberWarm
                    )
                    PrecisionSlider(
                        label = "Rotation",
                        value = selectedLayer.rotationDeg,
                        valueRange = -180f..180f,
                        formattedValue = String.format("%+.0f°", selectedLayer.rotationDeg),
                        onValueChange = { v -> onUpdateSelectedLayer(true) { it.copy(rotationDeg = v) } },
                        onValueChangeFinished = onEndContinuous,
                        onResetToDefault = { onUpdateSelectedLayer(false) { it.copy(rotationDeg = 0f) } }
                    )
                }
            }
        }
    }
}
