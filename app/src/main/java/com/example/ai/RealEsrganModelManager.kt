package com.example.ai

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Downloads selected NCNN model pairs into app-private storage.
 * Model weights are not APK assets and remain local/offline after installation.
 */
class RealEsrganModelManager(context: Context) {
    private val root = File(context.filesDir, "ai_models/realesrgan").apply { mkdirs() }

    data class InstalledModel(
        val spec: RealEsrganModelSpec,
        val paramFile: File,
        val binFile: File
    )

    fun isInstalled(spec: RealEsrganModelSpec): Boolean {
        val dir = modelDir(spec)
        val param = File(dir, spec.paramFileName)
        val bin = File(dir, spec.binFileName)
        return param.isFile && param.length() > 100 && bin.isFile && bin.length() > 1024
    }

    fun installedModels(): List<RealEsrganModelSpec> = RealEsrganModelCatalog.models.filter(::isInstalled)

    fun getInstalled(spec: RealEsrganModelSpec): InstalledModel? {
        if (!isInstalled(spec)) return null
        val dir = modelDir(spec)
        return InstalledModel(spec, File(dir, spec.paramFileName), File(dir, spec.binFileName))
    }

    suspend fun install(
        spec: RealEsrganModelSpec,
        onProgress: (fileName: String, percent: Int) -> Unit = { _, _ -> }
    ): InstalledModel = withContext(Dispatchers.IO) {
        require(spec.paramUrl.startsWith("https://") && spec.binUrl.startsWith("https://")) {
            "Model download must use HTTPS."
        }
        val dir = modelDir(spec).apply { mkdirs() }
        val param = File(dir, spec.paramFileName)
        val bin = File(dir, spec.binFileName)
        try {
            downloadAtomically(spec.paramUrl, param) { onProgress(spec.paramFileName, it) }
            downloadAtomically(spec.binUrl, bin) { onProgress(spec.binFileName, it) }
            require(param.length() > 100 && bin.length() > 1024) { "Downloaded model files are incomplete." }
            val pairSizeMb = (param.length() + bin.length()) / (1024f * 1024f)
            require(pairSizeMb >= spec.approximateDownloadMb * 0.70f) {
                "Model download appears truncated (${pairSizeMb} MB). Please retry."
            }
            InstalledModel(spec, param, bin)
        } catch (error: Throwable) {
            param.delete()
            bin.delete()
            throw error
        }
    }

    fun remove(spec: RealEsrganModelSpec): Boolean = modelDir(spec).deleteRecursively()

    private fun modelDir(spec: RealEsrganModelSpec) = File(root, spec.id)

    private fun downloadAtomically(urlString: String, destination: File, progress: (Int) -> Unit) {
        val temp = File(destination.parentFile, destination.name + ".part")
        temp.delete()
        val connection = (URL(urlString).openConnection() as HttpURLConnection).apply {
            connectTimeout = 20_000
            readTimeout = 30_000
            instanceFollowRedirects = true
            requestMethod = "GET"
            setRequestProperty("User-Agent", "PhotoStudio-RealESRGAN/1.0")
        }
        try {
            connection.connect()
            val code = connection.responseCode
            require(code in 200..299) { "Download HTTP $code for ${destination.name}" }
            val total = connection.contentLengthLong
            connection.inputStream.use { input ->
                temp.outputStream().buffered().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var downloaded = 0L
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        output.write(buffer, 0, count)
                        downloaded += count
                        if (total > 0) progress(((downloaded * 100) / total).toInt().coerceIn(0, 100))
                    }
                }
            }
            require(temp.length() > 100) { "Downloaded file is empty or truncated." }
            if (destination.exists()) destination.delete()
            check(temp.renameTo(destination)) { "Could not finalize ${destination.name}" }
            progress(100)
        } catch (error: Throwable) {
            temp.delete()
            throw error
        } finally {
            connection.disconnect()
        }
    }
}
