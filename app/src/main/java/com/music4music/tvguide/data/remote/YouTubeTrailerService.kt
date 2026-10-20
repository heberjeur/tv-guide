package com.music4music.tvguide.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.URLEncoder
import java.util.Locale
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

data class TrailerVideo(
    val videoId: String,
    val title: String,
    val thumbnailUrl: String = "https://img.youtube.com/vi/$videoId/hqdefault.jpg"
)

@Singleton
class YouTubeTrailerService @Inject constructor(
    private val okHttpClient: OkHttpClient
) {

    suspend fun searchTrailers(title: String, subTitle: String? = null, year: Int? = null): List<TrailerVideo> = withContext(Dispatchers.IO) {
        val cleanTitle = cleanSearchTitle(title)
        if (cleanTitle.isBlank()) return@withContext emptyList()

        val cleanSub = subTitle?.let { cleanSearchTitle(it) }?.takeIf { it.isNotBlank() }
        val baseQuery = if (cleanSub != null && !cleanTitle.contains(cleanSub, ignoreCase = true)) {
            "$cleanTitle $cleanSub"
        } else {
            cleanTitle
        }

        val results = queryYouTube(baseQuery, cleanTitle, year, "bande annonce")
        if (results.isNotEmpty()) {
            return@withContext results
        }
        val fallbackResults = queryYouTube(baseQuery, cleanTitle, year, "trailer")
        if (fallbackResults.isNotEmpty()) {
            return@withContext fallbackResults
        }
        return@withContext queryYouTube(cleanTitle, cleanTitle, null, "extrait")
    }

    private fun queryYouTube(query: String, matchTitle: String, year: Int?, suffix: String): List<TrailerVideo> {
        val queryBuilder = StringBuilder(query)
        if (year != null && year > 1900) {
            queryBuilder.append(" ").append(year)
        }
        queryBuilder.append(" ").append(suffix)

        val encoded = URLEncoder.encode(queryBuilder.toString(), "UTF-8")
        val url = "https://www.youtube.com/results?search_query=$encoded"

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .header("Accept-Language", "fr-FR,fr;q=0.9,en;q=0.8")
            .build()

        return try {
            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) return emptyList()
            val body = response.body?.string() ?: return emptyList()
            parseVideoResults(body, matchTitle)
        } catch (e: IOException) {
            emptyList()
        }
    }

    private fun parseVideoResults(html: String, matchTitle: String): List<TrailerVideo> {
        val results = mutableListOf<TrailerVideo>()
        val seenIds = mutableSetOf<String>()

        val pattern = Pattern.compile(
            "\"videoRenderer\":\\{\"videoId\":\"([a-zA-Z0-9_-]{11})\".*?\"title\":\\{\"runs\":\\[\\{\"text\":\"([^\"]+)\"\\}",
            Pattern.DOTALL
        )
        val matcher = pattern.matcher(html)

        val keywords = matchTitle
            .lowercase(Locale.ROOT)
            .split(Regex("[^\\p{L}\\p{Nd}]+"))
            .filter { it.length > 2 && it !in commonWords }

        while (matcher.find() && results.size < 5) {
            val videoId = matcher.group(1) ?: continue
            val rawTitle = matcher.group(2) ?: ""
            val title = unescapeJson(rawTitle)
            val lowerTitle = title.lowercase(Locale.ROOT)

            val isRelevant = if (keywords.isEmpty()) {
                true
            } else {
                keywords.any { keyword -> lowerTitle.contains(keyword) }
            }

            if (isRelevant && seenIds.add(videoId)) {
                results.add(TrailerVideo(videoId = videoId, title = title))
            }
        }

        return results
    }

    private fun cleanSearchTitle(title: String): String {
        return title
            .replace(Regex("\\[-?\\d+\\]"), "")
            .replace(Regex("(?i)\\b(saison|episode|épisode)\\s*\\d+.*"), "")
            .replace(Regex("(?i)\\b(rediffusion|inédit|direct|inédite)\\b"), "")
            .replace(Regex("[:\\-–—]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun unescapeJson(text: String): String {
        return text
            .replace("\\u0026", "&")
            .replace("\\\"", "\"")
            .replace("\\'", "'")
            .replace("\\\\", "\\")
    }

    companion object {
        private val commonWords = setOf("les", "des", "une", "pour", "dans", "avec", "sur", "par", "the", "and")
    }
}
