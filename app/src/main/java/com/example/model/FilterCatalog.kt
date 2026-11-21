package com.example.model

object FilterCatalog {
    val presets: List<FilterPreset> = listOf(
        FilterPreset(
            id = "original",
            name = "Original",
            category = "Base"
        ),
        FilterPreset(
            id = "portra_warm",
            name = "Portra 400",
            category = "Film",
            exposureDelta = 0.12f,
            contrastDelta = 8f,
            saturationDelta = 6f,
            temperatureDelta = 14f,
            tintDelta = 4f,
            fadeBlacks = 0.04f,
            highlightTintR = 0.05f,
            highlightTintG = 0.02f
        ),
        FilterPreset(
            id = "fuji_velvet",
            name = "Fuji Velvet",
            category = "Film",
            contrastDelta = 18f,
            saturationDelta = 24f,
            temperatureDelta = -6f,
            tintDelta = -5f,
            shadowTintB = 0.05f,
            shadowTintG = 0.02f
        ),
        FilterPreset(
            id = "teal_orange",
            name = "Cinema T&O",
            category = "Cinema",
            contrastDelta = 22f,
            saturationDelta = 10f,
            temperatureDelta = 8f,
            shadowTintR = -0.07f,
            shadowTintG = 0.03f,
            shadowTintB = 0.09f,
            highlightTintR = 0.09f,
            highlightTintG = 0.03f,
            highlightTintB = -0.06f
        ),
        FilterPreset(
            id = "nordic_matte",
            name = "Nordic Matte",
            category = "Editorial",
            exposureDelta = 0.08f,
            contrastDelta = -12f,
            saturationDelta = -26f,
            temperatureDelta = -16f,
            fadeBlacks = 0.11f,
            shadowTintB = 0.04f
        ),
        FilterPreset(
            id = "golden_hour",
            name = "Golden Hour",
            category = "Warm",
            exposureDelta = 0.15f,
            contrastDelta = 10f,
            saturationDelta = 18f,
            temperatureDelta = 28f,
            tintDelta = 6f,
            highlightTintR = 0.08f,
            highlightTintG = 0.04f
        ),
        FilterPreset(
            id = "moody_obsidian",
            name = "Moody Dark",
            category = "Cinema",
            exposureDelta = -0.22f,
            contrastDelta = 28f,
            saturationDelta = -18f,
            temperatureDelta = -8f,
            fadeBlacks = 0.05f,
            shadowTintB = 0.04f
        ),
        FilterPreset(
            id = "kodak_trix",
            name = "Tri-X B&W",
            category = "Mono",
            contrastDelta = 34f,
            fadeBlacks = 0.03f,
            isMonochrome = true
        ),
        FilterPreset(
            id = "silver_noir",
            name = "Silver Noir",
            category = "Mono",
            exposureDelta = -0.10f,
            contrastDelta = 46f,
            isMonochrome = true
        ),
        FilterPreset(
            id = "crisp_daylight",
            name = "Crisp HDR",
            category = "Editorial",
            exposureDelta = 0.06f,
            contrastDelta = 20f,
            saturationDelta = 16f,
            temperatureDelta = -4f
        ),
        FilterPreset(
            id = "pastel_air",
            name = "Pastel Air",
            category = "Editorial",
            exposureDelta = 0.25f,
            contrastDelta = -14f,
            saturationDelta = -12f,
            tintDelta = 8f,
            fadeBlacks = 0.09f,
            highlightTintR = 0.04f,
            highlightTintB = 0.05f
        ),
        FilterPreset(
            id = "vintage_sepia",
            name = "Archival Sepia",
            category = "Film",
            contrastDelta = 14f,
            saturationDelta = -55f,
            temperatureDelta = 35f,
            fadeBlacks = 0.07f,
            shadowTintR = 0.06f,
            shadowTintG = 0.02f,
            highlightTintR = 0.08f,
            highlightTintG = 0.04f
        )
    )

    val curvePresets: List<Pair<String, List<CurvePoint>>> = listOf(
        "Linear" to listOf(CurvePoint(0f, 0f), CurvePoint(1f, 1f)),
        "Medium S" to listOf(
            CurvePoint(0f, 0f),
            CurvePoint(0.25f, 0.20f),
            CurvePoint(0.50f, 0.50f),
            CurvePoint(0.75f, 0.80f),
            CurvePoint(1f, 1f)
        ),
        "Strong S" to listOf(
            CurvePoint(0f, 0f),
            CurvePoint(0.22f, 0.14f),
            CurvePoint(0.50f, 0.50f),
            CurvePoint(0.78f, 0.86f),
            CurvePoint(1f, 1f)
        ),
        "Matte Fade" to listOf(
            CurvePoint(0f, 0.12f),
            CurvePoint(0.28f, 0.25f),
            CurvePoint(0.58f, 0.58f),
            CurvePoint(1f, 0.94f)
        ),
        "Soft Lift" to listOf(
            CurvePoint(0f, 0.06f),
            CurvePoint(0.35f, 0.42f),
            CurvePoint(0.70f, 0.76f),
            CurvePoint(1f, 1f)
        )
    )
}
