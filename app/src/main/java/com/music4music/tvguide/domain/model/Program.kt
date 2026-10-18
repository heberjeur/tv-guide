package com.music4music.tvguide.domain.model

data class Program(
    val id: Long = 0,
    val channelId: String,
    val startTimestamp: Long,
    val stopTimestamp: Long,
    val title: String,
    val subTitle: String? = null,
    val description: String = "",
    val secondaryDescription: String? = null,
    val categories: List<String> = emptyList(),
    val iconUrl: String? = null,
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null,
    val episodeDisplay: String? = null,
    val credit: Credit = Credit(),
    val contentRating: String? = null,
    val starRating: Float? = null,
    val isUnreleased: Boolean = false,
    val hasSubtitles: Boolean = false,
    val isHd: Boolean = false,
    val durationMinutes: Int = 0,
    val productionYear: Int? = null,
    val seriesYear: Int? = null,
    val country: String? = null
) {
    val isNowPlaying: Boolean
        get() {
            val now = System.currentTimeMillis()
            return now in startTimestamp until stopTimestamp
        }

    val progressPercent: Float
        get() {
            val now = System.currentTimeMillis()
            if (now < startTimestamp) return 0f
            if (now >= stopTimestamp) return 1f
            val total = stopTimestamp - startTimestamp
            if (total <= 0) return 0f
            return (now - startTimestamp).toFloat() / total.toFloat()
        }

    val formattedDuration: String
        get() = formatDuration(durationMinutes)
}

fun formatDuration(minutes: Int): String {
    if (minutes <= 0) return ""
    val hours = minutes / 60
    val remainingMinutes = minutes % 60
    return when {
        hours > 0 && remainingMinutes > 0 -> "$hours h $remainingMinutes min"
        hours > 0 -> "$hours h"
        else -> "$remainingMinutes min"
    }
}
