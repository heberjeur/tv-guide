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

        val seasonGroups = baseMapped.groupBy { it.seasonNumber }
        val adjustedEpisodes = mutableMapOf<Long, Program>()

        for ((season, group) in seasonGroups) {
            if (season == null) {
                var prev: Program? = null
                for (p in group) {
                    var current = p
                    if (prev != null &&
                        prev.channelId == p.channelId &&
                        (p.startTimestamp - prev.stopTimestamp) in -300_000L..1_800_000L
                    ) {
                        val prevEp = prev.episodeNumber
                        val currEp = p.episodeNumber
                        if (prevEp != null && (currEp == null || currEp <= prevEp)) {
                            current = p.copy(episodeNumber = prevEp + 1)
                        }
                    }
                    adjustedEpisodes[current.id] = current
                    prev = current
                }
            } else {
                val seenSubtitles = mutableMapOf<String, Int>()
                var maxEp = 0
                var prev: Program? = null

                for (p in group) {
                    val cleanSub = cleanSubtitle(p.subTitle, p.title)?.lowercase()
                    val assignedEp: Int?

                    if (!cleanSub.isNullOrBlank()) {
                        if (seenSubtitles.containsKey(cleanSub)) {
                            assignedEp = seenSubtitles[cleanSub]
                        } else {
                            val origEp = p.episodeNumber
                            maxEp = if (origEp != null && origEp > maxEp) {
                                origEp
                            } else if (maxEp > 0) {
                                maxEp + 1
                            } else {
                                origEp ?: 1
                            }
                            seenSubtitles[cleanSub] = maxEp
                            assignedEp = maxEp
                        }
                    } else {
                        val prevEp = prev?.episodeNumber
                        val currEp = p.episodeNumber
                        val isConsecutive = prev != null &&
                            prev.channelId == p.channelId &&
                            (p.startTimestamp - prev.stopTimestamp) in -300_000L..1_800_000L
                        if (isConsecutive && prevEp != null && (currEp == null || currEp <= prevEp)) {
                            maxEp = maxOf(maxEp, prevEp) + 1
                            assignedEp = maxEp
                        } else if (currEp != null && currEp > maxEp) {
                            maxEp = currEp
                            assignedEp = currEp
                        } else if (currEp != null) {
                            assignedEp = currEp
                        } else if (maxEp > 0) {
                            maxEp += 1
                            assignedEp = maxEp
                        } else {
                            assignedEp = null
                        }
                    }

                    val updated = if (assignedEp != p.episodeNumber) {
                        p.copy(episodeNumber = assignedEp)
                    } else {
                        p
                    }
                    adjustedEpisodes[updated.id] = updated
                    prev = updated
                }
            }
        }

        return baseMapped.map { adjustedEpisodes[it.id] ?: it }
    }
}
