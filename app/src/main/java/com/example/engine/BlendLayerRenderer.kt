package com.example.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BlendMode
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Build
import android.util.LruCache
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.example.model.BlendFontStyle
import com.example.model.BlendLayer
import com.example.model.BlendLayerKind
import com.example.model.BlendShapeType
import com.example.model.GalleryShapeCutout
import com.example.model.LayerBlendMode
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

object BlendLayerRenderer {

    private val cutoutCache = object : LruCache<String, Bitmap>(10) {}

    fun renderBlendLayers(
        targetBitmap: Bitmap,
        layers: List<BlendLayer>,
        context: Context?,
        galleryShapeBitmaps: Map<String, Bitmap> = emptyMap()
    ) {
        val visibleLayers = layers.filter { it.visible }
        if (visibleLayers.isEmpty()) return

        val canvas = Canvas(targetBitmap)
        val w = targetBitmap.width.toFloat()
        val h = targetBitmap.height.toFloat()
        val minDim = min(w, h)

        for (layer in visibleLayers) {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                color = layer.colorArgb
                alpha = (layer.opacity.coerceIn(0f, 1f) * 255f).roundToInt()
                applyLayerBlendMode(this, layer.blendMode)
            }

            canvas.save()
            canvas.translate(layer.posX * w, layer.posY * h)
            canvas.rotate(layer.rotationDeg)

            when (layer.kind) {
                BlendLayerKind.TEXT -> drawTextLayer(canvas, layer, minDim, paint, context)
                BlendLayerKind.SHAPE -> drawShapeLayer(canvas, layer, minDim, paint)
                BlendLayerKind.GALLERY_SHAPE -> {
                    val assetBmp = layer.galleryAssetId?.let { galleryShapeBitmaps[it] }
                    if (assetBmp != null && !assetBmp.isRecycled) {
                        drawGalleryShapeLayer(canvas, layer, minDim, paint, assetBmp)
                    }
                }
            }

            canvas.restore()
        }
    }

    private fun drawGalleryShapeLayer(
        canvas: Canvas,
        layer: BlendLayer,
        minDim: Float,
        paint: Paint,
        sourceBitmap: Bitmap
    ) {
        val prepared = when (layer.galleryCutout) {
            GalleryShapeCutout.ORIGINAL -> sourceBitmap
            GalleryShapeCutout.DROP_WHITE, GalleryShapeCutout.DROP_BLACK -> {
                val cacheKey = "${layer.galleryAssetId}_${sourceBitmap.width}x${sourceBitmap.height}_${layer.galleryCutout.name}"
                val cached = synchronized(cutoutCache) { cutoutCache.get(cacheKey) }
                if (cached != null && !cached.isRecycled) {
                    cached
                } else {
                    val computed = buildCutoutBitmap(sourceBitmap, layer.galleryCutout)
                    synchronized(cutoutCache) { cutoutCache.put(cacheKey, computed) }
                    computed
                }
            }
        }

        if (layer.tintGalleryShape) {
            paint.colorFilter = PorterDuffColorFilter(layer.colorArgb, PorterDuff.Mode.SRC_IN)
        }

        val bw = prepared.width.toFloat().coerceAtLeast(1f)
        val bh = prepared.height.toFloat().coerceAtLeast(1f)
        val maxEdge = max(bw, bh)
        val targetMaxEdge = (minDim * 0.52f * layer.scale).coerceAtLeast(24f)
        val drawScale = targetMaxEdge / maxEdge
        val drawW = bw * drawScale
        val drawH = bh * drawScale
        val destRect = RectF(-drawW / 2f, -drawH / 2f, drawW / 2f, drawH / 2f)

        canvas.drawBitmap(prepared, null, destRect, paint)
    }

    private fun buildCutoutBitmap(source: Bitmap, mode: GalleryShapeCutout): Bitmap {
        val w = source.width
        val h = source.height
        val pixels = IntArray(w * h)
        source.getPixels(pixels, 0, w, 0, 0, w, h)

        for (i in pixels.indices) {
            val px = pixels[i]
            val origAlpha = (px ushr 24) and 0xFF
            if (origAlpha == 0) continue

            val r = (px ushr 16) and 0xFF
            val g = (px ushr 8) and 0xFF
            val b = px and 0xFF
            val maxC = max(r, max(g, b))
            val minC = min(r, min(g, b))
            val chroma = maxC - minC
            val luma = 0.299f * r + 0.587f * g + 0.114f * b

            val cutoutAlpha = when (mode) {
                GalleryShapeCutout.DROP_WHITE -> {
                    if (chroma > 48 && luma < 245f) {
                        1f
                    } else if (luma >= 238f && chroma < 36) {
                        0f
                    } else if (luma > 195f && chroma < 42) {
                        val t = ((luma - 195f) / (238f - 195f)).coerceIn(0f, 1f)
                        1f - (t * t * (3f - 2f * t))
                    } else {
                        1f
                    }
                }
                GalleryShapeCutout.DROP_BLACK -> {
                    if (chroma > 48 && luma > 12f) {
                        1f
                    } else if (luma <= 20f && chroma < 36) {
                        0f
                    } else if (luma < 62f && chroma < 42) {
                        val t = ((luma - 20f) / (62f - 20f)).coerceIn(0f, 1f)
                        t * t * (3f - 2f * t)
                    } else {
                        1f
                    }
                }
                GalleryShapeCutout.ORIGINAL -> 1f
            }

            val finalAlpha = (origAlpha * cutoutAlpha).roundToInt().coerceIn(0, 255)
            pixels[i] = (finalAlpha shl 24) or (px and 0x00FFFFFF)
        }

        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        out.setPixels(pixels, 0, w, 0, 0, w, h)
        return out
    }

    private fun drawTextLayer(
        canvas: Canvas,
        layer: BlendLayer,
        minDim: Float,
        paint: Paint,
        context: Context?
    ) {
        val displayText = layer.text.ifBlank { "LUMINA" }
        val textSizePx = (minDim * 0.12f * layer.scale).coerceIn(14f, minDim * 0.85f)
        paint.textSize = textSizePx
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = resolveTypeface(context, layer.fontStyle)
        paint.style = Paint.Style.FILL
        paint.letterSpacing = 0.06f

        val lines = displayText.split("\n")
        val fm = paint.fontMetrics
        val lineHeight = fm.descent - fm.ascent
        val totalHeight = lineHeight * lines.size
        var yOffset = -totalHeight / 2f - fm.ascent
        for (line in lines) {
            canvas.drawText(line, 0f, yOffset, paint)
            yOffset += lineHeight
        }
    }

    private fun drawShapeLayer(
        canvas: Canvas,
        layer: BlendLayer,
        minDim: Float,
        paint: Paint
    ) {
        val radius = (minDim * 0.24f * layer.scale).coerceAtLeast(16f)
        val strokeWidth = (radius * 0.14f).coerceAtLeast(3f)

        paint.style = if (layer.strokeOnly || layer.shapeType == BlendShapeType.RING || layer.shapeType == BlendShapeType.FRAME) {
            Paint.Style.STROKE
        } else {
            Paint.Style.FILL
        }
        paint.strokeWidth = strokeWidth
        paint.strokeJoin = Paint.Join.ROUND
        paint.strokeCap = Paint.Cap.ROUND

        when (layer.shapeType) {
            BlendShapeType.RING, BlendShapeType.DISC -> {
                canvas.drawCircle(0f, 0f, radius, paint)
            }
            BlendShapeType.DIAMOND -> {
                val path = Path().apply {
                    moveTo(0f, -radius)
                    lineTo(radius * 0.82f, 0f)
                    lineTo(0f, radius)
                    lineTo(-radius * 0.82f, 0f)
                    close()
                }
                canvas.drawPath(path, paint)
            }
            BlendShapeType.PRISM -> {
                val path = Path().apply {
                    moveTo(0f, -radius)
                    lineTo(radius * 0.92f, radius * 0.72f)
                    lineTo(-radius * 0.92f, radius * 0.72f)
                    close()
                }
                canvas.drawPath(path, paint)
            }
            BlendShapeType.STAR -> {
                val points = 8
                val outerR = radius
                val innerR = radius * 0.42f
                val path = Path()
                for (i in 0 until points * 2) {
                    val r = if (i % 2 == 0) outerR else innerR
                    val angle = Math.toRadians((i * 180.0 / points) - 90.0)
                    val px = (cos(angle) * r).toFloat()
                    val py = (sin(angle) * r).toFloat()
                    if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                path.close()
                canvas.drawPath(path, paint)
            }
            BlendShapeType.ARCH -> {
                val rect = RectF(-radius * 0.78f, -radius, radius * 0.78f, radius * 0.6f)
                val path = Path().apply {
                    moveTo(-radius * 0.78f, radius)
                    lineTo(-radius * 0.78f, -radius * 0.2f)
                    arcTo(rect, 180f, 180f, false)
                    lineTo(radius * 0.78f, radius)
                    close()
                }
                canvas.drawPath(path, paint)
            }
            BlendShapeType.FRAME -> {
                val rect = RectF(-radius, -radius * 1.2f, radius, radius * 1.2f)
                canvas.drawRoundRect(rect, radius * 0.08f, radius * 0.08f, paint)
            }
            BlendShapeType.STRIPES -> {
                val barCount = 4
                val barHeight = radius * 0.22f
                val gap = radius * 0.36f
                val startY = -((barCount - 1) * gap) / 2f
                for (i in 0 until barCount) {
                    val cy = startY + i * gap
                    val rect = RectF(-radius, cy - barHeight / 2f, radius, cy + barHeight / 2f)
                    canvas.drawRoundRect(rect, barHeight / 2f, barHeight / 2f, paint)
                }
            }
        }
    }

    private fun resolveTypeface(context: Context?, fontStyle: BlendFontStyle): Typeface {
        if (context == null) {
            return if (fontStyle == BlendFontStyle.EDITORIAL) Typeface.SERIF else Typeface.DEFAULT_BOLD
        }
        return when (fontStyle) {
            BlendFontStyle.DISPLAY -> ResourcesCompat.getFont(context, R.font.space_grotesk) ?: Typeface.DEFAULT_BOLD
            BlendFontStyle.MODERN -> ResourcesCompat.getFont(context, R.font.plus_jakarta_sans) ?: Typeface.SANS_SERIF
            BlendFontStyle.MONO -> ResourcesCompat.getFont(context, R.font.jetbrains_mono) ?: Typeface.MONOSPACE
            BlendFontStyle.EDITORIAL -> Typeface.create(Typeface.SERIF, Typeface.BOLD_ITALIC)
        }
    }

    private fun applyLayerBlendMode(paint: Paint, mode: LayerBlendMode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            paint.blendMode = when (mode) {
                LayerBlendMode.NORMAL -> BlendMode.SRC_OVER
                LayerBlendMode.OVERLAY -> BlendMode.OVERLAY
                LayerBlendMode.MULTIPLY -> BlendMode.MULTIPLY
                LayerBlendMode.SCREEN -> BlendMode.SCREEN
                LayerBlendMode.SOFT_LIGHT -> BlendMode.SOFT_LIGHT
                LayerBlendMode.DARKEN -> BlendMode.DARKEN
                LayerBlendMode.LIGHTEN -> BlendMode.LIGHTEN
                LayerBlendMode.DIFFERENCE -> BlendMode.DIFFERENCE
            }
        } else {
            @Suppress("DEPRECATION")
            val pdMode = when (mode) {
                LayerBlendMode.NORMAL -> PorterDuff.Mode.SRC_OVER
                LayerBlendMode.OVERLAY -> PorterDuff.Mode.OVERLAY
                LayerBlendMode.MULTIPLY -> PorterDuff.Mode.MULTIPLY
                LayerBlendMode.SCREEN -> PorterDuff.Mode.SCREEN
                LayerBlendMode.SOFT_LIGHT -> PorterDuff.Mode.OVERLAY
                LayerBlendMode.DARKEN -> PorterDuff.Mode.DARKEN
                LayerBlendMode.LIGHTEN -> PorterDuff.Mode.LIGHTEN
                LayerBlendMode.DIFFERENCE -> PorterDuff.Mode.XOR
            }
            paint.xfermode = PorterDuffXfermode(pdMode)
        }
    }
}
