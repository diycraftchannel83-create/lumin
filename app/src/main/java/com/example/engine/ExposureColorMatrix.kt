package com.example.engine

import androidx.compose.ui.graphics.ColorMatrix as ComposeColorMatrix
import android.graphics.ColorMatrix as AndroidColorMatrix
import android.graphics.ColorMatrixColorFilter
import kotlin.math.pow

/**
 * Builds photographic exposure scale matrices (2^EV) for real-time GPU ColorFilter
 * transformations in Compose as well as Android Canvas ColorMatrixColorFilter export.
 */
object ExposureColorMatrix {

    fun exposureScaleFactor(exposureEv: Float): Float {
        return 2.0f.pow(exposureEv.coerceIn(-2.0f, 2.0f))
    }

    fun createComposeMatrix(exposureEv: Float): ComposeColorMatrix {
        val scale = exposureScaleFactor(exposureEv)
        return ComposeColorMatrix().apply {
            setToScale(
                redScale = scale,
                greenScale = scale,
                blueScale = scale,
                alphaScale = 1f
            )
        }
    }

    fun createAndroidMatrix(exposureEv: Float): AndroidColorMatrix {
        val scale = exposureScaleFactor(exposureEv)
        return AndroidColorMatrix().apply {
            setScale(scale, scale, scale, 1f)
        }
    }

    fun createAndroidColorFilter(exposureEv: Float): ColorMatrixColorFilter {
        return ColorMatrixColorFilter(createAndroidMatrix(exposureEv))
    }
}
