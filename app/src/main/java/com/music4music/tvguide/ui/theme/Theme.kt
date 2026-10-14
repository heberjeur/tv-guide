package com.music4music.tvguide.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = AccentRed,
    secondary = AccentRedVariant,
    background = RawDarkBackground,
    surface = RawDarkSurface,
    surfaceVariant = RawDarkSurfaceVariant,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = RawTextPrimary,
    onSurface = RawTextPrimary,
    onSurfaceVariant = RawTextSecondary,
    outline = RawSurfaceBorder
)

private val OledColorScheme = darkColorScheme(
    primary = AccentRed,
    secondary = AccentRedVariant,
    background = Color.Black,
    surface = Color(0xFF0F0F0F),
    surfaceVariant = Color(0xFF1E1E1E),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = RawTextPrimary,
    onSurface = RawTextPrimary,
    onSurfaceVariant = RawTextSecondary,
    outline = Color(0xFF2E2E2E)
)

private val LightColorScheme = lightColorScheme(
    primary = AccentRed,
    secondary = AccentRedVariant,
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = LightTextPrimary,
    onSurface = LightTextPrimary,
    onSurfaceVariant = LightTextSecondary,
    outline = Color(0xFFCCCCCC)
)

val LocalDisplayDensity = compositionLocalOf { "comfort" }
val LocalHighlightAccessibility = compositionLocalOf { true }
val LocalFontSizeScale = compositionLocalOf { "normal" }

private fun scaledTypography(scale: Float): Typography {
    val default = Typography()
    if (scale == 1.0f) return default
    return Typography(
        displayLarge = default.displayLarge.copy(fontSize = default.displayLarge.fontSize * scale),
        displayMedium = default.displayMedium.copy(fontSize = default.displayMedium.fontSize * scale),
        displaySmall = default.displaySmall.copy(fontSize = default.displaySmall.fontSize * scale),
        headlineLarge = default.headlineLarge.copy(fontSize = default.headlineLarge.fontSize * scale),
        headlineMedium = default.headlineMedium.copy(fontSize = default.headlineMedium.fontSize * scale),
        headlineSmall = default.headlineSmall.copy(fontSize = default.headlineSmall.fontSize * scale),
        titleLarge = default.titleLarge.copy(fontSize = default.titleLarge.fontSize * scale),
        titleMedium = default.titleMedium.copy(fontSize = default.titleMedium.fontSize * scale),
        titleSmall = default.titleSmall.copy(fontSize = default.titleSmall.fontSize * scale),
        bodyLarge = default.bodyLarge.copy(fontSize = default.bodyLarge.fontSize * scale),
        bodyMedium = default.bodyMedium.copy(fontSize = default.bodyMedium.fontSize * scale),
        bodySmall = default.bodySmall.copy(fontSize = default.bodySmall.fontSize * scale),
        labelLarge = default.labelLarge.copy(fontSize = default.labelLarge.fontSize * scale),
        labelMedium = default.labelMedium.copy(fontSize = default.labelMedium.fontSize * scale),
        labelSmall = default.labelSmall.copy(fontSize = default.labelSmall.fontSize * scale)
    )
}

@Composable
fun TvGuideTheme(
    themeMode: String = "dark",
    fontSizeScale: String = "normal",
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val colorScheme = when (themeMode) {
        "oled" -> OledColorScheme
        "light" -> LightColorScheme
        "system" -> if (isSystemDark) DarkColorScheme else LightColorScheme
        else -> DarkColorScheme
    }

    val scale = if (fontSizeScale == "large") 1.15f else 1.0f

    MaterialTheme(
        colorScheme = colorScheme,
        typography = scaledTypography(scale),
        content = content
    )
}
