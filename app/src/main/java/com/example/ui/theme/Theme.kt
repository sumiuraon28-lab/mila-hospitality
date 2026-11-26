package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val MilaLuxuryColorScheme = darkColorScheme(
    primary = MilaChampagneGold,
    onPrimary = MilaCharcoalDeep,
    primaryContainer = MilaCharcoalElevated,
    onPrimaryContainer = MilaChampagneGold,
    secondary = MilaGoldMuted,
    onSecondary = MilaCharcoalDeep,
    secondaryContainer = MilaCharcoalSurface,
    onSecondaryContainer = MilaIvory,
    tertiary = MilaIvory,
    onTertiary = MilaCharcoal,
    background = MilaCharcoal,
    onBackground = MilaIvory,
    surface = MilaCharcoal,
    onSurface = MilaIvory,
    surfaceVariant = MilaCharcoalSurface,
    onSurfaceVariant = MilaIvoryMuted,
    surfaceContainerLowest = MilaCharcoalDeep,
    surfaceContainerLow = MilaCharcoal,
    surfaceContainer = MilaCharcoalSurface,
    surfaceContainerHigh = MilaCharcoalElevated,
    outline = MilaCharcoalBorder,
    outlineVariant = MilaGoldSubtle,
    error = MilaTerracotta,
    onError = MilaIvory
)

val MilaShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun MilaHospitalityTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = MilaLuxuryColorScheme,
        typography = MilaTypography,
        shapes = MilaShapes,
        content = content
    )
}
