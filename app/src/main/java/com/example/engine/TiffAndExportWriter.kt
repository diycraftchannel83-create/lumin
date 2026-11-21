package com.example.engine

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.model.ExportFormat
import com.example.model.ExportResolution
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

data class ExportResult(
    val displayPath: String,
    val fileSizeBytes: Long,
    val width: Int,
    val height: Int,
    val shareUri: Uri,
    val mimeType: String
)

object TiffAndExportWriter {

    suspend fun exportBitmap(
        context: Context,
        processedFullRes: Bitmap,
        format: ExportFormat,
        resolution: ExportResolution
    ): ExportResult = withContext(Dispatchers.IO) {
        val scaled = scaleForResolution(processedFullRes, resolution)
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "PhotoStudio_${timestamp}.${format.extension}"

        // 1. Write to app cache exports/ for instant FileProvider sharing + exact byte count
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val cacheFile = File(exportDir, fileName)
        FileOutputStream(cacheFile).use { fos ->
            BufferedOutputStream(fos, 64 * 1024).use { bos ->
                encodeBitmap(scaled, format, bos)
                bos.flush()
            }
        }
        val fileBytes = cacheFile.length()

        // 2. Also save to MediaStore Gallery / Downloads (API 29+ scoped storage: zero permissions!)
        val savedDisplayPath = saveToMediaStore(context, cacheFile, fileName, format)

        val shareUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            cacheFile
        )

        val outW = scaled.width
        val outH = scaled.height
        if (scaled !== processedFullRes) {
            scaled.recycle()
        }

        ExportResult(
            displayPath = savedDisplayPath,
            fileSizeBytes = fileBytes,
            width = outW,
            height = outH,
            shareUri = shareUri,
            mimeType = format.mimeType
        )
    }

    private fun scaleForResolution(source: Bitmap, resolution: ExportResolution): Bitmap {
        val maxEdge = resolution.maxLongEdge ?: return source
        val w = source.width
        val h = source.height
        val longest = max(w, h)
        if (longest <= maxEdge) return source
        val scale = maxEdge.toFloat() / longest.toFloat()
        val nw = (w * scale).roundToInt().coerceAtLeast(1)
        val nh = (h * scale).roundToInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(source, nw, nh, true)
    }

    private fun encodeBitmap(bitmap: Bitmap, format: ExportFormat, out: OutputStream) {
        when (format) {
            ExportFormat.PNG_LOSSLESS -> {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            ExportFormat.WEBP_LOSSLESS -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSLESS, 100, out)
                } else {
                    @Suppress("DEPRECATION")
                    bitmap.compress(Bitmap.CompressFormat.WEBP, 100, out)
                }
            }
            ExportFormat.JPEG_MAX -> {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
            }
            ExportFormat.TIFF_UNCOMPRESSED -> {
                writeUncompressedTiff(bitmap, out)
            }
        }
    }

    private fun saveToMediaStore(
        context: Context,
        sourceFile: File,
        fileName: String,
        format: ExportFormat
    ): String {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return "App Cache (${sourceFile.name}) — tap Share to save"
        }
        val resolver = context.contentResolver
        val isTiff = format == ExportFormat.TIFF_UNCOMPRESSED
        val collection = if (isTiff) {
            MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        }
        val relativeDir = if (isTiff) {
            "${Environment.DIRECTORY_DOWNLOADS}/LuminaLab"
        } else {
            "${Environment.DIRECTORY_PICTURES}/LuminaLab"
        }

        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, format.mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, relativeDir)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }

        val uri = resolver.insert(collection, values)
        if (uri != null) {
            try {
                resolver.openOutputStream(uri)?.use { dst ->
                    sourceFile.inputStream().use { src ->
                        src.copyTo(dst)
                    }
                }
                val doneValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.IS_PENDING, 0)
                }
                resolver.update(uri, doneValues, null, null)
                return "$relativeDir/$fileName"
            } catch (_: Exception) {
                resolver.delete(uri, null, null)
            }
        }
        return "App Cache ($fileName)"
    }

    /**
     * Pure-Kotlin Uncompressed Baseline TIFF Encoder (Little-Endian RGBA 8-bit/channel).
     * Zero compression: every pixel is stored verbatim in raw strip bytes.
     */
    private fun writeUncompressedTiff(bitmap: Bitmap, out: OutputStream) {
        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

        val stripByteCount = w * h * 4
        val headerSize = 8
        val numEntries = 12
        val ifdSize = 2 + numEntries * 12 + 4
        val bitsPerSampleOffset = headerSize + ifdSize
        val xResOffset = bitsPerSampleOffset + 8
        val yResOffset = xResOffset + 8
        val stripDataOffset = yResOffset + 8

        val headerAndIfd = ByteBuffer.allocate(stripDataOffset).order(ByteOrder.LITTLE_ENDIAN)
        // TIFF Header: "II", 42, IFD0 offset = 8
        headerAndIfd.put('I'.code.toByte())
        headerAndIfd.put('I'.code.toByte())
        headerAndIfd.putShort(42)
        headerAndIfd.putInt(headerSize)

        fun putTag(tag: Int, type: Int, count: Int, valueOrOffset: Int) {
            headerAndIfd.putShort(tag.toShort())
            headerAndIfd.putShort(type.toShort())
            headerAndIfd.putInt(count)
            headerAndIfd.putInt(valueOrOffset)
        }

        // IFD entries (must be sorted by tag ascending)
        headerAndIfd.putShort(numEntries.toShort())
        putTag(256, 4, 1, w)                  // ImageWidth (LONG)
        putTag(257, 4, 1, h)                  // ImageLength (LONG)
        putTag(258, 3, 4, bitsPerSampleOffset)// BitsPerSample: 8,8,8,8 (SHORT[4])
        putTag(259, 3, 1, 1)                  // Compression = 1 (Uncompressed)
        putTag(262, 3, 1, 2)                  // PhotometricInterpretation = 2 (RGB)
        putTag(273, 4, 1, stripDataOffset)    // StripOffsets
        putTag(277, 3, 1, 4)                  // SamplesPerPixel = 4 (RGBA)
        putTag(278, 4, 1, h)                  // RowsPerStrip = ImageLength
        putTag(279, 4, 1, stripByteCount)     // StripByteCounts
        putTag(282, 5, 1, xResOffset)         // XResolution (RATIONAL)
        putTag(283, 5, 1, yResOffset)         // YResolution (RATIONAL)
        putTag(296, 3, 1, 2)                  // ResolutionUnit = 2 (Inch)
        headerAndIfd.putInt(0)                // Next IFD = 0

        // Extra IFD values: BitsPerSample [8,8,8,8]
        headerAndIfd.putShort(8)
        headerAndIfd.putShort(8)
        headerAndIfd.putShort(8)
        headerAndIfd.putShort(8)
        // XResolution 300/1 dpi
        headerAndIfd.putInt(300)
        headerAndIfd.putInt(1)
        // YResolution 300/1 dpi
        headerAndIfd.putInt(300)
        headerAndIfd.putInt(1)

        out.write(headerAndIfd.array())

        // Write raw uncompressed RGBA rows in buffered chunks
        val rowBuffer = ByteArray(w * 4)
        for (y in 0 until h) {
            val rowStart = y * w
            var bIdx = 0
            for (x in 0 until w) {
                val c = pixels[rowStart + x]
                rowBuffer[bIdx++] = ((c ushr 16) and 0xFF).toByte() // R
                rowBuffer[bIdx++] = ((c ushr 8) and 0xFF).toByte()  // G
                rowBuffer[bIdx++] = (c and 0xFF).toByte()           // B
                rowBuffer[bIdx++] = ((c ushr 24) and 0xFF).toByte() // A
            }
            out.write(rowBuffer)
        }
    }
}
