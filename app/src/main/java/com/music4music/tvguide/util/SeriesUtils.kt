package com.music4music.tvguide.util

import com.music4music.tvguide.domain.model.Program

object SeriesUtils {

    private val episodeRegex = Regex("""(?:[ÉéE]pisode|[ÉéE]p\.?)\s*(\d+)""", RegexOption.IGNORE_CASE)
    private val seasonRegex = Regex("""Saisons?\s*(\d+)""", RegexOption.IGNORE_CASE)
    private val seasonPrefixRegex = Regex("""^(?:(?:Saison|Season|S)\s*\d+\s*[-:•]?\s*|(?:[ÉéE]pisode|[ÉéE]p\.?|E)\s*\d+\s*[-:•]?\s*)+""", RegexOption.IGNORE_CASE)
    private val seasonSuffixRegex = Regex("""\s*[-:•(]?\s*(?:(?:Saison|Season|S)\s*\d+|(?:[ÉéE]pisode|[ÉéE]p\.?|E)\s*\d+)\s*\)?$""", RegexOption.IGNORE_CASE)

    fun cleanSubtitle(subTitle: String?, seriesTitle: String? = null): String? {
        val raw = subTitle?.trim()
        if (raw.isNullOrBlank()) return null
        val stripped = raw
            .replace(seasonPrefixRegex, "")
            .replace(seasonSuffixRegex, "")
            .trim()
        if (stripped.isEmpty()) return null
        if (seriesTitle != null && stripped.equals(seriesTitle.trim(), ignoreCase = true)) return null
        return stripped
    }

    fun normalizeEpisodes(programs: List<Program>): List<Program> {
        val baseMapped = programs.sortedBy { it.startTimestamp }.map { p ->
            val epFromSub = episodeRegex.find(p.subTitle ?: "")?.groupValues?.get(1)?.toIntOrNull()
            val seasonFromSub = seasonRegex.find(p.subTitle ?: "")?.groupValues?.get(1)?.toIntOrNull()
            if (epFromSub != null || seasonFromSub != null) {
                p.copy(
                    episodeNumber = epFromSub ?: p.episodeNumber,
                    seasonNumber = seasonFromSub ?: p.seasonNumber
                )
            } else {
                p
            }
        }

        val result = mutableListOf<Program>()
        for (p in baseMapped) {
            if (result.isNotEmpty()) {
                val prev = result.last()
                val sameChannel = prev.channelId == p.channelId
                val sameSeason = prev.seasonNumber != null && prev.seasonNumber == p.seasonNumber
                val diffSubtitle = !prev.subTitle.isNullOrBlank() && !p.subTitle.isNullOrBlank() &&
                    !prev.subTitle.trim().equals(p.subTitle.trim(), ignoreCase = true)
                val consecutiveTime = (p.startTimestamp - prev.stopTimestamp) in -300_000L..1_800_000L
                val prevEp = prev.episodeNumber
                val currEp = p.episodeNumber
                if (sameChannel && sameSeason && diffSubtitle && consecutiveTime && prevEp != null && (currEp == null || currEp <= prevEp)) {
                    result.add(p.copy(episodeNumber = prevEp + 1))
                    continue
                }
            }
            result.add(p)
        }
        return result
    }
}
