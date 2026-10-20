package com.music4music.tvguide.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.URLEncoder
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

    suspend fun searchTrailers(title: String, year: Int? = null): List<TrailerVideo> = withContext(Dispatchers.IO) {
        val cleanTitle = cleanSearchTitle(title)
        if (cleanTitle.isBlank()) return@withContext emptyList()

        val results = queryYouTube(cleanTitle, year, "bande annonce")
        if (results.isNotEmpty()) {
            return@withContext results
        }
        return@withContext queryYouTube(cleanTitle, year, "trailer")
    }

    private fun queryYouTube(cleanTitle: String, year: Int?, suffix: String): List<TrailerVideo> {
        val queryBuilder = StringBuilder(cleanTitle)
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
            parseVideoResults(body)
        } catch (e: IOException) {
            emptyList()
        }
    }

    private fun parseVideoResults(html: String): List<TrailerVideo> {
        val results = mutableListOf<TrailerVideo>()
        val seenIds = mutableSetOf<String>()

        val pattern = Pattern.compile(
            "\"videoId\"\\s*:\\s*\"([a-zA-Z0-9_-]{11})\"[\\s\\S]*?\"title\"\\s*:\\s*\\{[\\s\\S]*?\"text\"\\s*:\\s*\"([^\"]+)\"",
            Pattern.MULTILINE
        )
        val matcher = pattern.matcher(html)

        while (matcher.find() && results.size < 5) {
            val videoId = matcher.group(1) ?: continue
            val title = matcher.group(2) ?: ""
            if (seenIds.add(videoId)) {
                results.add(TrailerVideo(videoId = videoId, title = unescapeJson(title)))
            }
        }

        if (results.isEmpty()) {
            val fallbackPattern = Pattern.compile("\"videoId\"\\s*:\\s*\"([a-zA-Z0-9_-]{11})\"")
            val fallbackMatcher = fallbackPattern.matcher(html)
            while (fallbackMatcher.find() && results.size < 3) {
                val videoId = fallbackMatcher.group(1) ?: continue
                if (seenIds.add(videoId)) {
                    results.add(TrailerVideo(videoId = videoId, title = ""))
                }
            }
        }

        return results
    }

    private fun cleanSearchTitle(title: String): String {
        return title
            .replace(Regex("\\[-?\\d+\\]"), "")
            .replace(Regex("(?i)\\b(saison|episode|épisode)\\s*\\d+.*"), "")
            .replace(Regex("(?i)\\b(rediffusion|inédit|direct)\\b"), "")
            .trim()
    }

    private fun unescapeJson(text: String): String {
        return text
            .replace("\\u0026", "&")
            .replace("\\\"", "\"")
            .replace("\\'", "'")
            .replace("\\\\", "\\")
    }
}
