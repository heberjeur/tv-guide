package com.music4music.tvguide.data.remote

import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.io.InputStream
import java.util.zip.GZIPInputStream
import javax.inject.Inject
import javax.inject.Singleton

data class DownloadResult(
    val inputStream: InputStream?,
    val etag: String?,
    val isNotModified: Boolean
)

@Singleton
class XmltvDownloader @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    fun downloadFeed(url: String, currentEtag: String?): DownloadResult {
        val requestBuilder = Request.Builder().url(url)
        if (!currentEtag.isNullOrEmpty()) {
            requestBuilder.header("If-None-Match", currentEtag)
        }

        val response = okHttpClient.newCall(requestBuilder.build()).execute()
        if (response.code == 304) {
            response.close()
            return DownloadResult(inputStream = null, etag = currentEtag, isNotModified = true)
        }

        if (!response.isSuccessful) {
            val code = response.code
            response.close()
            throw IOException("HTTP download failed with code $code")
        }

        val body = response.body ?: throw IOException("HTTP response body was null")
        val newEtag = response.header("ETag")

        val rawStream = body.byteStream()
        val decompressedStream = if (url.endsWith(".gz", ignoreCase = true)) {
            GZIPInputStream(rawStream)
        } else {
            rawStream
        }

        return DownloadResult(
            inputStream = decompressedStream,
            etag = newEtag,
            isNotModified = false
        )
    }
}
