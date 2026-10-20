package com.music4music.tvguide.util

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.music4music.tvguide.R

data class AccentColorPreset(
    val id: String,
    @StringRes val nameRes: Int,
    val hex: String,
    val color: Color
)

object ColorUtils {

    val DefaultAccent = Color(0xFFE50914)

    val Presets = listOf(
        AccentColorPreset("red", R.string.color_red, "#E50914", Color(0xFFE50914)),
        AccentColorPreset("blue", R.string.color_blue, "#1E88E5", Color(0xFF1E88E5)),
        AccentColorPreset("purple", R.string.color_purple, "#7C4DFF", Color(0xFF7C4DFF)),
        AccentColorPreset("green", R.string.color_green, "#00C853", Color(0xFF00C853)),
        AccentColorPreset("orange", R.string.color_orange, "#FF6D00", Color(0xFFFF6D00)),
        AccentColorPreset("pink", R.string.color_pink, "#FF2A6D", Color(0xFFFF2A6D)),
        AccentColorPreset("cyan", R.string.color_cyan, "#00B4D8", Color(0xFF00B4D8)),
        AccentColorPreset("gold", R.string.color_gold, "#FFB300", Color(0xFFFFB300))
    )

    fun parseHexColor(hex: String?, fallback: Color = DefaultAccent): Color {
        if (hex.isNullOrBlank()) return fallback
        return try {
            val clean = hex.trim().removePrefix("#")
            when (clean.length) {
                6 -> Color(android.graphics.Color.parseColor("#$clean"))
                8 -> Color(android.graphics.Color.parseColor("#$clean"))
                else -> fallback
            }
        } catch (_: Exception) {
            fallback
        }
    }

    fun computeVariant(color: Color): Color {
        return Color(
            red = (color.red * 0.78f).coerceIn(0f, 1f),
            green = (color.green * 0.78f).coerceIn(0f, 1f),
            blue = (color.blue * 0.78f).coerceIn(0f, 1f),
            alpha = color.alpha
        )
    }

    fun toHex(color: Color): String {
        val r = (color.red * 255).toInt().coerceIn(0, 255)
        val g = (color.green * 255).toInt().coerceIn(0, 255)
        val b = (color.blue * 255).toInt().coerceIn(0, 255)
        return String.format(java.util.Locale.ROOT, "#%02X%02X%02X", r, g, b)
    }
}
