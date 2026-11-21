package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.ai.AiImageRepository
import com.example.ai.FirebaseAiImageRepository
import com.example.ai.RealEsrganModelCatalog
import com.example.ai.RealEsrganModelManager
import com.example.ai.RealEsrganNative
import com.example.engine.ExportResult
import com.example.engine.ImageProcessor
import com.example.engine.LocalEnhancementEngine
import com.example.engine.TiffAndExportWriter
import com.example.model.AdjustSubTab
import com.example.model.AiStudioState
import com.example.model.AiToolMode
import com.example.model.AspectRatioPreset
import com.example.model.BackgroundReplacement
import com.example.model.BlendLayer
import com.example.model.BlendLayerKind
import com.example.model.BlendShapeType
import com.example.model.BrushPoint
import com.example.model.BrushStroke
import com.example.model.CropGeometryState
import com.example.model.LayerBlendMode
import com.example.model.CurveChannel
import com.example.model.CurvePoint
import com.example.model.EditorStateSnapshot
import com.example.model.EditorToolTab
import com.example.model.ExportFormat
import com.example.model.ExportResolution
import com.example.model.HistogramData
import com.example.model.ImageMetadataInfo
import com.example.model.MaskAdjustments
import com.example.model.MaskLayer
import com.example.model.MaskType
import com.example.model.NormalizedRect
import com.example.model.PhotoAdjustments
import com.example.model.ToneCurvesState
import com.google.firebase.ai.type.QuotaExceededException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.math.max
import kotlin.math.roundToInt

private const val TAG = "PhotoEditorViewModel"

sealed interface AiOperationUiState {
    data object Idle : AiOperationUiState
    data class Loading(val statusText: String) : AiOperationUiState
    data class Success(val message: String) : AiOperationUiState
    data class Error(val message: String) : AiOperationUiState
}

sealed interface ExportUiState {
    data object Idle : ExportUiState
    data object Exporting : ExportUiState
    data class Success(val result: ExportResult) : ExportUiState
    data class Error(val message: String) : ExportUiState
}

data class EditorScreenUiState(
    val activeTab: EditorToolTab = EditorToolTab.ADJUST,
    val adjustSubTab: AdjustSubTab = AdjustSubTab.LIGHT,
    val snapshot: EditorStateSnapshot = EditorStateSnapshot(),
    val aiState: AiStudioState = AiStudioState(),
    val aiOperationState: AiOperationUiState = AiOperationUiState.Idle,
    val exportUiState: ExportUiState = ExportUiState.Idle,
    val processedPreview: Bitmap? = null,
    val originalPreview: Bitmap? = null,
    val filterThumbnails: Map<String, Bitmap> = emptyMap(),
    val histogram: HistogramData = HistogramData(),
    val metadata: ImageMetadataInfo = ImageMetadataInfo("Mountain Fjord", 0, 0, "4:3", "0.0 MP"),
    val showHistogram: Boolean = true,
    val showMaskOverlay: Boolean = true,
    val isComparing: Boolean = false,
    val isRenderingPreview: Boolean = false,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val canRevertAiBase: Boolean = false
)

class PhotoEditorViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val aiRepository: AiImageRepository = FirebaseAiImageRepository()

    private val _uiState = MutableStateFlow(EditorScreenUiState())
    val uiState: StateFlow<EditorScreenUiState> = _uiState.asStateFlow()

    private var originalLoadedBitmap: Bitmap? = null
    private var workingBaseFullRes: Bitmap? = null
    private var workingBasePreview: Bitmap? = null
    private var sourceLoadJob: Job? = null
    private var sourceLoadGeneration = 0L
    private val galleryShapeBitmaps = LinkedHashMap<String, Bitmap>()

    private val undoStack = ArrayDeque<EditorStateSnapshot>()
    private val redoStack = ArrayDeque<EditorStateSnapshot>()
    private var isInContinuousGesture = false

    private var renderJob: Job? = null
    private var previewRenderGeneration = 0L
    private var thumbsJob: Job? = null
    private var thumbnailGeneration = 0L

    // Start in the branded empty workspace. Sample images remain available from the sample picker.
    // Users should choose their own image instead of having a sample auto-loaded on launch.

    fun loadSampleDrawable(@DrawableRes resId: Int, title: String) {
        val generation = ++sourceLoadGeneration
        sourceLoadJob?.cancel()
        sourceLoadJob = viewModelScope.launch {
            val bmp = withContext(Dispatchers.IO) {
                val opts = BitmapFactory.Options().apply {
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                BitmapFactory.decodeResource(getApplication<Application>().resources, resId, opts)
            }
            if (generation == sourceLoadGeneration && bmp != null) {
                setNewSourceBitmap(bmp, title)
            }
        }
    }

    fun loadPhotoFromUri(uri: Uri, title: String = "Imported Photo") {
        val generation = ++sourceLoadGeneration
        sourceLoadJob?.cancel()
        sourceLoadJob = viewModelScope.launch {
            try {
                val decoded = withContext(Dispatchers.IO) {
                    decodeAndOrientUri(uri)
                }
                if (generation != sourceLoadGeneration) return@launch
                if (decoded != null) {
                    setNewSourceBitmap(decoded, title)
                } else {
                    _uiState.update {
                        it.copy(aiOperationState = AiOperationUiState.Error("Could not decode selected image."))
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (generation != sourceLoadGeneration) return@launch
                Log.e(TAG, "Failed to load image URI", e)
                _uiState.update {
                    it.copy(aiOperationState = AiOperationUiState.Error(e.localizedMessage ?: "Failed to load photo."))
                }
            }
        }
    }

    private fun decodeAndOrientUri(uri: Uri, maxAllowed: Int = 4096): Bitmap? {
        val resolver = getApplication<Application>().contentResolver
        val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, boundsOpts) }

        var sampleSize = 1
        while (boundsOpts.outWidth / sampleSize > maxAllowed || boundsOpts.outHeight / sampleSize > maxAllowed) {
            sampleSize *= 2
        }

        val decodeOpts = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val raw = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, decodeOpts)
        } ?: return null

        val orientation = try {
            resolver.openInputStream(uri)?.use { stream ->
                ExifInterface(stream).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            } ?: ExifInterface.ORIENTATION_NORMAL
        } catch (_: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        }

        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            // EXIF transpose/transverse orientations combine a rotation and a mirror.
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.postScale(-1f, 1f)
            }
            else -> return raw
        }
        val oriented = Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, matrix, true)
        if (oriented !== raw) raw.recycle()
        return oriented
    }

    private fun setNewSourceBitmap(bitmap: Bitmap, title: String) {
        originalLoadedBitmap = bitmap
        workingBaseFullRes = bitmap
        workingBasePreview = ImageProcessor.downsampleIfNeeded(bitmap, 1024)
        galleryShapeBitmaps.clear()
        undoStack.clear()
        redoStack.clear()

        val meta = buildMetadata(title, bitmap)
        val freshSnapshot = EditorStateSnapshot()
        _uiState.update {
            it.copy(
                snapshot = freshSnapshot,
                aiState = AiStudioState(),
                aiOperationState = AiOperationUiState.Idle,
                exportUiState = ExportUiState.Idle,
                metadata = meta,
                canUndo = false,
                canRedo = false,
                canRevertAiBase = false
            )
        }
        refreshFilterThumbnails()
        schedulePreviewRender(immediate = true)
    }

    private fun buildMetadata(title: String, bmp: Bitmap): ImageMetadataInfo {
        val w = bmp.width
        val h = bmp.height
        val gcdVal = gcd(w, h).coerceAtLeast(1)
        val rw = w / gcdVal
        val rh = h / gcdVal
        val aspect = if (rw <= 20 && rh <= 20) "$rw:$rh" else String.format("%.2f:1", w.toFloat() / max(1, h))
        val mp = String.format("%.1f MP", (w * h) / 1_000_000f)
        return ImageMetadataInfo(title, w, h, aspect, mp)
    }

    private fun gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)

    private fun refreshFilterThumbnails() {
        val base = workingBasePreview ?: return
        val generation = ++thumbnailGeneration
        thumbsJob?.cancel()
        thumbsJob = viewModelScope.launch(Dispatchers.Default) {
            val thumbs = ImageProcessor.renderFilterThumbnails(base)
            // Thumbnail rendering can be synchronous and finish after cancellation; ignore stale work.
            if (generation == thumbnailGeneration) {
                _uiState.update { it.copy(filterThumbnails = thumbs) }
            }
        }
    }

    // --- Navigation & HUD ---

    fun selectToolTab(tab: EditorToolTab) {
        _uiState.update { it.copy(activeTab = tab) }
        schedulePreviewRender(immediate = true)
    }

    fun selectAdjustSubTab(subTab: AdjustSubTab) {
        _uiState.update { it.copy(adjustSubTab = subTab) }
    }

    fun toggleHistogram() {
        _uiState.update { it.copy(showHistogram = !it.showHistogram) }
    }

    fun toggleMaskOverlay() {
        _uiState.update { it.copy(showMaskOverlay = !it.showMaskOverlay) }
        schedulePreviewRender(immediate = true)
    }

    fun setComparing(comparing: Boolean) {
        _uiState.update { it.copy(isComparing = comparing) }
    }

    // --- History (Undo / Redo / Reset) ---

    fun beginContinuousEdit() {
        if (!isInContinuousGesture) {
            pushUndoSnapshot(_uiState.value.snapshot)
            isInContinuousGesture = true
        }
    }

    fun endContinuousEdit() {
        isInContinuousGesture = false
    }

    private fun pushUndoSnapshot(snap: EditorStateSnapshot) {
        if (undoStack.lastOrNull() == snap) return
        if (undoStack.size >= 25) undoStack.removeFirst()
        undoStack.addLast(snap)
        redoStack.clear()
    }

    private fun applySnapshot(newSnap: EditorStateSnapshot, recordHistory: Boolean = true, immediateRender: Boolean = false) {
        if (recordHistory) {
            pushUndoSnapshot(_uiState.value.snapshot)
        }
        _uiState.update {
            it.copy(
                snapshot = newSnap,
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty()
            )
        }
        schedulePreviewRender(immediate = immediateRender)
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        val current = _uiState.value.snapshot
        val prev = undoStack.removeLast()
        redoStack.addLast(current)
        _uiState.update {
            it.copy(
                snapshot = prev,
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty()
            )
        }
        schedulePreviewRender(immediate = true)
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        val current = _uiState.value.snapshot
        val next = redoStack.removeLast()
        undoStack.addLast(current)
        _uiState.update {
            it.copy(
                snapshot = next,
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty()
            )
        }
        schedulePreviewRender(immediate = true)
    }

    fun resetAllEdits() {
        val original = originalLoadedBitmap ?: return
        workingBaseFullRes = original
        workingBasePreview = ImageProcessor.downsampleIfNeeded(original, 1024)
        pushUndoSnapshot(_uiState.value.snapshot)
        _uiState.update {
            it.copy(
                snapshot = EditorStateSnapshot(),
                aiState = AiStudioState(),
                aiOperationState = AiOperationUiState.Idle,
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty(),
                canRevertAiBase = false
            )
        }
        refreshFilterThumbnails()
        schedulePreviewRender(immediate = true)
    }

    // --- Filters ---

    fun selectPreset(presetId: String) {
        val cur = _uiState.value.snapshot
        applySnapshot(cur.copy(selectedPresetId = presetId), recordHistory = true, immediateRender = true)
    }

    fun updateFilterIntensity(intensity: Float, continuous: Boolean = true) {
        if (continuous) beginContinuousEdit()
        val cur = _uiState.value.snapshot
        applySnapshot(cur.copy(filterIntensity = intensity.coerceIn(0f, 1f)), recordHistory = !continuous)
    }

    // --- Light & Color Adjustments ---

    fun updateAdjustments(continuous: Boolean = true, transform: (PhotoAdjustments) -> PhotoAdjustments) {
        if (continuous) beginContinuousEdit()
        val cur = _uiState.value.snapshot
        applySnapshot(cur.copy(adjustments = transform(cur.adjustments)), recordHistory = !continuous)
    }

    // --- Tone Curves ---

    fun selectCurveChannel(channel: CurveChannel) {
        val cur = _uiState.value.snapshot
        _uiState.update {
            it.copy(snapshot = cur.copy(curves = cur.curves.copy(activeChannel = channel)))
        }
    }

    fun updateActiveCurvePoints(points: List<CurvePoint>, continuous: Boolean = true) {
        if (continuous) beginContinuousEdit()
        val cur = _uiState.value.snapshot
        val updatedCurves = cur.curves.withPoints(cur.curves.activeChannel, points)
        applySnapshot(cur.copy(curves = updatedCurves), recordHistory = !continuous)
    }

    fun applyCurvePreset(points: List<CurvePoint>) {
        val cur = _uiState.value.snapshot
        val updatedCurves = cur.curves.withPoints(cur.curves.activeChannel, points)
        applySnapshot(cur.copy(curves = updatedCurves), recordHistory = true, immediateRender = true)
    }

    fun resetActiveCurveChannel() {
        applyCurvePreset(ToneCurvesState.defaultCurvePoints())
    }

    // --- Crop & Geometry ---

    fun updateCropRect(rect: NormalizedRect, continuous: Boolean = true) {
        if (continuous) beginContinuousEdit()
        val cur = _uiState.value.snapshot
        applySnapshot(cur.copy(crop = cur.crop.copy(cropRect = rect)), recordHistory = !continuous)
    }

    fun selectAspectPreset(preset: AspectRatioPreset) {
        val base = workingBasePreview ?: return
        val cur = _uiState.value.snapshot
        val rotatedTurns = cur.crop.rotationQuarterTurns % 2
        val baseW = if (rotatedTurns == 1) base.height.toFloat() else base.width.toFloat()
        val baseH = if (rotatedTurns == 1) base.width.toFloat() else base.height.toFloat()
        val imgAspect = baseW / max(1f, baseH)

        val targetRatio = when (preset) {
            AspectRatioPreset.FREE -> null
            AspectRatioPreset.ORIGINAL -> imgAspect
            else -> preset.ratio
        }

        val newRect = if (targetRatio == null) {
            cur.crop.cropRect
        } else {
            val relRatio = targetRatio / imgAspect
            if (relRatio >= 1f) {
                val h = (1f / relRatio).coerceIn(0.1f, 1f)
                val pad = (1f - h) / 2f
                NormalizedRect(0f, pad, 1f, 1f - pad)
            } else {
                val w = relRatio.coerceIn(0.1f, 1f)
                val pad = (1f - w) / 2f
                NormalizedRect(pad, 0f, 1f - pad, 1f)
            }
        }
        applySnapshot(
            cur.copy(crop = cur.crop.copy(aspectPreset = preset, cropRect = newRect)),
            recordHistory = true,
            immediateRender = true
        )
    }

    fun rotateQuarterTurn(delta: Int) {
        val cur = _uiState.value.snapshot
        val next = ((cur.crop.rotationQuarterTurns + delta) % 4 + 4) % 4
        applySnapshot(
            cur.copy(crop = cur.crop.copy(rotationQuarterTurns = next)),
            recordHistory = true,
            immediateRender = true
        )
    }

    fun toggleFlipHorizontal() {
        val cur = _uiState.value.snapshot
        applySnapshot(
            cur.copy(crop = cur.crop.copy(flipHorizontal = !cur.crop.flipHorizontal)),
            recordHistory = true,
            immediateRender = true
        )
    }

    fun toggleFlipVertical() {
        val cur = _uiState.value.snapshot
        applySnapshot(
            cur.copy(crop = cur.crop.copy(flipVertical = !cur.crop.flipVertical)),
            recordHistory = true,
            immediateRender = true
        )
    }

    fun updateStraighten(degrees: Float, continuous: Boolean = true) {
        if (continuous) beginContinuousEdit()
        val cur = _uiState.value.snapshot
        applySnapshot(
            cur.copy(crop = cur.crop.copy(straightenDegrees = degrees.coerceIn(-45f, 45f))),
            recordHistory = !continuous
        )
    }

    fun resetCrop() {
        val cur = _uiState.value.snapshot
        applySnapshot(cur.copy(crop = CropGeometryState()), recordHistory = true, immediateRender = true)
    }

    // --- Masking ---

    fun addMask(type: MaskType) {
        val cur = _uiState.value.snapshot
        val count = cur.masks.count { it.type == type } + 1
        val newMask = MaskLayer(
            id = UUID.randomUUID().toString(),
            name = "${type.label} $count",
            type = type,
            adjustments = MaskAdjustments(exposure = 0.40f)
        )
        applySnapshot(
            cur.copy(masks = cur.masks + newMask, selectedMaskId = newMask.id),
            recordHistory = true,
            immediateRender = true
        )
    }

    fun selectMask(maskId: String) {
        val cur = _uiState.value.snapshot
        _uiState.update { it.copy(snapshot = cur.copy(selectedMaskId = maskId)) }
        schedulePreviewRender(immediate = true)
    }

    fun deleteMask(maskId: String) {
        val cur = _uiState.value.snapshot
        val remaining = cur.masks.filterNot { it.id == maskId }
        val nextSelected = if (cur.selectedMaskId == maskId) remaining.lastOrNull()?.id else cur.selectedMaskId
        applySnapshot(
            cur.copy(masks = remaining, selectedMaskId = nextSelected),
            recordHistory = true,
            immediateRender = true
        )
    }

    fun updateSelectedMask(continuous: Boolean = true, transform: (MaskLayer) -> MaskLayer) {
        val cur = _uiState.value.snapshot
        val targetId = cur.selectedMaskId ?: return
        if (continuous) beginContinuousEdit()
        val updated = cur.masks.map { if (it.id == targetId) transform(it) else it }
        applySnapshot(cur.copy(masks = updated), recordHistory = !continuous)
    }

    fun addBrushPointToSelectedMask(point: BrushPoint, isNewStroke: Boolean, radius: Float = 0.065f) {
        val cur = _uiState.value.snapshot
        val targetId = cur.selectedMaskId ?: return
        val target = cur.masks.find { it.id == targetId } ?: return
        if (target.type != MaskType.BRUSH) return
        if (isNewStroke) beginContinuousEdit()

        val updatedStrokes = if (isNewStroke || target.strokes.isEmpty()) {
            target.strokes + BrushStroke(points = listOf(point), radius = radius)
        } else {
            val last = target.strokes.last()
            target.strokes.dropLast(1) + last.copy(points = last.points + point)
        }
        val updatedMasks = cur.masks.map {
            if (it.id == targetId) it.copy(strokes = updatedStrokes) else it
        }
        applySnapshot(cur.copy(masks = updatedMasks), recordHistory = false)
    }

    // --- Blend Layers (Text & Shape Image Blending) ---

    fun addBlendLayer(kind: BlendLayerKind) {
        val cur = _uiState.value.snapshot
        val count = cur.blendLayers.count { it.kind == kind } + 1
        val newLayer = if (kind == BlendLayerKind.TEXT) {
            BlendLayer(
                id = UUID.randomUUID().toString(),
                name = "Text $count",
                kind = BlendLayerKind.TEXT,
                text = "LUMINA",
                blendMode = LayerBlendMode.OVERLAY,
                opacity = 0.88f
            )
        } else {
            BlendLayer(
                id = UUID.randomUUID().toString(),
                name = "Shape $count",
                kind = BlendLayerKind.SHAPE,
                shapeType = BlendShapeType.RING,
                blendMode = LayerBlendMode.SCREEN,
                opacity = 0.85f
            )
        }
        applySnapshot(
            cur.copy(
                blendLayers = cur.blendLayers + newLayer,
                selectedBlendLayerId = newLayer.id
            ),
            recordHistory = true,
            immediateRender = true
        )
    }

    fun addGalleryShapeBlendLayer(uri: Uri, replaceSelected: Boolean = false) {
        viewModelScope.launch {
            try {
                val decodedAndName = withContext(Dispatchers.IO) {
                    val bmp = decodeAndOrientUri(uri, maxAllowed = 1536) ?: return@withContext null
                    val label = resolveUriDisplayLabel(uri)
                    bmp to label
                }
                if (decodedAndName == null) {
                    _uiState.update {
                        it.copy(aiOperationState = AiOperationUiState.Error("Could not decode shape image from gallery."))
                    }
                    return@launch
                }

                val (decoded, label) = decodedAndName
                val assetId = UUID.randomUUID().toString()
                galleryShapeBitmaps[assetId] = decoded

                val cur = _uiState.value.snapshot
                val selectedLayer = cur.blendLayers.find { it.id == cur.selectedBlendLayerId }
                if (replaceSelected && selectedLayer != null && selectedLayer.kind == BlendLayerKind.GALLERY_SHAPE) {
                    val updated = cur.blendLayers.map {
                        if (it.id == selectedLayer.id) {
                            it.copy(
                                galleryAssetId = assetId,
                                galleryShapeLabel = label ?: it.galleryShapeLabel
                            )
                        } else {
                            it
                        }
                    }
                    applySnapshot(
                        cur.copy(blendLayers = updated),
                        recordHistory = true,
                        immediateRender = true
                    )
                } else {
                    val count = cur.blendLayers.count { it.kind == BlendLayerKind.GALLERY_SHAPE } + 1
                    val newLayer = BlendLayer(
                        id = UUID.randomUUID().toString(),
                        name = "Import $count",
                        kind = BlendLayerKind.GALLERY_SHAPE,
                        blendMode = LayerBlendMode.NORMAL,
                        opacity = 0.92f,
                        galleryAssetId = assetId,
                        galleryShapeLabel = label ?: "Shape $count"
                    )
                    applySnapshot(
                        cur.copy(
                            blendLayers = cur.blendLayers + newLayer,
                            selectedBlendLayerId = newLayer.id
                        ),
                        recordHistory = true,
                        immediateRender = true
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to import gallery shape", e)
                _uiState.update {
                    it.copy(aiOperationState = AiOperationUiState.Error(e.localizedMessage ?: "Failed to import shape."))
                }
            }
        }
    }

    private fun resolveUriDisplayLabel(uri: Uri): String? {
        return try {
            val resolver = getApplication<Application>().contentResolver
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) {
                        val raw = cursor.getString(idx)
                        raw?.substringBeforeLast('.')?.takeIf { it.isNotBlank() }
                    } else null
                } else null
            }
        } catch (_: Exception) {
            null
        }
    }

    fun selectBlendLayer(layerId: String) {
        val cur = _uiState.value.snapshot
        _uiState.update { it.copy(snapshot = cur.copy(selectedBlendLayerId = layerId)) }
    }

    fun toggleBlendLayerVisibility(layerId: String) {
        val cur = _uiState.value.snapshot
        val updated = cur.blendLayers.map {
            if (it.id == layerId) it.copy(visible = !it.visible) else it
        }
        applySnapshot(cur.copy(blendLayers = updated), recordHistory = true, immediateRender = true)
    }

    fun deleteBlendLayer(layerId: String) {
        val cur = _uiState.value.snapshot
        val remaining = cur.blendLayers.filterNot { it.id == layerId }
        val nextSelected = if (cur.selectedBlendLayerId == layerId) remaining.lastOrNull()?.id else cur.selectedBlendLayerId
        applySnapshot(
            cur.copy(blendLayers = remaining, selectedBlendLayerId = nextSelected),
            recordHistory = true,
            immediateRender = true
        )
    }

    fun updateSelectedBlendLayer(continuous: Boolean = true, transform: (BlendLayer) -> BlendLayer) {
        val cur = _uiState.value.snapshot
        val targetId = cur.selectedBlendLayerId ?: return
        if (continuous) beginContinuousEdit()
        val updated = cur.blendLayers.map { if (it.id == targetId) transform(it) else it }
        applySnapshot(cur.copy(blendLayers = updated), recordHistory = !continuous)
    }

    fun moveSelectedBlendLayer(dxNorm: Float, dyNorm: Float) {
        updateSelectedBlendLayer(continuous = true) { layer ->
            layer.copy(
                posX = (layer.posX + dxNorm).coerceIn(0.02f, 0.98f),
                posY = (layer.posY + dyNorm).coerceIn(0.02f, 0.98f)
            )
        }
    }

    // --- AI Studio (Firebase AI Logic: Gemini Object Eraser & 1-Click BG Remover) ---

    fun selectAiMode(mode: AiToolMode) {
        _uiState.update { it.copy(aiState = it.aiState.copy(activeMode = mode)) }
    }

    fun updateAiBrushSize(size: Float) {
        _uiState.update { it.copy(aiState = it.aiState.copy(eraserBrushSize = size.coerceIn(0.02f, 0.14f))) }
    }

    fun addAiEraserPoint(point: BrushPoint, isNewStroke: Boolean) {
        _uiState.update { state ->
            val currentStrokes = state.aiState.eraserStrokes
            val radius = state.aiState.eraserBrushSize
            val nextStrokes = if (isNewStroke || currentStrokes.isEmpty()) {
                currentStrokes + BrushStroke(points = listOf(point), radius = radius)
            } else {
                val last = currentStrokes.last()
                currentStrokes.dropLast(1) + last.copy(points = last.points + point)
            }
            state.copy(aiState = state.aiState.copy(eraserStrokes = nextStrokes))
        }
    }

    fun clearAiEraserStrokes() {
        _uiState.update {
            it.copy(aiState = it.aiState.copy(eraserStrokes = emptyList()))
        }
    }

    fun runAiObjectRemoval() {
        val base = workingBaseFullRes ?: return
        val strokes = _uiState.value.aiState.eraserStrokes
        if (strokes.isEmpty()) {
            _uiState.update {
                it.copy(aiOperationState = AiOperationUiState.Error("Brush over the unwanted object on the photo first."))
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(aiOperationState = AiOperationUiState.Loading("Erasing object with Gemini AI..."))
            }
            try {
                val result = aiRepository.removeObject(base, strokes)
                workingBaseFullRes = result.bitmap
                workingBasePreview = ImageProcessor.downsampleIfNeeded(result.bitmap, 1024)
                _uiState.update {
                    it.copy(
                        aiState = it.aiState.copy(
                            eraserStrokes = emptyList(),
                            hasObjectRemovalApplied = true,
                            noticeBanner = result.notice
                        ),
                        aiOperationState = AiOperationUiState.Success(result.notice),
                        canRevertAiBase = true
                    )
                }
                refreshFilterThumbnails()
                schedulePreviewRender(immediate = true)
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "AI object removal timed out", e)
                _uiState.update {
                    it.copy(aiOperationState = AiOperationUiState.Error("AI object removal timed out after 60s. Please try again."))
                }
            } catch (e: QuotaExceededException) {
                Log.e(TAG, "Quota exceeded", e)
                _uiState.update {
                    it.copy(aiOperationState = AiOperationUiState.Error("AI quota exceeded. Please check Firebase billing or retry later."))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI object removal failed", e)
                _uiState.update {
                    it.copy(aiOperationState = AiOperationUiState.Error(e.localizedMessage ?: "AI object removal failed."))
                }
            }
        }
    }

    fun runLocalEnhance(sharpen: Float = 0.65f, denoise: Float = 0.25f, localContrast: Float = 0.35f) {
        val base = workingBaseFullRes ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(aiOperationState = AiOperationUiState.Loading("Enhancing locally — no upload or billing…")) }
            try {
                val result = withContext(Dispatchers.Default) { LocalEnhancementEngine.enhance(base, sharpen, denoise, localContrast) }
                workingBaseFullRes = result
                workingBasePreview = ImageProcessor.downsampleIfNeeded(result, 1024)
                _uiState.update { it.copy(aiState = it.aiState.copy(hasEnhancementApplied = true, noticeBanner = "Local enhancement applied"), aiOperationState = AiOperationUiState.Success("Offline enhancement complete. No internet/API/billing used."), canRevertAiBase = true) }
                refreshFilterThumbnails(); schedulePreviewRender(immediate = true)
            } catch (e: CancellationException) { throw e } catch (e: Exception) {
                Log.e(TAG, "Local enhance failed", e); _uiState.update { it.copy(aiOperationState = AiOperationUiState.Error(e.localizedMessage ?: "Local enhancement failed")) }
            }
        }
    }
    fun upscaleImage2xLocal() {
        val base = workingBaseFullRes ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(aiOperationState = AiOperationUiState.Loading("Upscaling 2× locally…")) }
            try {
                val result = withContext(Dispatchers.Default) { LocalEnhancementEngine.upscale2x(base) }
                workingBaseFullRes = result; workingBasePreview = ImageProcessor.downsampleIfNeeded(result, 1024)
                _uiState.update { it.copy(aiState = it.aiState.copy(hasEnhancementApplied = true, noticeBanner = "Local 2× upscale applied"), aiOperationState = AiOperationUiState.Success("2× upscale completed using interpolation."), canRevertAiBase = true) }
                refreshFilterThumbnails(); schedulePreviewRender(immediate = true)
            } catch (e: CancellationException) { throw e } catch (e: Exception) {
                Log.e(TAG, "Local upscale failed", e); _uiState.update { it.copy(aiOperationState = AiOperationUiState.Error(e.localizedMessage ?: "Upscale failed")) }
            }
        }
    }

    fun runRealEsrgan(modelId: String) {
        val base = workingBaseFullRes ?: return
        val spec = RealEsrganModelCatalog.find(modelId)
        if (spec == null) {
            _uiState.update { it.copy(aiOperationState = AiOperationUiState.Error("Unknown Real-ESRGAN model.")) }
            return
        }
        val manager = RealEsrganModelManager(getApplication())
        val installed = manager.getInstalled(spec)
        if (installed == null) {
            _uiState.update { it.copy(aiOperationState = AiOperationUiState.Error("Download and install the selected model first.")) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(aiOperationState = AiOperationUiState.Loading("Running on-device Real-ESRGAN ${spec.scale}×…")) }
            try {
                val result = RealEsrganNative.upscale(base, installed, tileSize = 256, useVulkan = true)
                workingBaseFullRes = result
                workingBasePreview = ImageProcessor.downsampleIfNeeded(result, 1024)
                _uiState.update {
                    it.copy(
                        aiState = it.aiState.copy(hasEnhancementApplied = true, noticeBanner = "Real-ESRGAN ${spec.displayName} applied"),
                        aiOperationState = AiOperationUiState.Success("Offline neural upscaling complete (${spec.scale}×)."),
                        canRevertAiBase = true
                    )
                }
                refreshFilterThumbnails()
                schedulePreviewRender(immediate = true)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Real-ESRGAN inference failed", e)
                _uiState.update { it.copy(aiOperationState = AiOperationUiState.Error(e.localizedMessage ?: "Real-ESRGAN processing failed.")) }
            }
        }
    }

    fun runAiEnhance() {
        val base = workingBaseFullRes ?: return
        viewModelScope.launch {
            _uiState.update {
                it.copy(aiOperationState = AiOperationUiState.Loading("Enhancing sharpness, clarity, and photo quality with AI..."))
            }
            try {
                val result = aiRepository.enhancePhoto(base)
                workingBaseFullRes = result.bitmap
                workingBasePreview = ImageProcessor.downsampleIfNeeded(result.bitmap, 1024)
                _uiState.update {
                    it.copy(
                        aiState = it.aiState.copy(
                            hasEnhancementApplied = true,
                            noticeBanner = result.notice
                        ),
                        aiOperationState = AiOperationUiState.Success(result.notice),
                        canRevertAiBase = true
                    )
                }
                refreshFilterThumbnails()
                schedulePreviewRender(immediate = true)
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "AI Enhance timed out", e)
                _uiState.update {
                    it.copy(aiOperationState = AiOperationUiState.Error("AI Enhance timed out after 90 seconds. Please try again."))
                }
            } catch (e: QuotaExceededException) {
                Log.e(TAG, "AI Enhance quota exceeded", e)
                _uiState.update {
                    it.copy(aiOperationState = AiOperationUiState.Error("AI quota exceeded. Please check Firebase billing or retry later."))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI Enhance failed", e)
                _uiState.update {
                    it.copy(aiOperationState = AiOperationUiState.Error(e.localizedMessage ?: "AI photo enhancement failed."))
                }
            }
        }
    }

    fun runOneClickBackgroundRemoval() {
        val base = workingBaseFullRes ?: return
        viewModelScope.launch {
            _uiState.update {
                it.copy(aiOperationState = AiOperationUiState.Loading("Extracting subject with Gemini AI..."))
            }
            try {
                val result = aiRepository.removeBackground(base)
                workingBaseFullRes = result.bitmap
                workingBasePreview = ImageProcessor.downsampleIfNeeded(result.bitmap, 1024)
                _uiState.update {
                    it.copy(
                        aiState = it.aiState.copy(
                            hasBackgroundRemoved = true,
                            noticeBanner = result.notice
                        ),
                        aiOperationState = AiOperationUiState.Success(result.notice),
                        canRevertAiBase = true
                    )
                }
                refreshFilterThumbnails()
                schedulePreviewRender(immediate = true)
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Background removal timed out", e)
                _uiState.update {
                    it.copy(aiOperationState = AiOperationUiState.Error("Background extraction timed out after 60s. Please try again."))
                }
            } catch (e: QuotaExceededException) {
                Log.e(TAG, "Quota exceeded", e)
                _uiState.update {
                    it.copy(aiOperationState = AiOperationUiState.Error("AI quota exceeded. Please check Firebase billing or retry later."))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Background removal failed", e)
                _uiState.update {
                    it.copy(aiOperationState = AiOperationUiState.Error(e.localizedMessage ?: "Background removal failed."))
                }
            }
        }
    }

    fun selectBackgroundReplacement(style: BackgroundReplacement) {
        _uiState.update { it.copy(aiState = it.aiState.copy(backgroundStyle = style)) }
        schedulePreviewRender(immediate = true)
    }

    fun revertAiEdits() {
        val orig = originalLoadedBitmap ?: return
        workingBaseFullRes = orig
        workingBasePreview = ImageProcessor.downsampleIfNeeded(orig, 1024)
        _uiState.update {
            it.copy(
                aiState = AiStudioState(activeMode = it.aiState.activeMode),
                aiOperationState = AiOperationUiState.Idle,
                canRevertAiBase = false
            )
        }
        refreshFilterThumbnails()
        schedulePreviewRender(immediate = true)
    }

    fun dismissAiMessage() {
        _uiState.update { it.copy(aiOperationState = AiOperationUiState.Idle) }
    }

    // --- Export ---

    fun exportHighRes(format: ExportFormat, resolution: ExportResolution) {
        val baseFull = workingBaseFullRes ?: return
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(exportUiState = ExportUiState.Exporting) }
            try {
                val renderedFull = withContext(Dispatchers.Default) {
                    ImageProcessor.renderProcessedBitmap(
                        baseSource = baseFull,
                        state = state.snapshot,
                        applyGeometryCrop = true,
                        hasBackgroundRemoved = state.aiState.hasBackgroundRemoved,
                        backgroundStyle = state.aiState.backgroundStyle,
                        previewMaskOverlayId = null,
                        context = getApplication(),
                        galleryShapeBitmaps = galleryShapeBitmaps.toMap()
                    )
                }
                val result = TiffAndExportWriter.exportBitmap(
                    context = getApplication(),
                    processedFullRes = renderedFull,
                    format = format,
                    resolution = resolution
                )
                if (renderedFull !== baseFull) renderedFull.recycle()
                _uiState.update { it.copy(exportUiState = ExportUiState.Success(result)) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Export failed", e)
                _uiState.update {
                    it.copy(exportUiState = ExportUiState.Error(e.localizedMessage ?: "Export failed."))
                }
            }
        }
    }

    fun resetExportState() {
        _uiState.update { it.copy(exportUiState = ExportUiState.Idle) }
    }

    // --- Preview Rendering Pipeline ---

    private fun schedulePreviewRender(immediate: Boolean) {
        val basePrev = workingBasePreview ?: return
        val origFull = originalLoadedBitmap ?: basePrev
        val generation = ++previewRenderGeneration
        renderJob?.cancel()
        renderJob = viewModelScope.launch(Dispatchers.Default) {
            if (!immediate) {
                delay(28)
            }
            _uiState.update { it.copy(isRenderingPreview = true) }

            val curState = _uiState.value
            val snap = curState.snapshot
            val showCropUnclipped = curState.activeTab == EditorToolTab.CROP
            val overlayMaskId = if (curState.activeTab == EditorToolTab.MASKING && curState.showMaskOverlay) {
                snap.selectedMaskId
            } else {
                null
            }

            val processed = ImageProcessor.renderProcessedBitmap(
                baseSource = basePrev,
                state = snap,
                applyGeometryCrop = !showCropUnclipped,
                hasBackgroundRemoved = curState.aiState.hasBackgroundRemoved,
                backgroundStyle = curState.aiState.backgroundStyle,
                previewMaskOverlayId = overlayMaskId,
                context = getApplication(),
                galleryShapeBitmaps = galleryShapeBitmaps.toMap(),
                applyExposureToPixels = false
            )

            // Also compute geometry-matched original preview for Before/After comparison
            val origPreviewBase = ImageProcessor.downsampleIfNeeded(origFull, 1024)
            val origMatched = if (!showCropUnclipped) {
                ImageProcessor.applyGeometry(origPreviewBase, snap.crop)
            } else {
                origPreviewBase
            }

            val hist = ImageProcessor.computeHistogram(processed, snap.adjustments.exposure)

            // Cancellation alone may not stop synchronous bitmap processing. Guard against an
            // older render finishing after a newer edit and overwriting its preview state.
            if (generation == previewRenderGeneration) {
                _uiState.update {
                    it.copy(
                        processedPreview = processed,
                        originalPreview = origMatched,
                        histogram = hist,
                        isRenderingPreview = false
                    )
                }
            }
        }
    }
}
