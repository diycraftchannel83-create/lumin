package com.example.model

import androidx.compose.runtime.Immutable

enum class EditorToolTab(val label: String) {
    FILTERS("Filters"),
    CROP("Crop"),
    ADJUST("Adjust"),
    CURVES("Curves"),
    MASKING("Masking"),
    BLEND("Blend"),
    AI_STUDIO("AI Studio")
}

enum class AdjustSubTab(val label: String) {
    LIGHT("Light"),
    COLOR("Color"),
    DETAIL("Effects")
}

@Immutable
data class PhotoAdjustments(
    // Light
    val exposure: Float = 0f,      // -2.0f .. +2.0f EV
    val contrast: Float = 0f,      // -100f .. +100f
    val highlights: Float = 0f,    // -100f .. +100f
    val shadows: Float = 0f,       // -100f .. +100f
    val whites: Float = 0f,        // -100f .. +100f
    val blacks: Float = 0f,        // -100f .. +100f
    // Color
    val temperature: Float = 0f,   // -100f .. +100f
    val tint: Float = 0f,          // -100f .. +100f
    val vibrance: Float = 0f,      // -100f .. +100f
    val saturation: Float = 0f,    // -100f .. +100f
    // Detail & Optics
    val clarity: Float = 0f,       // 0f .. +100f
    val vignette: Float = 0f,      // -100f .. +100f
    val grain: Float = 0f          // 0f .. +100f
) {
    val isDefault: Boolean
        get() = exposure == 0f && contrast == 0f && highlights == 0f && shadows == 0f &&
            whites == 0f && blacks == 0f && temperature == 0f && tint == 0f &&
            vibrance == 0f && saturation == 0f && clarity == 0f && vignette == 0f && grain == 0f
}

enum class CurveChannel(val label: String) {
    RGB("RGB"),
    RED("Red"),
    GREEN("Green"),
    BLUE("Blue")
}

@Immutable
data class CurvePoint(
    val x: Float, // 0f..1f input
    val y: Float  // 0f..1f output
)

@Immutable
data class ToneCurvesState(
    val activeChannel: CurveChannel = CurveChannel.RGB,
    val rgbPoints: List<CurvePoint> = defaultCurvePoints(),
    val redPoints: List<CurvePoint> = defaultCurvePoints(),
    val greenPoints: List<CurvePoint> = defaultCurvePoints(),
    val bluePoints: List<CurvePoint> = defaultCurvePoints()
) {
    fun pointsFor(channel: CurveChannel): List<CurvePoint> = when (channel) {
        CurveChannel.RGB -> rgbPoints
        CurveChannel.RED -> redPoints
        CurveChannel.GREEN -> greenPoints
        CurveChannel.BLUE -> bluePoints
    }

    fun withPoints(channel: CurveChannel, points: List<CurvePoint>): ToneCurvesState = when (channel) {
        CurveChannel.RGB -> copy(rgbPoints = points)
        CurveChannel.RED -> copy(redPoints = points)
        CurveChannel.GREEN -> copy(greenPoints = points)
        CurveChannel.BLUE -> copy(bluePoints = points)
    }

    val isLinear: Boolean
        get() = isPointsLinear(rgbPoints) && isPointsLinear(redPoints) &&
            isPointsLinear(greenPoints) && isPointsLinear(bluePoints)

    companion object {
        fun defaultCurvePoints(): List<CurvePoint> = listOf(
            CurvePoint(0f, 0f),
            CurvePoint(1f, 1f)
        )

        private fun isPointsLinear(pts: List<CurvePoint>): Boolean {
            return pts.size == 2 &&
                pts[0].x == 0f && pts[0].y == 0f &&
                pts[1].x == 1f && pts[1].y == 1f
        }
    }
}

enum class AspectRatioPreset(val label: String, val ratio: Float?) {
    FREE("Free", null),
    ORIGINAL("Original", -1f),
    SQUARE_1_1("1:1", 1f),
    PORTRAIT_4_5("4:5", 4f / 5f),
    CLASSIC_3_2("3:2", 3f / 2f),
    WIDE_16_9("16:9", 16f / 9f),
    STORY_9_16("9:16", 9f / 16f)
}

@Immutable
data class NormalizedRect(
    val left: Float = 0f,
    val top: Float = 0f,
    val right: Float = 1f,
    val bottom: Float = 1f
) {
    val width: Float get() = (right - left).coerceAtLeast(0.05f)
    val height: Float get() = (bottom - top).coerceAtLeast(0.05f)
    val isFull: Boolean
        get() = left <= 0.002f && top <= 0.002f && right >= 0.998f && bottom >= 0.998f
}

@Immutable
data class CropGeometryState(
    val cropRect: NormalizedRect = NormalizedRect(),
    val rotationQuarterTurns: Int = 0, // 0, 1, 2, 3 (90 deg steps)
    val straightenDegrees: Float = 0f, // -45f..45f
    val flipHorizontal: Boolean = false,
    val flipVertical: Boolean = false,
    val aspectPreset: AspectRatioPreset = AspectRatioPreset.FREE
) {
    val isDefault: Boolean
        get() = cropRect.isFull && rotationQuarterTurns == 0 &&
            straightenDegrees == 0f && !flipHorizontal && !flipVertical
}

@Immutable
data class FilterPreset(
    val id: String,
    val name: String,
    val category: String,
    val exposureDelta: Float = 0f,
    val contrastDelta: Float = 0f,
    val saturationDelta: Float = 0f,
    val temperatureDelta: Float = 0f,
    val tintDelta: Float = 0f,
    val fadeBlacks: Float = 0f,       // 0f..0.25f lifts blacks for film matte
    val shadowTintR: Float = 0f,      // split tone shadows (-0.15..0.15)
    val shadowTintG: Float = 0f,
    val shadowTintB: Float = 0f,
    val highlightTintR: Float = 0f,   // split tone highlights (-0.15..0.15)
    val highlightTintG: Float = 0f,
    val highlightTintB: Float = 0f,
    val isMonochrome: Boolean = false
)

enum class MaskType(val label: String) {
    RADIAL("Radial"),
    LINEAR("Linear"),
    BRUSH("Brush")
}

@Immutable
data class BrushPoint(
    val x: Float, // normalized 0..1
    val y: Float  // normalized 0..1
)

@Immutable
data class BrushStroke(
    val points: List<BrushPoint>,
    val radius: Float = 0.055f // normalized relative to min image dimension
)

@Immutable
data class MaskAdjustments(
    val exposure: Float = 0f,     // -2f..2f
    val contrast: Float = 0f,     // -100f..100f
    val saturation: Float = 0f,   // -100f..100f
    val temperature: Float = 0f,  // -100f..100f
    val clarity: Float = 0f       // 0f..100f
)

@Immutable
data class MaskLayer(
    val id: String,
    val name: String,
    val type: MaskType,
    val enabled: Boolean = true,
    val inverted: Boolean = false,
    val feather: Float = 0.45f,   // 0.05f..1.0f
    // Radial geometry (normalized 0..1)
    val centerX: Float = 0.5f,
    val centerY: Float = 0.5f,
    val radiusX: Float = 0.30f,
    val radiusY: Float = 0.30f,
    // Linear gradient geometry (normalized 0..1)
    val startX: Float = 0.5f,
    val startY: Float = 0.15f,
    val endX: Float = 0.5f,
    val endY: Float = 0.75f,
    // Brush strokes
    val strokes: List<BrushStroke> = emptyList(),
    val adjustments: MaskAdjustments = MaskAdjustments(exposure = 0.35f)
)

enum class BackgroundReplacement(val label: String) {
    TRANSPARENT("Transparent"),
    STUDIO_DARK("Studio Dark"),
    PURE_WHITE("Pure White"),
    BOKEH_BLUR("Bokeh Blur")
}

enum class AiToolMode(val label: String) {
    OBJECT_ERASER("Object Eraser"),
    BACKGROUND_REMOVER("BG Remover")
}

@Immutable
data class AiStudioState(
    val activeMode: AiToolMode = AiToolMode.OBJECT_ERASER,
    val eraserBrushSize: Float = 0.05f,
    val eraserStrokes: List<BrushStroke> = emptyList(),
    val hasObjectRemovalApplied: Boolean = false,
    val hasBackgroundRemoved: Boolean = false,
    val backgroundStyle: BackgroundReplacement = BackgroundReplacement.TRANSPARENT,
    val noticeBanner: String? = null
)

enum class BlendLayerKind {
    TEXT,
    SHAPE,
    GALLERY_SHAPE
}

enum class GalleryShapeCutout(val label: String) {
    ORIGINAL("Original"),
    DROP_WHITE("Remove White"),
    DROP_BLACK("Remove Black")
}

enum class LayerBlendMode(val label: String) {
    NORMAL("Normal"),
    OVERLAY("Overlay"),
    MULTIPLY("Multiply"),
    SCREEN("Screen"),
    SOFT_LIGHT("Soft Light"),
    DARKEN("Darken"),
    LIGHTEN("Lighten"),
    DIFFERENCE("Difference")
}

enum class BlendFontStyle(val label: String) {
    DISPLAY("Display"),
    MODERN("Modern"),
    MONO("Mono"),
    EDITORIAL("Editorial")
}

enum class BlendShapeType(val label: String) {
    RING("Ring"),
    DISC("Disc"),
    STAR("Burst Star"),
    DIAMOND("Diamond"),
    ARCH("Arch"),
    PRISM("Prism"),
    FRAME("Frame"),
    STRIPES("Stripes")
}

@Immutable
data class BlendLayer(
    val id: String,
    val name: String,
    val kind: BlendLayerKind,
    val visible: Boolean = true,
    val blendMode: LayerBlendMode = LayerBlendMode.OVERLAY,
    val opacity: Float = 0.85f,
    val colorArgb: Int = 0xFFFFFFFF.toInt(),
    val posX: Float = 0.5f,
    val posY: Float = 0.5f,
    val scale: Float = 1.0f,
    val rotationDeg: Float = 0f,
    // Text properties
    val text: String = "LUMINA",
    val fontStyle: BlendFontStyle = BlendFontStyle.DISPLAY,
    // Shape properties
    val shapeType: BlendShapeType = BlendShapeType.RING,
    val strokeOnly: Boolean = false,
    // Imported gallery shape properties
    val galleryAssetId: String? = null,
    val galleryShapeLabel: String = "Gallery Shape",
    val galleryCutout: GalleryShapeCutout = GalleryShapeCutout.ORIGINAL,
    val tintGalleryShape: Boolean = false
)

@Immutable
data class EditorStateSnapshot(
    val adjustments: PhotoAdjustments = PhotoAdjustments(),
    val curves: ToneCurvesState = ToneCurvesState(),
    val crop: CropGeometryState = CropGeometryState(),
    val selectedPresetId: String = "original",
    val filterIntensity: Float = 1f,
    val masks: List<MaskLayer> = emptyList(),
    val selectedMaskId: String? = null,
    val blendLayers: List<BlendLayer> = emptyList(),
    val selectedBlendLayerId: String? = null
)

@Immutable
data class HistogramData(
    val luma: FloatArray = FloatArray(64),
    val red: FloatArray = FloatArray(64),
    val green: FloatArray = FloatArray(64),
    val blue: FloatArray = FloatArray(64)
)

@Immutable
data class ImageMetadataInfo(
    val title: String,
    val width: Int,
    val height: Int,
    val aspectLabel: String,
    val megapixels: String
)

enum class ExportFormat(
    val label: String,
    val extension: String,
    val mimeType: String,
    val badge: String,
    val description: String
) {
    PNG_LOSSLESS(
        label = "PNG Lossless",
        extension = "png",
        mimeType = "image/png",
        badge = "LOSSLESS • ALPHA",
        description = "Bit-exact lossless compression with full alpha transparency support"
    ),
    TIFF_UNCOMPRESSED(
        label = "TIFF Uncompressed",
        extension = "tif",
        mimeType = "image/tiff",
        badge = "RAW ARCHIVAL",
        description = "Zero compression baseline TIFF for maximum archival & print fidelity"
    ),
    WEBP_LOSSLESS(
        label = "WebP Lossless",
        extension = "webp",
        mimeType = "image/webp",
        badge = "LOSSLESS • COMPACT",
        description = "100% lossless archival encoding with smaller file footprint"
    ),
    JPEG_MAX(
        label = "JPEG 100%",
        extension = "jpg",
        mimeType = "image/jpeg",
        badge = "MAX QUALITY",
        description = "Maximum quality 100% JPEG for universal sharing & web delivery"
    )
}

enum class ExportResolution(val label: String, val maxLongEdge: Int?) {
    ORIGINAL("Original", null),
    UHD_4K("4K (3840px)", 3840),
    QHD_2K("2K (2048px)", 2048),
    FHD_1080("1080p (1920px)", 1920)
}
