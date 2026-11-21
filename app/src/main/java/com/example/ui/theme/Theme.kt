package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

private val PhotoStudioDarkColorScheme = darkColorScheme(
    primary = CyanPrecision,
    onPrimary = ObsidianVoid,
    primaryContainer = CyanMuted,
    onPrimaryContainer = TextPrimary,
    secondary = AmberWarm,
    onSecondary = ObsidianVoid,
    secondaryContainer = AmberMuted,
    onSecondaryContainer = TextPrimary,
    tertiary = EmeraldSuccess,
    onTertiary = ObsidianVoid,
    background = ObsidianVoid,
    onBackground = TextPrimary,
    surface = StudioSurface,
    onSurface = TextPrimary,
    surfaceVariant = StudioPanel,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLow = StudioSurface,
    surfaceContainer = StudioPanel,
    surfaceContainerHigh = StudioElevated,
    outline = StudioHairline,
    outlineVariant = StudioHairline.copy(alpha = 0.5f),
    error = RoseMask,
    onError = ObsidianVoid
)

val PhotoStudioShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(26.dp)
)

@Composable
fun PhotoStudioTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = PhotoStudioDarkColorScheme,
        typography = LuminaTypography,
        shapes = PhotoStudioShapes,
        content = content
    )
}