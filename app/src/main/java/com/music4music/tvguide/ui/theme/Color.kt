package com.music4music.tvguide.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

val RawDarkBackground = Color(0xFF121212)
val RawDarkSurface = Color(0xFF1E1E1E)
val RawDarkSurfaceVariant = Color(0xFF2A2A2A)
val AccentRed = Color(0xFFE50914)
val AccentRedVariant = Color(0xFFB20710)
val RawTextPrimary = Color(0xFFF5F5F5)
val RawTextSecondary = Color(0xFFAAAAAA)
val IndicatorGreen = Color(0xFF4CAF50)
val RawSurfaceBorder = Color(0xFF383838)

val LightBackground = Color(0xFFF8F9FA)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFEEEEEE)
val LightTextPrimary = Color(0xFF1A1A1A)
val LightTextSecondary = Color(0xFF666666)

val DarkBackground: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.background

val DarkSurface: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.surface

val DarkSurfaceVariant: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.surfaceVariant

val TextPrimary: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.onSurface

val TextSecondary: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.onSurfaceVariant

val SurfaceBorder: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.outline
