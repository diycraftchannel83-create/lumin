package com.example.ai

import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * JNI entry point for the bundled NCNN Real-ESRGAN native inference bridge.
 * The native library is built by CI after downloading the official NCNN Android Vulkan SDK.
 */
object RealEsrganNative {
    private const val LIBRARY_NAME = "photostudio_realesrgan"
    private var libraryLoaded = false

    @Synchronized
    fun isAvailable(): Boolean {
        if (libraryLoaded) return true
        return try {
            System.loadLibrary(LIBRARY_NAME)
            libraryLoaded = true
            true
        } catch (_: UnsatisfiedLinkError) {
            false
        }
    }

    suspend fun upscale(
        source: Bitmap,
        model: RealEsrganModelManager.InstalledModel,
        tileSize: Int = 256,
        useVulkan: Boolean = true
    ): Bitmap = withContext(Dispatchers.Default) {
        check(isAvailable()) { "Real-ESRGAN native engine is not installed in this build." }
        require(source.width <= 8192 && source.height <= 8192) {
            "Source dimensions exceed the safe native processing limit."
        }
        val outWidth = source.width * model.spec.scale
        val outHeight = source.height * model.spec.scale
        require(outWidth.toLong() * outHeight <= 8_000_000L) {
            "Upscaled output exceeds the safe 8 MP memory limit."
        }
        val output = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(source.width * source.height)
        source.getPixels(pixels, 0, source.width, 0, 0, source.width, source.height)
        val result = nativeUpscale(
            pixels, source.width, source.height,
            model.paramFile.absolutePath, model.binFile.absolutePath,
            model.spec.scale, tileSize, useVulkan
        )
        require(result.size == outWidth * outHeight) { "Native inference returned unexpected output dimensions." }
        output.setPixels(result, 0, outWidth, 0, 0, outWidth, outHeight)
        output
    }

    private external fun nativeUpscale(
        sourceArgb: IntArray,
        width: Int,
        height: Int,
        paramPath: String,
        modelPath: String,
        scale: Int,
        tileSize: Int,
        useVulkan: Boolean
    ): IntArray
}
