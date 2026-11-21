package com.example.ai

import android.graphics.Bitmap
import com.example.model.BrushStroke

data class AiEditResult(
    val bitmap: Bitmap,
    val isSamplePlaceholder: Boolean,
    val notice: String
)

/**
 * Abstraction for generative photo editing tools (AI Object Removal & 1-Click Background Removal).
 * When Firebase AI Logic is provisioned, this interface routes calls to Gemini image editing models;
 * otherwise it is backed by LocalPlaceholderAiImageRepository with an explicit sample preview notice.
 */
interface AiImageRepository {
    suspend fun removeObject(
        source: Bitmap,
        maskStrokes: List<BrushStroke>
    ): AiEditResult

    suspend fun removeBackground(
        source: Bitmap
    ): AiEditResult
}
