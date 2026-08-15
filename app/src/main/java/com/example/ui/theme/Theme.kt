package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = SaffronLight,
    onPrimary = Color(0xFF451A03),
    primaryContainer = CivicNavy800,
    onPrimaryContainer = SaffronContainer,
    secondary = EmeraldLight,
    onSecondary = Color(0xFF054F31),
    secondaryContainer = Color(0xFF065F46),
    onSecondaryContainer = EmeraldContainer,
    tertiary = CivicNavy100,
    onTertiary = CivicNavy900,
    background = BackgroundDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = BorderDark,
    error = CrimsonPrimary,
    errorContainer = Color(0xFF450A0A),
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = CivicNavy800,
    onPrimary = Color.White,
    primaryContainer = CivicNavy100,
    onPrimaryContainer = CivicNavy900,
    secondary = SaffronPrimary,
    onSecondary = Color.White,
    secondaryContainer = SaffronContainer,
    onSecondaryContainer = SaffronText,
    tertiary = EmeraldDark,
    onTertiary = Color.White,
    tertiaryContainer = EmeraldContainer,
    onTertiaryContainer = EmeraldText,
    background = BackgroundLight,
    onBackground = TextPrimaryLight,
    surface = SurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = TextSecondaryLight,
    outline = BorderLight,
    error = CrimsonPrimary,
    errorContainer = CrimsonContainer,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Use our handcrafted civic theme for consistent branding
    content: @Composable () -> Unit,
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
