package com.example.ai

/**
 * Initial Real-ESRGAN model catalog for PhotoStudio.
 *
 * Model weights are intentionally not bundled in the APK. Download/install and native
 * inference are separate roadmap milestones; these entries are metadata only.
 */
enum class RealEsrganModelType {
    PHOTO, ANIME, FAST
}

data class RealEsrganModelSpec(
    val id: String,
    val displayName: String,
    val description: String,
    val scale: Int,
    val approximateDownloadMb: Float,
    val type: RealEsrganModelType,
    val recommended: Boolean = false,
    val upstreamModelName: String,
    val paramFileName: String,
    val binFileName: String,
    val paramUrl: String,
    val binUrl: String
)

object RealEsrganModelCatalog {
    val models: List<RealEsrganModelSpec> = listOf(
        RealEsrganModelSpec(
            id = "realesrgan-x4plus",
            displayName = "Real-ESRGAN x4plus Photo",
            description = "General-purpose photo restoration and 4× super-resolution.",
            scale = 4,
            approximateDownloadMb = 33.4f,
            type = RealEsrganModelType.PHOTO,
            recommended = true,
            upstreamModelName = "realesrgan-x4plus",
            paramFileName = "realesrgan-x4plus.param",
            binFileName = "realesrgan-x4plus.bin",
            paramUrl = "https://huggingface.co/edgetools/realesrgan/resolve/main/realesrgan-x4plus.param",
            binUrl = "https://huggingface.co/edgetools/realesrgan/resolve/main/realesrgan-x4plus.bin"
        ),
        RealEsrganModelSpec(
            id = "realesrgan-x4plus-anime",
            displayName = "Real-ESRGAN Anime x4",
            description = "Optimized for anime, illustration and stylized artwork.",
            scale = 4,
            approximateDownloadMb = 8.9f,
            type = RealEsrganModelType.ANIME,
            upstreamModelName = "realesrgan-x4plus-anime",
            paramFileName = "realesrgan-x4plus-anime.param",
            binFileName = "realesrgan-x4plus-anime.bin",
            paramUrl = "https://huggingface.co/spaces/KudoSin/AyayaUpscale/resolve/main/models/xintao-realesrgan-x4plus-anime.param",
            binUrl = "https://huggingface.co/spaces/KudoSin/AyayaUpscale/resolve/main/models/xintao-realesrgan-x4plus-anime.bin"
        ),
        RealEsrganModelSpec(
            id = "realesr-animevideov3-x2",
            displayName = "AnimeVideo v3 Fast x2",
            description = "Lightweight model option intended for faster, lower-cost processing.",
            scale = 2,
            approximateDownloadMb = 1.2f,
            type = RealEsrganModelType.FAST,
            upstreamModelName = "realesr-animevideov3-x2",
            paramFileName = "realesr-animevideov3-x2.param",
            binFileName = "realesr-animevideov3-x2.bin",
            paramUrl = "https://huggingface.co/edgetools/realesrgan/resolve/main/realesr-animevideov3-x2.param",
            binUrl = "https://huggingface.co/edgetools/realesrgan/resolve/main/realesr-animevideov3-x2.bin"
        )
    )

    fun find(id: String): RealEsrganModelSpec? = models.firstOrNull { it.id == id }

    val totalSelectedModelSizeMb: Float = models.sumOf { it.approximateDownloadMb.toDouble() }.toFloat()
}
