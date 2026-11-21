package com.example.ai

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.Log
import com.example.engine.ImageProcessor
import com.example.model.BrushStroke
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.ImagePart
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.util.ArrayDeque
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private const val TAG = "FirebaseAiImageRepo"

class FirebaseAiImageRepository : AiImageRepository {

    private val imageEditModel by lazy {
        val config = generationConfig {
            responseModalities = listOf(ResponseModality.IMAGE)
        }
        Firebase.ai.generativeModel(
            modelName = "gemini-nano-banana-2.1",
            generationConfig = config
        )
    }

    override suspend fun removeObject(
        source: Bitmap,
        maskStrokes: List<BrushStroke>
    ): AiEditResult = withContext(Dispatchers.Default) {
        if (maskStrokes.isEmpty()) {
            throw IllegalArgumentException("Brush over the object you want to erase first.")
        }

        // Keep inline media safely under payload limits while preserving crisp detail
        val preparedInput = ImageProcessor.downsampleIfNeeded(source, 1280)
        val guideBitmap = createAnnotatedMaskGuide(preparedInput, maskStrokes)

        try {
            val response = withTimeout(60_000L) {
                imageEditModel.generateContent(
                    content {
                        image(preparedInput)
                        image(guideBitmap)
                        text(
                            "Edit the first photograph to completely and seamlessly remove the object(s) " +
                                "marked with bright magenta strokes in the second guide image. " +
                                "Inpaint the removed area with realistic surrounding textures, lighting, " +
                                "depth of field, and natural shadows. Preserve all unmasked areas of the " +
                                "first photograph identically without altering framing or colors."
                        )
                    }
                )
            }

            val outputPart = response.candidates.firstOrNull()
                ?.content?.parts?.filterIsInstance<ImagePart>()
                ?.firstOrNull()
                ?: throw IllegalStateException("Gemini did not return an edited image.")

            val rawResult = outputPart.image
            val matched = if (rawResult.width != source.width || rawResult.height != source.height) {
                Bitmap.createScaledBitmap(rawResult, source.width, source.height, true)
            } else {
                rawResult
            }

            AiEditResult(
                bitmap = matched,
                isSamplePlaceholder = false,
                notice = "AI Object Removal completed via Gemini."
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI object removal failed", e)
            throw e
        } finally {
            if (guideBitmap !== preparedInput) guideBitmap.recycle()
            if (preparedInput !== source) preparedInput.recycle()
        }
    }

    override suspend fun removeBackground(source: Bitmap): AiEditResult = withContext(Dispatchers.Default) {
        val preparedInput = ImageProcessor.downsampleIfNeeded(source, 1280)
        try {
            val response = withTimeout(60_000L) {
                imageEditModel.generateContent(
                    content {
                        image(preparedInput)
                        text(
                            "Isolate the main foreground subject(s) from this photo with clean, accurate edges " +
                                "and place the subject on a completely uniform, flat pure neon green (#00FF00) " +
                                "chroma-key background with no shadows, gradients, or floor reflections. " +
                                "Preserve the subject's original pose, proportions, colors, and fine details exactly."
                        )
                    }
                )
            }

            val outputPart = response.candidates.firstOrNull()
                ?.content?.parts?.filterIsInstance<ImagePart>()
                ?.firstOrNull()
                ?: throw IllegalStateException("Gemini did not return an extracted subject image.")

            val chromaCutout = extractChromaForeground(outputPart.image)
            val matched = if (chromaCutout.width != source.width || chromaCutout.height != source.height) {
                Bitmap.createScaledBitmap(chromaCutout, source.width, source.height, true).also {
                    if (it !== chromaCutout) chromaCutout.recycle()
                }
            } else {
                chromaCutout
            }

            AiEditResult(
                bitmap = matched,
                isSamplePlaceholder = false,
                notice = "1-Click Subject Extraction completed via Gemini."
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI background removal failed", e)
            throw e
        } finally {
            if (preparedInput !== source) preparedInput.recycle()
        }
    }

    override suspend fun enhancePhoto(source: Bitmap): AiEditResult = withContext(Dispatchers.Default) {
        val preparedInput = ImageProcessor.downsampleIfNeeded(source, 1280)
        try {
            val response = withTimeout(90_000L) {
                imageEditModel.generateContent(
                    content {
                        image(preparedInput)
                        text(
                            "Enhance this photograph to look clean, sharp, detailed, and high quality. " +
                                "Reduce blur, motion softness where possible, compression artifacts, chroma and luminance noise. " +
                                "Recover natural fine detail and improve edge clarity without oversharpening, halos, plastic skin, " +
                                "or unnatural textures. Improve local contrast and tonal clarity while keeping colors realistic. " +
                                "Preserve the original composition, crop, camera perspective, objects, facial identity, " +
                                "expressions, clothing, text, and scene exactly. Do not add, remove, replace, or invent objects. " +
                                "Do not apply a stylized look. Return a natural photographic enhancement."
                        )
                    }
                )
            }
            val outputPart = response.candidates.firstOrNull()
                ?.content?.parts?.filterIsInstance<ImagePart>()
                ?.firstOrNull()
                ?: throw IllegalStateException("Gemini did not return an enhanced image.")

            val rawResult = outputPart.image
            val matched = if (rawResult.width != source.width || rawResult.height != source.height) {
                Bitmap.createScaledBitmap(rawResult, source.width, source.height, true)
            } else {
                rawResult
            }
            AiEditResult(
                bitmap = matched,
                isSamplePlaceholder = false,
                notice = "AI Enhance completed. Blur, noise, and detail were improved where possible."
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI photo enhancement failed", e)
            throw e
        } finally {
            if (preparedInput !== source) preparedInput.recycle()
        }
    }

    private fun createAnnotatedMaskGuide(
        base: Bitmap,
        strokes: List<BrushStroke>
    ): Bitmap {
        val copy = base.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(copy)
        val w = copy.width.toFloat()
        val h = copy.height.toFloat()
        val minDim = min(w, h)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(235, 255, 0, 220)
            style = Paint.Style.FILL
        }
        for (stroke in strokes) {
            val r = (stroke.radius * minDim).coerceAtLeast(6f)
            for (pt in stroke.points) {
                canvas.drawCircle(pt.x * w, pt.y * h, r, paint)
            }
        }
        return copy
    }

    /**
     * Flood-fills from image borders across the green chroma background so interior green
     * details on the subject itself are preserved while exterior background becomes transparent.
     */
    private fun extractChromaForeground(bitmap: Bitmap): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

        val visited = BooleanArray(w * h)
        val isBg = BooleanArray(w * h)
        val queue = ArrayDeque<Int>()

        fun isChromaGreen(c: Int): Boolean {
            val r = (c ushr 16) and 0xFF
            val g = (c ushr 8) and 0xFF
            val b = c and 0xFF
            val greenDominance = g - max(r, b)
            return g > 110 && greenDominance > 36
        }

        fun enqueueIfBg(idx: Int) {
            if (!visited[idx] && isChromaGreen(pixels[idx])) {
                visited[idx] = true
                isBg[idx] = true
                queue.add(idx)
            }
        }

        for (x in 0 until w) {
            enqueueIfBg(x)
            enqueueIfBg((h - 1) * w + x)
        }
        for (y in 0 until h) {
            enqueueIfBg(y * w)
            enqueueIfBg(y * w + (w - 1))
        }

        while (queue.isNotEmpty()) {
            val cur = queue.removeFirst()
            val cx = cur % w
            val cy = cur / w
            if (cx > 0) enqueueIfBg(cur - 1)
            if (cx < w - 1) enqueueIfBg(cur + 1)
            if (cy > 0) enqueueIfBg(cur - w)
            if (cy < h - 1) enqueueIfBg(cur + w)
        }

        // Convert background pixels to alpha = 0 and feather + de-spill 1px border halo
        for (y in 0 until h) {
            val row = y * w
            for (x in 0 until w) {
                val idx = row + x
                if (isBg[idx]) {
                    pixels[idx] = Color.TRANSPARENT
                } else {
                    var bgNeighbors = 0
                    if (x > 0 && isBg[idx - 1]) bgNeighbors++
                    if (x < w - 1 && isBg[idx + 1]) bgNeighbors++
                    if (y > 0 && isBg[idx - w]) bgNeighbors++
                    if (y < h - 1 && isBg[idx + w]) bgNeighbors++

                    if (bgNeighbors > 0) {
                        val c = pixels[idx]
                        val r = (c ushr 16) and 0xFF
                        val g = (c ushr 8) and 0xFF
                        val b = c and 0xFF
                        // Green spill suppression on boundary pixels
                        val cleanG = min(g, ((r + b) / 2 + 12)).coerceIn(0, 255)
                        val alpha = (255 - bgNeighbors * 42).coerceIn(90, 255)
                        pixels[idx] = Color.argb(alpha, r, cleanG, b)
                    }
                }
            }
        }

        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        out.setPixels(pixels, 0, w, 0, 0, w, h)
        return out
    }
}
