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
    val contentCategories: List<String>
        get() = categories.filter { cat ->
            val c = cat.trim().lowercase()
            c.isNotEmpty() && !isSignaletique(c)
        }.distinctBy { it.trim().lowercase() }

    val hasAudioDescription: Boolean
        get() = description.contains("audiodescription", ignoreCase = true) ||
                description.contains("audio-description", ignoreCase = true) ||
                (subTitle?.contains("audiodescription", ignoreCase = true) == true) ||
                (subTitle?.contains("audio-description", ignoreCase = true) == true) ||
                categories.any {
                    it.contains("audiodescription", ignoreCase = true) ||
                    it.contains("audio-description", ignoreCase = true) ||
                    it.contains("audio description", ignoreCase = true)
                }

    val hasSubtitlesResolved: Boolean
        get() = hasSubtitles ||
                description.contains("sous-titr", ignoreCase = true) ||
                description.contains("malentendant", ignoreCase = true) ||
                description.contains("VOST", ignoreCase = true) ||
                description.contains("SME", ignoreCase = false) ||
                description.contains("[ST]", ignoreCase = true) ||
                description.contains("(ST)", ignoreCase = true) ||
                (subTitle?.contains("sous-titr", ignoreCase = true) == true) ||
                (subTitle?.contains("malentendant", ignoreCase = true) == true) ||
                (subTitle?.contains("VOST", ignoreCase = true) == true) ||
                categories.any { it.contains("sous-titre", ignoreCase = true) || it.contains("malentendant", ignoreCase = true) }

    val isRerun: Boolean
        get() = categories.any { it.equals("rediffusion", ignoreCase = true) || it.equals("rediff", ignoreCase = true) } ||
            title.contains("rediffusion", ignoreCase = true) ||
            title.contains("(rediff", ignoreCase = true) ||
            title.contains("[rediff", ignoreCase = true) ||
            title.contains("(r)", ignoreCase = true) ||
            title.contains("[r]", ignoreCase = true) ||
            (subTitle?.contains("rediffusion", ignoreCase = true) == true) ||
            (subTitle?.contains("(rediff", ignoreCase = true) == true) ||
            (subTitle?.contains("[rediff", ignoreCase = true) == true) ||
            (subTitle?.contains("(r)", ignoreCase = true) == true) ||
            description.contains("rediffusion", ignoreCase = true) ||
            description.contains("(rediff", ignoreCase = true) ||
            description.contains("[rediff", ignoreCase = true)

    val isUnreleasedResolved: Boolean
        get() = !isRerun && (
            isUnreleased ||
            title.contains("inédit", ignoreCase = true) ||
            (subTitle?.contains("inédit", ignoreCase = true) == true) ||
            description.contains("inédit", ignoreCase = true) ||
            description.contains("inédite", ignoreCase = true) ||
            categories.any { it.contains("inédit", ignoreCase = true) }
        )

    val isLiveBroadcast: Boolean
        get() = !isRerun && (
            title.contains("en direct", ignoreCase = true) ||
            title.contains("[direct]", ignoreCase = true) ||
            title.contains("(direct)", ignoreCase = true) ||
            (subTitle?.contains("en direct", ignoreCase = true) == true) ||
            categories.any { it.equals("direct", ignoreCase = true) || it.equals("en direct", ignoreCase = true) }
        )

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

fun isSignaletique(category: String): Boolean {
    val clean = category.trim().lowercase()
    if (clean.isBlank()) return true
    val exactMatches = setOf(
        "rediffusion", "rediff", "r", "(r)", "[r]",
        "audiodescription", "audio-description", "audio description", "ad",
        "sous-titres", "sous-titres sourds et malentendants", "sous-titrage", "sous-titré", "sous-titrée",
        "st", "sme", "vost", "vostfr",
        "inédit", "inedit", "inédite", "inedite",
        "direct", "en direct", "live",
        "hd", "haute définition", "haute definition",
        "tout public", "tous publics", "tp", "u", "g", "csa", "-10", "-12", "-16", "-18",
        "general", "autre", "divers", "programme indéterminé"
    )
    if (clean in exactMatches) return true
    if (clean.startsWith("csa") || clean.startsWith("déconseillé") || clean.startsWith("interdit")) return true
    if (clean.contains("audiodescription") || clean.contains("audio-description")) return true
    if (clean.contains("rediffusion")) return true
    if (clean.contains("sous-titr") || clean.contains("malentendant")) return true
    return false
}
