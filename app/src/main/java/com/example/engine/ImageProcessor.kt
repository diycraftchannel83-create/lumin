package com.example.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import com.example.model.BackgroundReplacement
import com.example.model.CropGeometryState
import com.example.model.EditorStateSnapshot
import com.example.model.FilterCatalog
import com.example.model.FilterPreset
import com.example.model.HistogramData
import com.example.model.MaskLayer
import com.example.model.MaskType
import com.example.model.PhotoAdjustments
import com.example.model.ToneCurvesState
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

object ImageProcessor {

    fun downsampleIfNeeded(source: Bitmap, maxLongEdge: Int): Bitmap {
        val w = source.width
        val h = source.height
        val longest = max(w, h)
        if (longest <= maxLongEdge) return source
        val scale = maxLongEdge.toFloat() / longest.toFloat()
        val nw = (w * scale).roundToInt().coerceAtLeast(1)
        val nh = (h * scale).roundToInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(source, nw, nh, true)
    }

    /**
     * Applies 90° rotations, horizontal/vertical flips, fine straighten angle,
     * and normalized crop rectangle.
     */
    fun applyGeometry(source: Bitmap, crop: CropGeometryState): Bitmap {
        if (crop.isDefault) return source

        // Step 1: Rotate quarter turns & flips
        val matrix = Matrix()
        if (crop.flipHorizontal || crop.flipVertical) {
            matrix.postScale(
                if (crop.flipHorizontal) -1f else 1f,
                if (crop.flipVertical) -1f else 1f
            )
        }
        if (crop.rotationQuarterTurns != 0) {
            matrix.postRotate(crop.rotationQuarterTurns * 90f)
        }

        val oriented = if (!matrix.isIdentity) {
            Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
        } else {
            source
        }

        // Step 2: Straighten with automatic inscribed zoom to avoid black corners
        val straightened = if (abs(crop.straightenDegrees) > 0.05f) {
            val w = oriented.width.toFloat()
            val h = oriented.height.toFloat()
            val rad = Math.toRadians(abs(crop.straightenDegrees).toDouble())
            val sinA = sin(rad).toFloat()
            val cosA = cos(rad).toFloat()
            // Scale factor so rotated rectangle covers original aspect without dark corners
            val scaleW = (w * cosA + h * sinA) / w
            val scaleH = (w * sinA + h * cosA) / h
            val zoom = max(scaleW, scaleH)

            val out = Bitmap.createBitmap(oriented.width, oriented.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(out)
            val m = Matrix()
            m.postTranslate(-w / 2f, -h / 2f)
            m.postRotate(crop.straightenDegrees)
            m.postScale(zoom, zoom)
            m.postTranslate(w / 2f, h / 2f)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            canvas.drawBitmap(oriented, m, paint)
            if (oriented !== source) oriented.recycle()
            out
        } else {
            oriented
        }

        // Step 3: Normalized crop
        if (crop.cropRect.isFull) return straightened

        val cw = straightened.width
        val ch = straightened.height
        val left = (crop.cropRect.left * cw).roundToInt().coerceIn(0, cw - 2)
        val top = (crop.cropRect.top * ch).roundToInt().coerceIn(0, ch - 2)
        val right = (crop.cropRect.right * cw).roundToInt().coerceIn(left + 2, cw)
        val bottom = (crop.cropRect.bottom * ch).roundToInt().coerceIn(top + 2, ch)
        val cropW = (right - left).coerceAtLeast(1)
        val cropH = (bottom - top).coerceAtLeast(1)

        val cropped = Bitmap.createBitmap(straightened, left, top, cropW, cropH)
        if (straightened !== source) straightened.recycle()
        return cropped
    }

    /**
     * Renders the complete non-destructive pipeline on [baseSource] (which already has any
     * AI base edits applied if active).
     */
    fun renderProcessedBitmap(
        baseSource: Bitmap,
        state: EditorStateSnapshot,
        applyGeometryCrop: Boolean = true,
        hasBackgroundRemoved: Boolean = false,
        backgroundStyle: BackgroundReplacement = BackgroundReplacement.TRANSPARENT,
        previewMaskOverlayId: String? = null,
        context: Context? = null,
        galleryShapeBitmaps: Map<String, Bitmap> = emptyMap(),
        applyExposureToPixels: Boolean = true
    ): Bitmap {
        val workingInput = if (applyGeometryCrop) {
            applyGeometry(baseSource, state.crop)
        } else {
            baseSource
        }

        val w = workingInput.width
        val h = workingInput.height
        val total = w * h
        val pixels = IntArray(total)
        workingInput.getPixels(pixels, 0, w, 0, 0, w, h)

        // Optional background replacement when AI Background Remover is active
        if (hasBackgroundRemoved && backgroundStyle != BackgroundReplacement.TRANSPARENT) {
            applyBackgroundReplacement(pixels, workingInput, w, h, backgroundStyle)
        }

        val preset = FilterCatalog.presets.find { it.id == state.selectedPresetId }
            ?: FilterCatalog.presets.first()
        val fMix = state.filterIntensity.coerceIn(0f, 1f)

        val adj = state.adjustments
        val userExposure = if (applyExposureToPixels) adj.exposure else 0f
        val effExposure = userExposure + preset.exposureDelta * fMix
        val effContrast = adj.contrast + preset.contrastDelta * fMix
        val effSaturation = adj.saturation + preset.saturationDelta * fMix
        val effTemp = adj.temperature + preset.temperatureDelta * fMix
        val effTint = adj.tint + preset.tintDelta * fMix

        val useCurves = !state.curves.isLinear
        val rgbLut = if (useCurves) SplineCurveLut.buildLut(state.curves.rgbPoints) else null
        val rLut = if (useCurves) SplineCurveLut.buildLut(state.curves.redPoints) else null
        val gLut = if (useCurves) SplineCurveLut.buildLut(state.curves.greenPoints) else null
        val bLut = if (useCurves) SplineCurveLut.buildLut(state.curves.bluePoints) else null

        val exposureMult = 2.0f.pow(effExposure)
        val contrastFactor = ((effContrast / 100f) + 1f).let { c ->
            if (c <= 1f) c.coerceAtLeast(0.15f) else 1f + (c - 1f) * 1.45f
        }
        val satMult = ((effSaturation / 100f) + 1f).coerceIn(0f, 2.2f)
        val vibranceAmt = (adj.vibrance / 100f).coerceIn(-1f, 1f)
        val highlightsAmt = (adj.highlights / 100f).coerceIn(-1f, 1f)
        val shadowsAmt = (adj.shadows / 100f).coerceIn(-1f, 1f)
        val whitesAmt = (adj.whites / 100f).coerceIn(-1f, 1f)
        val blacksAmt = (adj.blacks / 100f).coerceIn(-1f, 1f)

        val tempShift = (effTemp / 100f) * 0.18f
        val tintShift = (effTint / 100f) * 0.14f

        val fadeBlacks = preset.fadeBlacks * fMix
        val isMono = preset.isMonochrome && fMix > 0.01f
        val monoMix = if (preset.isMonochrome) fMix else 0f

        // Precompute active mask alpha maps if any masks exist
        val enabledMasks = state.masks.filter { it.enabled }
        val maskAlphaMaps: List<FloatArray> = enabledMasks.map { mask ->
            computeMaskAlphaMap(mask, w, h)
        }
        val overlayMaskIndex = if (previewMaskOverlayId != null) {
            enabledMasks.indexOfFirst { it.id == previewMaskOverlayId }
        } else {
            -1
        }

        val hasClarity = adj.clarity > 0.5f || enabledMasks.any { it.adjustments.clarity > 0.5f }
        val origCopyForClarity = if (hasClarity) pixels.copyOf() else null

        val vignetteAmt = (adj.vignette / 100f).coerceIn(-1f, 1f)
        val grainAmt = (adj.grain / 100f).coerceIn(0f, 1f)
        val invW = 1f / max(1, w - 1)
        val invH = 1f / max(1, h - 1)

        for (y in 0 until h) {
            val ny = y * invH
            val dyCenter = (ny - 0.5f) * 2f
            val rowOffset = y * w
            for (x in 0 until w) {
                val idx = rowOffset + x
                val px = pixels[idx]
                val a = (px ushr 24) and 0xFF
                if (a == 0) continue

                var r = ((px ushr 16) and 0xFF) / 255f
                var g = ((px ushr 8) and 0xFF) / 255f
                var b = (px and 0xFF) / 255f

                // 1. Tone Curves (Channel + Master RGB)
                if (useCurves) {
                    val ri = (r * 255f).roundToInt().coerceIn(0, 255)
                    val gi = (g * 255f).roundToInt().coerceIn(0, 255)
                    val bi = (b * 255f).roundToInt().coerceIn(0, 255)
                    r = rgbLut!![rLut!![ri]] / 255f
                    g = rgbLut[gLut!![gi]] / 255f
                    b = rgbLut[bLut!![bi]] / 255f
                }

                // 2. White Balance (Temperature & Tint)
                if (tempShift != 0f || tintShift != 0f) {
                    r += tempShift - tintShift * 0.35f
                    g += tintShift * 0.7f
                    b -= tempShift + tintShift * 0.35f
                }

                // 3. EV Exposure
                if (effExposure != 0f) {
                    r *= exposureMult
                    g *= exposureMult
                    b *= exposureMult
                }

                // 4. Parametric Tone Regions (Blacks, Shadows, Highlights, Whites)
                val luma = (0.2126f * r + 0.7152f * g + 0.0722f * b).coerceIn(0f, 1.5f)
                if (shadowsAmt != 0f || highlightsAmt != 0f || blacksAmt != 0f || whitesAmt != 0f) {
                    val shadowWeight = (1f - (luma / 0.55f)).coerceIn(0f, 1f).pow(1.6f)
                    val highlightWeight = ((luma - 0.45f) / 0.55f).coerceIn(0f, 1f).pow(1.6f)
                    val blackWeight = (1f - (luma / 0.25f)).coerceIn(0f, 1f).pow(2.0f)
                    val whiteWeight = ((luma - 0.75f) / 0.25f).coerceIn(0f, 1f).pow(2.0f)
                    val toneDelta = shadowsAmt * 0.28f * shadowWeight +
                        highlightsAmt * 0.28f * highlightWeight +
                        blacksAmt * 0.20f * blackWeight +
                        whitesAmt * 0.22f * whiteWeight
                    r += toneDelta
                    g += toneDelta
                    b += toneDelta
                }

                // 5. Contrast around mid-gray (0.5)
                if (effContrast != 0f) {
                    r = (r - 0.5f) * contrastFactor + 0.5f
                    g = (g - 0.5f) * contrastFactor + 0.5f
                    b = (b - 0.5f) * contrastFactor + 0.5f
                }

                // 6. Local Masks (Exposure, Contrast, Saturation, Temp, Clarity)
                var localClarityBoost = 0f
                for (mIdx in enabledMasks.indices) {
                    val mAlpha = maskAlphaMaps[mIdx][idx]
                    if (mAlpha > 0.004f) {
                        val mAdj = enabledMasks[mIdx].adjustments
                        var mr = r
                        var mg = g
                        var mb = b
                        if (mAdj.exposure != 0f) {
                            val mExp = 2.0f.pow(mAdj.exposure)
                            mr *= mExp
                            mg *= mExp
                            mb *= mExp
                        }
                        if (mAdj.temperature != 0f) {
                            val mTemp = (mAdj.temperature / 100f) * 0.18f
                            mr += mTemp
                            mb -= mTemp
                        }
                        if (mAdj.contrast != 0f) {
                            val mc = ((mAdj.contrast / 100f) + 1f).coerceAtLeast(0.2f)
                            mr = (mr - 0.5f) * mc + 0.5f
                            mg = (mg - 0.5f) * mc + 0.5f
                            mb = (mb - 0.5f) * mc + 0.5f
                        }
                        if (mAdj.saturation != 0f) {
                            val mSat = ((mAdj.saturation / 100f) + 1f).coerceIn(0f, 2f)
                            val mLum = 0.2126f * mr + 0.7152f * mg + 0.0722f * mb
                            mr = mLum + (mr - mLum) * mSat
                            mg = mLum + (mg - mLum) * mSat
                            mb = mLum + (mb - mLum) * mSat
                        }
                        r = r + (mr - r) * mAlpha
                        g = g + (mg - g) * mAlpha
                        b = b + (mb - b) * mAlpha
                        if (mAdj.clarity > 0f) {
                            localClarityBoost += (mAdj.clarity / 100f) * mAlpha
                        }
                    }
                }

                // 7. Split Toning & Matte Blacks from Filter Preset
                if (fMix > 0.005f) {
                    val toneLum = (0.2126f * r + 0.7152f * g + 0.0722f * b).coerceIn(0f, 1f)
                    val sW = (1f - toneLum).pow(1.4f) * fMix
                    val hW = toneLum.pow(1.4f) * fMix
                    r += preset.shadowTintR * sW + preset.highlightTintR * hW
                    g += preset.shadowTintG * sW + preset.highlightTintG * hW
                    b += preset.shadowTintB * sW + preset.highlightTintB * hW
                    if (fadeBlacks > 0f) {
                        r = fadeBlacks + r * (1f - fadeBlacks * 0.85f)
                        g = fadeBlacks + g * (1f - fadeBlacks * 0.85f)
                        b = fadeBlacks + b * (1f - fadeBlacks * 0.85f)
                    }
                }

                // 8. Vibrance & Saturation
                if (vibranceAmt != 0f || satMult != 1f || isMono) {
                    val curLum = 0.2126f * r + 0.7152f * g + 0.0722f * b
                    if (vibranceAmt != 0f) {
                        val maxC = max(r, max(g, b))
                        val minC = min(r, min(g, b))
                        val curSat = (maxC - minC).coerceIn(0f, 1f)
                        val vibFactor = 1f + vibranceAmt * (1f - curSat) * 0.9f
                        r = curLum + (r - curLum) * vibFactor
                        g = curLum + (g - curLum) * vibFactor
                        b = curLum + (b - curLum) * vibFactor
                    }
                    if (satMult != 1f) {
                        r = curLum + (r - curLum) * satMult
                        g = curLum + (g - curLum) * satMult
                        b = curLum + (b - curLum) * satMult
                    }
                    if (isMono) {
                        val bw = (0.299f * r + 0.587f * g + 0.114f * b)
                        r = r + (bw - r) * monoMix
                        g = g + (bw - g) * monoMix
                        b = b + (bw - b) * monoMix
                    }
                }

                // 9. Clarity (Local Midtone Micro-Contrast)
                val totalClarity = (adj.clarity / 100f) + localClarityBoost
                if (totalClarity > 0.01f && origCopyForClarity != null && x > 0 && x < w - 1 && y > 0 && y < h - 1) {
                    val cL = origCopyForClarity[idx - 1]
                    val cR = origCopyForClarity[idx + 1]
                    val cU = origCopyForClarity[idx - w]
                    val cD = origCopyForClarity[idx + w]
                    val avgR = (((cL ushr 16) and 0xFF) + ((cR ushr 16) and 0xFF) +
                        ((cU ushr 16) and 0xFF) + ((cD ushr 16) and 0xFF)) / (4f * 255f)
                    val avgG = (((cL ushr 8) and 0xFF) + ((cR ushr 8) and 0xFF) +
                        ((cU ushr 8) and 0xFF) + ((cD ushr 8) and 0xFF)) / (4f * 255f)
                    val avgB = ((cL and 0xFF) + (cR and 0xFF) + (cU and 0xFF) + (cD and 0xFF)) / (4f * 255f)
                    val cOrig = origCopyForClarity[idx]
                    val oR = ((cOrig ushr 16) and 0xFF) / 255f
                    val oG = ((cOrig ushr 8) and 0xFF) / 255f
                    val oB = (cOrig and 0xFF) / 255f
                    val strength = totalClarity * 0.75f
                    r += (oR - avgR) * strength
                    g += (oG - avgG) * strength
                    b += (oB - avgB) * strength
                }

                // 10. Vignette
                if (vignetteAmt != 0f) {
                    val dxCenter = (x * invW - 0.5f) * 2f
                    val dist = hypot(dxCenter, dyCenter)
                    val vigWeight = ((dist - 0.45f) / 0.95f).coerceIn(0f, 1f).pow(1.8f)
                    val vigFactor = 1f + vignetteAmt * 0.75f * vigWeight
                    r *= vigFactor
                    g *= vigFactor
                    b *= vigFactor
                }

                // 11. Deterministic Film Grain
                if (grainAmt > 0.01f) {
                    val hash = ((x * 73856093) xor (y * 19349663)) and 0xFF
                    val noise = ((hash / 255f) - 0.5f) * grainAmt * 0.14f
                    r += noise
                    g += noise
                    b += noise
                }

                // 12. Optional Rubylith Mask Visualization Overlay (Preview Only)
                if (overlayMaskIndex >= 0) {
                    val mAlpha = maskAlphaMaps[overlayMaskIndex][idx]
                    if (mAlpha > 0.01f) {
                        val tintAmount = mAlpha * 0.52f
                        r = r * (1f - tintAmount) + 0.98f * tintAmount
                        g = g * (1f - tintAmount) + 0.22f * tintAmount
                        b = b * (1f - tintAmount) + 0.36f * tintAmount
                    }
                }

                val outR = (r * 255f).roundToInt().coerceIn(0, 255)
                val outG = (g * 255f).roundToInt().coerceIn(0, 255)
                val outB = (b * 255f).roundToInt().coerceIn(0, 255)
                pixels[idx] = (a shl 24) or (outR shl 16) or (outG shl 8) or outB
            }
        }

        val outBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        outBitmap.setPixels(pixels, 0, w, 0, 0, w, h)
        if (state.blendLayers.isNotEmpty()) {
            BlendLayerRenderer.renderBlendLayers(
                targetBitmap = outBitmap,
                layers = state.blendLayers,
                context = context,
                galleryShapeBitmaps = galleryShapeBitmaps
            )
        }
        if (workingInput !== baseSource && workingInput !== sourceIfSame(baseSource, workingInput)) {
            workingInput.recycle()
        }
        return outBitmap
    }

    private fun sourceIfSame(a: Bitmap, b: Bitmap): Bitmap? = if (a === b) a else null

    private fun applyBackgroundReplacement(
        pixels: IntArray,
        source: Bitmap,
        w: Int,
        h: Int,
        style: BackgroundReplacement
    ) {
        val bgPixels: IntArray? = if (style == BackgroundReplacement.BOKEH_BLUR) {
            val tiny = Bitmap.createScaledBitmap(source, (w / 14).coerceAtLeast(8), (h / 14).coerceAtLeast(8), true)
            val blurred = Bitmap.createScaledBitmap(tiny, w, h, true)
            tiny.recycle()
            val arr = IntArray(w * h)
            blurred.getPixels(arr, 0, w, 0, 0, w, h)
            blurred.recycle()
            arr
        } else null

        val solidBgR = when (style) {
            BackgroundReplacement.PURE_WHITE -> 248
            BackgroundReplacement.STUDIO_DARK -> 18
            else -> 0
        }
        val solidBgG = when (style) {
            BackgroundReplacement.PURE_WHITE -> 249
            BackgroundReplacement.STUDIO_DARK -> 21
            else -> 0
        }
        val solidBgB = when (style) {
            BackgroundReplacement.PURE_WHITE -> 252
            BackgroundReplacement.STUDIO_DARK -> 28
            else -> 0
        }

        for (i in pixels.indices) {
            val c = pixels[i]
            val a = ((c ushr 24) and 0xFF) / 255f
            if (a >= 0.999f) continue
            val fgR = (c ushr 16) and 0xFF
            val fgG = (c ushr 8) and 0xFF
            val fgB = c and 0xFF

            val bgR: Int
            val bgG: Int
            val bgB: Int
            if (bgPixels != null) {
                val bc = bgPixels[i]
                bgR = (bc ushr 16) and 0xFF
                bgG = (bc ushr 8) and 0xFF
                bgB = bc and 0xFF
            } else {
                bgR = solidBgR
                bgG = solidBgG
                bgB = solidBgB
            }

            val r = (fgR * a + bgR * (1f - a)).roundToInt().coerceIn(0, 255)
            val g = (fgG * a + bgG * (1f - a)).roundToInt().coerceIn(0, 255)
            val b = (fgB * a + bgB * (1f - a)).roundToInt().coerceIn(0, 255)
            pixels[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }
    }

    private fun computeMaskAlphaMap(mask: MaskLayer, w: Int, h: Int): FloatArray {
        val map = FloatArray(w * h)
        val invW = 1f / max(1, w - 1)
        val invH = 1f / max(1, h - 1)
        val feather = mask.feather.coerceIn(0.05f, 1f)

        when (mask.type) {
            MaskType.RADIAL -> {
                val rx = mask.radiusX.coerceAtLeast(0.03f)
                val ry = mask.radiusY.coerceAtLeast(0.03f)
                val innerEdge = (1f - feather).coerceIn(0f, 0.95f)
                for (y in 0 until h) {
                    val dy = (y * invH - mask.centerY) / ry
                    val row = y * w
                    for (x in 0 until w) {
                        val dx = (x * invW - mask.centerX) / rx
                        val d = hypot(dx, dy)
                        var alpha = when {
                            d <= innerEdge -> 1f
                            d >= 1f -> 0f
                            else -> {
                                val t = (d - innerEdge) / (1f - innerEdge)
                                // Smoothstep falloff
                                1f - (t * t * (3f - 2f * t))
                            }
                        }
                        if (mask.inverted) alpha = 1f - alpha
                        map[row + x] = alpha
                    }
                }
            }
            MaskType.LINEAR -> {
                val vx = mask.endX - mask.startX
                val vy = mask.endY - mask.startY
                val lenSq = (vx * vx + vy * vy).coerceAtLeast(1e-4f)
                for (y in 0 until h) {
                    val ny = y * invH
                    val row = y * w
                    for (x in 0 until w) {
                        val nx = x * invW
                        val proj = ((nx - mask.startX) * vx + (ny - mask.startY) * vy) / lenSq
                        val t = proj.coerceIn(0f, 1f)
                        var alpha = 1f - (t * t * (3f - 2f * t))
                        if (mask.inverted) alpha = 1f - alpha
                        map[row + x] = alpha
                    }
                }
            }
            MaskType.BRUSH -> {
                val minDim = min(w, h).toFloat()
                for (stroke in mask.strokes) {
                    val rPx = (stroke.radius * minDim).coerceAtLeast(5f)
                    val rSq = rPx * rPx
                    val innerFrac = (1f - feather * 0.85f).coerceIn(0.1f, 0.95f)
                    for (pt in stroke.points) {
                        val cx = (pt.x * w).roundToInt()
                        val cy = (pt.y * h).roundToInt()
                        val minX = (cx - rPx.toInt()).coerceIn(0, w - 1)
                        val maxX = (cx + rPx.toInt()).coerceIn(0, w - 1)
                        val minY = (cy - rPx.toInt()).coerceIn(0, h - 1)
                        val maxY = (cy + rPx.toInt()).coerceIn(0, h - 1)
                        for (y in minY..maxY) {
                            val dy = (y - cy).toFloat()
                            val dy2 = dy * dy
                            val row = y * w
                            for (x in minX..maxX) {
                                val dx = (x - cx).toFloat()
                                val d2 = dx * dx + dy2
                                if (d2 <= rSq) {
                                    val dNorm = sqrt(d2) / rPx
                                    val weight = if (dNorm <= innerFrac) {
                                        1f
                                    } else {
                                        val t = ((dNorm - innerFrac) / (1f - innerFrac)).coerceIn(0f, 1f)
                                        1f - (t * t * (3f - 2f * t))
                                    }
                                    val idx = row + x
                                    if (weight > map[idx]) map[idx] = weight
                                }
                            }
                        }
                    }
                }
                if (mask.inverted) {
                    for (i in map.indices) {
                        map[i] = 1f - map[i]
                    }
                }
            }
        }
        return map
    }

    fun applyExposureColorFilter(source: Bitmap, exposureEv: Float): Bitmap {
        val out = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = ExposureColorMatrix.createAndroidColorFilter(exposureEv)
        }
        canvas.drawBitmap(source, 0f, 0f, paint)
        return out
    }

    fun computeHistogram(bitmap: Bitmap, exposureEv: Float = 0f): HistogramData {
        val bins = 64
        val lumaBins = FloatArray(bins)
        val rBins = FloatArray(bins)
        val gBins = FloatArray(bins)
        val bBins = FloatArray(bins)

        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
        val expScale = if (abs(exposureEv) > 0.005f) {
            ExposureColorMatrix.exposureScaleFactor(exposureEv)
        } else {
            1f
        }

        val step = max(1, pixels.size / 12_000)
        var i = 0
        while (i < pixels.size) {
            val px = pixels[i]
            val a = (px ushr 24) and 0xFF
            if (a > 16) {
                val r = (((px ushr 16) and 0xFF) * expScale).roundToInt().coerceIn(0, 255)
                val g = (((px ushr 8) and 0xFF) * expScale).roundToInt().coerceIn(0, 255)
                val b = ((px and 0xFF) * expScale).roundToInt().coerceIn(0, 255)
                val lum = (0.2126f * r + 0.7152f * g + 0.0722f * b).roundToInt().coerceIn(0, 255)
                rBins[r * (bins - 1) / 255] += 1f
                gBins[g * (bins - 1) / 255] += 1f
                bBins[b * (bins - 1) / 255] += 1f
                lumaBins[lum * (bins - 1) / 255] += 1f
            }
            i += step
        }

        fun normalize(arr: FloatArray) {
            var peak = 1f
            for (v in arr) if (v > peak) peak = v
            for (j in arr.indices) arr[j] = (arr[j] / peak).coerceIn(0f, 1f)
        }
        normalize(lumaBins)
        normalize(rBins)
        normalize(gBins)
        normalize(bBins)

        return HistogramData(luma = lumaBins, red = rBins, green = gBins, blue = bBins)
    }

    fun renderFilterThumbnails(baseSource: Bitmap): Map<String, Bitmap> {
        val thumbSize = 112
        val w = baseSource.width
        val h = baseSource.height
        val side = min(w, h)
        val xOff = (w - side) / 2
        val yOff = (h - side) / 2
        val square = Bitmap.createBitmap(baseSource, xOff, yOff, side, side)
        val small = Bitmap.createScaledBitmap(square, thumbSize, thumbSize, true)
        if (square !== baseSource) square.recycle()

        val result = LinkedHashMap<String, Bitmap>(FilterCatalog.presets.size)
        for (preset in FilterCatalog.presets) {
            val snap = EditorStateSnapshot(
                selectedPresetId = preset.id,
                filterIntensity = 1f
            )
            result[preset.id] = renderProcessedBitmap(
                baseSource = small,
                state = snap,
                applyGeometryCrop = false
            )
        }
        if (small !== baseSource) small.recycle()
        return result
    }
}
