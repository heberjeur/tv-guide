package com.music4music.tvguide.data.remote

import android.util.Xml
import com.music4music.tvguide.data.local.ChannelCatalog
import com.music4music.tvguide.data.local.entity.ChannelEntity
import com.music4music.tvguide.data.local.entity.ProgramEntity
import com.music4music.tvguide.util.ProgramDescriptionUtils
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

private const val XML_TAG_CHANNEL = "channel"
private const val XML_TAG_PROGRAM = "programme"

@Singleton
class XmltvParser @Inject constructor() {

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss Z", Locale.ROOT)

    suspend fun parse(
        inputStream: InputStream,
        packageId: String,
        onBatchParsed: suspend (channels: List<ChannelEntity>, programs: List<ProgramEntity>) -> Unit
    ) {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(inputStream, "UTF-8")

        val channelsBatch = mutableListOf<ChannelEntity>()
        val programsBatch = mutableListOf<ProgramEntity>()
        val batchLimit = 500

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                when (parser.name) {
                    XML_TAG_CHANNEL -> {
                        val channel = parseChannel(parser, packageId)
                        if (channel != null) {
                            channelsBatch.add(channel)
                        }
                    }
                    XML_TAG_PROGRAM -> {
                        val program = parseProgram(parser)
                        if (program != null) {
                            programsBatch.add(program)
                            if (programsBatch.size >= batchLimit) {
                                onBatchParsed(channelsBatch.toList(), programsBatch.toList())
                                channelsBatch.clear()
                                programsBatch.clear()
                            }
                        }
                    }
                }
            }
            eventType = parser.next()
        }

        if (channelsBatch.isNotEmpty() || programsBatch.isNotEmpty()) {
            onBatchParsed(channelsBatch.toList(), programsBatch.toList())
            channelsBatch.clear()
            programsBatch.clear()
        }
    }

    private fun parseChannel(parser: XmlPullParser, packageId: String): ChannelEntity? {
        val id = parser.getAttributeValue(null, "id") ?: run {
            skipElement(parser)
            return null
        }
        var displayName = ""
        var iconUrl: String? = null
        var lcnNumber = 0

        while (parser.next() != XmlPullParser.END_TAG || parser.name != XML_TAG_CHANNEL) {
            if (parser.eventType != XmlPullParser.START_TAG) continue

            when (parser.name) {
                "display-name" -> displayName = readText(parser)
                "icon" -> {
                    val rawIcon = parser.getAttributeValue(null, "src")
                    if (!rawIcon.isNullOrBlank()) {
                        iconUrl = if (rawIcon.startsWith("//")) "https:$rawIcon" else rawIcon
                    }
                    skipElement(parser)
                }
                "lcn", "channel-num" -> {
                    lcnNumber = readText(parser).trim().toIntOrNull() ?: 0
                }
                else -> skipElement(parser)
            }
        }

        val resolvedNumber = if (lcnNumber > 0) lcnNumber else ChannelCatalog.getChannelNumber(id)
        val resolvedIcon = ChannelCatalog.getLogoUrl(id) ?: iconUrl

        return ChannelEntity(
            id = id,
            displayName = displayName.ifEmpty { id },
            iconUrl = resolvedIcon,
            packageId = packageId,
            channelNumber = resolvedNumber
        )
    }

    private fun parseProgram(parser: XmlPullParser): ProgramEntity? {
        val channelId = parser.getAttributeValue(null, "channel") ?: run {
            skipElement(parser)
            return null
        }
        val startString = parser.getAttributeValue(null, "start") ?: run {
            skipElement(parser)
            return null
        }
        val stopString = parser.getAttributeValue(null, "stop") ?: run {
            skipElement(parser)
            return null
        }

        val startTimestamp = parseDate(startString)
        val stopTimestamp = parseDate(stopString)
        if (startTimestamp == null || stopTimestamp == null) {
            skipElement(parser)
            return null
        }

        var title = ""
        var subTitle: String? = null
        var description = ""
        var secondaryDescription: String? = null
        val categories = mutableListOf<String>()
        var iconUrl: String? = null
        var seasonNumber: Int? = null
        var episodeNumber: Int? = null
        var episodeDisplay: String? = null
        val presenters = mutableListOf<String>()
        val actors = mutableListOf<String>()
        val guests = mutableListOf<String>()
        val directors = mutableListOf<String>()
        val producers = mutableListOf<String>()
        val writers = mutableListOf<String>()
        var contentRating: String? = null
        var starRating: Float? = null
        var isUnreleased = false
        var hasSubtitles = false
        var isHd = false
        var lengthMinutes = 0
        var productionYear: Int? = null
        var seriesYear: Int? = null
        var country: String? = null

        while (parser.next() != XmlPullParser.END_TAG || parser.name != XML_TAG_PROGRAM) {
            if (parser.eventType != XmlPullParser.START_TAG) continue

            when (parser.name) {
                "title" -> title = readText(parser)
                "sub-title" -> subTitle = readText(parser)
                "desc" -> {
                    val lang = parser.getAttributeValue(null, "lang")
                    val text = readText(parser)
                    if (lang == "fr-more" || lang == "more") {
                        secondaryDescription = text
                    } else if (description.isEmpty()) {
                        description = text
                    } else if (secondaryDescription.isNullOrEmpty() && text != description) {
                        secondaryDescription = text
                    }
                }
                "desc-more" -> secondaryDescription = readText(parser)
                "category" -> categories.add(readText(parser))
                "date" -> {
                    val text = readText(parser).trim()
                    productionYear = text.take(4).toIntOrNull()
                }
                "orig-date" -> {
                    val text = readText(parser).trim()
                    seriesYear = text.take(4).toIntOrNull()
                }
                "country" -> {
                    country = readText(parser).trim()
                }
                "length" -> {
                    val text = readText(parser)
                    lengthMinutes = text.toIntOrNull() ?: 0
                }
                "icon" -> {
                    val rawIcon = parser.getAttributeValue(null, "src")
                    if (!rawIcon.isNullOrBlank()) {
                        var resolved = if (rawIcon.startsWith("//")) "https:$rawIcon" else rawIcon
                        if (resolved.contains("/64x90/")) {
                            resolved = resolved.replace("/64x90/quality/80/", "/640x360/quality/90/")
                        }
                        iconUrl = resolved
                    }
                    skipElement(parser)
                }
                "episode-num" -> {
                    val system = parser.getAttributeValue(null, "system")
                    val text = readText(parser)
                    if (system == "xmltv_ns") {
                        val parts = text.split(".")
                        if (parts.isNotEmpty()) seasonNumber = parts[0].trim().toIntOrNull()?.plus(1)
                        if (parts.size > 1) episodeNumber = parts[1].trim().toIntOrNull()?.plus(1)
                    } else {
                        episodeDisplay = text
                    }
                }
                "credits" -> parseCredits(
                    parser, presenters, actors, guests, directors, producers, writers
                )
                "rating" -> contentRating = parseRating(parser)
                "star-rating" -> starRating = parseStarRating(parser)
                "premiere" -> {
                    if (!categories.any { it.equals("Rediffusion", ignoreCase = true) }) {
                        isUnreleased = true
                    }
                    skipElement(parser)
                }
                "previously-shown" -> {
                    categories.add("Rediffusion")
                    isUnreleased = false
                    skipElement(parser)
                }
                "subtitles" -> {
                    hasSubtitles = true
                    skipElement(parser)
                }
                "audio-description", "audiodescription" -> {
                    categories.add("Audiodescription")
                    skipElement(parser)
                }
                "video" -> {
                    isHd = parseVideoQuality(parser)
                }
                else -> skipElement(parser)
            }
        }

        val sRegex = Regex("""Saisons?\s*(\d+)""", RegexOption.IGNORE_CASE)
        val sFromSub = sRegex.find(subTitle ?: "")?.groupValues?.get(1)?.toIntOrNull()
            ?: sRegex.find(title)?.groupValues?.get(1)?.toIntOrNull()
        if (sFromSub != null) {
            seasonNumber = sFromSub
        } else if (seasonNumber == null) {
            seasonNumber = sRegex.find(description)?.groupValues?.get(1)?.toIntOrNull()
        }

        val eRegex = Regex("""(?:[ÉéE]pisode|[ÉéE]p\.?)\s*(\d+)""", RegexOption.IGNORE_CASE)
        val eFromSub = eRegex.find(subTitle ?: "")?.groupValues?.get(1)?.toIntOrNull()
            ?: eRegex.find(title)?.groupValues?.get(1)?.toIntOrNull()
        if (eFromSub != null) {
            episodeNumber = eFromSub
        } else if (episodeNumber == null) {
            episodeNumber = eRegex.find(description)?.groupValues?.get(1)?.toIntOrNull()
        }

        val isNewsOrWeather = categories.any { it.equals("Journal", ignoreCase = true) || it.equals("Météo", ignoreCase = true) } ||
            Regex("""\b(?:journal|m[ée]t[ée]o|l'info|le 1945|le 1245|jt)\b""", RegexOption.IGNORE_CASE).containsMatchIn(title)
        if (isNewsOrWeather) {
            seasonNumber = null
            episodeNumber = null
        }
        if (productionYear == null) {
            val yRegex = Regex("""(?:Sortie|Année)\s*:\s*(\d{4})""", RegexOption.IGNORE_CASE)
            val pRegex = Regex("""\((\d{4})\)""")
            productionYear = yRegex.find(description)?.groupValues?.get(1)?.toIntOrNull()
                ?: pRegex.find(title)?.groupValues?.get(1)?.toIntOrNull()
                ?: pRegex.find(subTitle ?: "")?.groupValues?.get(1)?.toIntOrNull()
        }
        if (seriesYear == null) {
            val syRegex = Regex("""(?:Série|Origine)\s*:\s*(\d{4})""", RegexOption.IGNORE_CASE)
            seriesYear = syRegex.find(description)?.groupValues?.get(1)?.toIntOrNull()
        }
        if (country == null) {
            val cRegex = Regex("""Pays\s*:\s*([^•\n\r|]{3,30})""", RegexOption.IGNORE_CASE)
            country = cRegex.find(description)?.groupValues?.get(1)?.trim()
        }
        if (productionYear == null && !secondaryDescription.isNullOrBlank()) {
            val yRegex = Regex("""(?:Sortie|Année)\s*:\s*(\d{4})""", RegexOption.IGNORE_CASE)
            productionYear = yRegex.find(secondaryDescription)?.groupValues?.get(1)?.toIntOrNull()
        }
        if (seriesYear == null && !secondaryDescription.isNullOrBlank()) {
            val syRegex = Regex("""(?:Série|Origine)\s*:\s*(\d{4})""", RegexOption.IGNORE_CASE)
            seriesYear = syRegex.find(secondaryDescription)?.groupValues?.get(1)?.toIntOrNull()
        }
        if (country == null && !secondaryDescription.isNullOrBlank()) {
            val cRegex = Regex("""Pays\s*:\s*([^•\n\r|]{3,30})""", RegexOption.IGNORE_CASE)
            country = cRegex.find(secondaryDescription)?.groupValues?.get(1)?.trim()
        }

        if (!hasSubtitles) {
            val scanText = "$title ${subTitle ?: ""} $description ${secondaryDescription ?: ""}"
            if (scanText.contains("sous-titr", ignoreCase = true) ||
                scanText.contains("malentendant", ignoreCase = true) ||
                scanText.contains("VOST", ignoreCase = true) ||
                scanText.contains("SME", ignoreCase = false) ||
                scanText.contains("[ST]", ignoreCase = true) ||
                scanText.contains("(ST)", ignoreCase = true)
            ) {
                hasSubtitles = true
            }
        }

        val cleanedPrimary = ProgramDescriptionUtils.clean(description) ?: ""
        val cleanedSecondary = ProgramDescriptionUtils.clean(secondaryDescription)
        val finalSecondary = if (ProgramDescriptionUtils.isRedundant(cleanedPrimary, cleanedSecondary)) {
            null
        } else {
            cleanedSecondary
        }

        val calculatedDuration = if (lengthMinutes > 0) {
            lengthMinutes
        } else {
            ((stopTimestamp - startTimestamp) / 60000L).toInt().coerceAtLeast(0)
        }

        return ProgramEntity(
            channelId = channelId,
            startTimestamp = startTimestamp,
            stopTimestamp = stopTimestamp,
            title = title,
            subTitle = subTitle,
            description = cleanedPrimary,
            secondaryDescription = finalSecondary,
            categories = categories,
            iconUrl = iconUrl,
            seasonNumber = seasonNumber,
            episodeNumber = episodeNumber,
            episodeDisplay = episodeDisplay,
            presenters = presenters,
            actors = actors,
            guests = guests,
            directors = directors,
            producers = producers,
            writers = writers,
            contentRating = contentRating,
            starRating = starRating,
            isUnreleased = isUnreleased,
            hasSubtitles = hasSubtitles,
            isHd = isHd,
            durationMinutes = calculatedDuration,
            productionYear = productionYear,
            seriesYear = seriesYear,
            country = country
        )
    }

    private fun parseCredits(
        parser: XmlPullParser,
        presenters: MutableList<String>,
        actors: MutableList<String>,
        guests: MutableList<String>,
        directors: MutableList<String>,
        producers: MutableList<String>,
        writers: MutableList<String>
    ) {
        while (parser.next() != XmlPullParser.END_TAG || parser.name != "credits") {
            if (parser.eventType != XmlPullParser.START_TAG) continue
            when (parser.name) {
                "presenter" -> presenters.add(readText(parser))
                "actor" -> actors.add(readText(parser))
                "guest" -> guests.add(readText(parser))
                "director" -> directors.add(readText(parser))
                "producer" -> producers.add(readText(parser))
                "writer" -> writers.add(readText(parser))
                else -> skipElement(parser)
            }
        }
    }

    private fun parseRating(parser: XmlPullParser): String? {
        var value: String? = null
        while (parser.next() != XmlPullParser.END_TAG || parser.name != "rating") {
            if (parser.eventType != XmlPullParser.START_TAG) continue
            if (parser.name == "value") {
                value = readText(parser)
            } else {
                skipElement(parser)
            }
        }
        return value
    }

    private fun parseStarRating(parser: XmlPullParser): Float? {
        var value: Float? = null
        while (parser.next() != XmlPullParser.END_TAG || parser.name != "star-rating") {
            if (parser.eventType != XmlPullParser.START_TAG) continue
            if (parser.name == "value") {
                val text = readText(parser)
                val split = text.split("/")
                val numeric = split.firstOrNull()?.trim()?.toFloatOrNull()
                if (numeric != null) {
                    val max = split.getOrNull(1)?.trim()?.toFloatOrNull() ?: 5f
                    value = (numeric / max) * 5f
                }
            } else {
                skipElement(parser)
            }
        }
        return value
    }

    private fun parseVideoQuality(parser: XmlPullParser): Boolean {
        var isHd = false
        while (parser.next() != XmlPullParser.END_TAG || parser.name != "video") {
            if (parser.eventType != XmlPullParser.START_TAG) continue
            if (parser.name == "quality") {
                val text = readText(parser)
                if (text.contains("HD", ignoreCase = true)) {
                    isHd = true
                }
            } else {
                skipElement(parser)
            }
        }
        return isHd
    }

    private fun parseDate(dateStr: String): Long? {
        val trimmed = dateStr.trim()
        val normalized = when {
            trimmed.length == 14 -> "$trimmed +0000"
            trimmed.length >= 19 && trimmed[14] != ' ' -> "${trimmed.substring(0, 14)} ${trimmed.substring(14)}"
            else -> trimmed
        }
        val cleanOffset = normalized.replace("([+-]\\d{2}):(\\d{2})".toRegex(), "$1$2")

        return try {
            val zonedDateTime = ZonedDateTime.parse(cleanOffset, dateFormatter)
            zonedDateTime.toInstant().toEpochMilli()
        } catch (_: DateTimeParseException) {
            null
        }
    }

    private fun readText(parser: XmlPullParser): String {
        var result = ""
        var eventType = parser.next()
        while (eventType != XmlPullParser.END_TAG) {
            if (eventType == XmlPullParser.TEXT) {
                result += parser.text.trim()
            }
            eventType = parser.next()
        }
        return result
    }

    private fun skipElement(parser: XmlPullParser) {
        if (parser.eventType != XmlPullParser.START_TAG) return
        var depth = 1
        while (depth != 0) {
            when (parser.next()) {
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.START_TAG -> depth++
            }
        }
    }
}
