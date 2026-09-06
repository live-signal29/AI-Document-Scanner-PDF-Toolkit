package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = HighDensityPrimaryDark,
    onPrimary = HighDensityOnPrimaryDark,
    primaryContainer = HighDensityPrimaryContainerDark,
    onPrimaryContainer = HighDensityOnPrimaryContainerDark,
    secondary = HighDensitySecondaryDark,
    onSecondary = HighDensityOnSecondaryDark,
    secondaryContainer = HighDensitySecondaryContainerDark,
    onSecondaryContainer = HighDensityOnSecondaryContainerDark,
    tertiary = HighDensityTertiaryDark,
    onTertiary = HighDensityOnTertiaryDark,
    tertiaryContainer = HighDensityTertiaryContainerDark,
    onTertiaryContainer = HighDensityOnTertiaryContainerDark,
    background = HighDensityBackgroundDark,
    onBackground = HighDensityOnBackgroundDark,
    surface = HighDensitySurfaceDark,
    onSurface = HighDensityOnSurfaceDark,
    surfaceVariant = HighDensitySurfaceVariantDark,
    onSurfaceVariant = HighDensityOnSurfaceVariantDark,
    outline = HighDensityOutlineDark
)

private val LightColorScheme = lightColorScheme(
    primary = HighDensityPrimary,
    onPrimary = HighDensityOnPrimary,
    primaryContainer = HighDensityPrimaryContainer,
    onPrimaryContainer = HighDensityOnPrimaryContainer,
    secondary = HighDensitySecondary,
    onSecondary = HighDensityOnSecondary,
    secondaryContainer = HighDensitySecondaryContainer,
    onSecondaryContainer = HighDensityOnSecondaryContainer,
    tertiary = HighDensityTertiary,
    onTertiary = HighDensityOnTertiary,
    tertiaryContainer = HighDensityTertiaryContainer,
    onTertiaryContainer = HighDensityOnTertiaryContainer,
    background = HighDensityBackgroundLight,
    onBackground = HighDensityOnBackgroundLight,
    surface = HighDensitySurfaceLight,
    onSurface = HighDensityOnSurfaceLight,
    surfaceVariant = HighDensitySurfaceVariantLight,
    onSurfaceVariant = HighDensityOnSurfaceVariantLight,
    outline = HighDensityOutlineLight
)

@Composable
fun AIPdfScannerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// Keep backwards-compatible alias for existing templates
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) = AIPdfScannerTheme(darkTheme, dynamicColor, content)
