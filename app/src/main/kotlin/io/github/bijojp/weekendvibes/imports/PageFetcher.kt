package io.github.bijojp.weekendvibes.imports

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.Charset

/** A fetched page: where it ended up after redirects, its HTTP status and its text. */
data class Fetched(val url: String, val status: Int, val html: String)

/** Plain HTTP GETs for Link import (ADR 0002). Only http(s), redirects followed by hand, sizes capped. */
internal object PageFetcher {
    private const val MAX_REDIRECTS = 6
    private const val MAX_PAGE_BYTES = 4 * 1024 * 1024
    private const val MAX_IMAGE_BYTES = 15 * 1024 * 1024
    private const val CONNECT_TIMEOUT = 10_000
    private const val READ_TIMEOUT = 15_000

    suspend fun page(url: String, userAgent: String): Fetched = withContext(Dispatchers.IO) {
        get(url, userAgent, accept = "text/html,application/xhtml+xml;q=0.9,*/*;q=0.8", limit = MAX_PAGE_BYTES) { final, status, bytes, type ->
            Fetched(final, status, String(bytes, charsetOf(type)))
        }
    }

    /** An event page's picture, or an [IOException] when it isn't one. */
    suspend fun image(url: String, userAgent: String): ByteArray = withContext(Dispatchers.IO) {
        get(url, userAgent, accept = "image/*", limit = MAX_IMAGE_BYTES) { _, status, bytes, _ ->
            if (status !in 200..299) throw IOException("Image request failed: $status")
            bytes
        }
    }

    private fun <T> get(
        start: String,
        userAgent: String,
        accept: String,
        limit: Int,
        result: (url: String, status: Int, body: ByteArray, contentType: String?) -> T,
    ): T {
        // Plain-text HTTP is blocked on Android, so http links are tried over https.
        var url = URL(start.trim().replaceFirst(Regex("^http://", RegexOption.IGNORE_CASE), "https://"))
        repeat(MAX_REDIRECTS) {
            if (url.protocol != "https") throw IOException("Only https links can be read")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = false
                connectTimeout = CONNECT_TIMEOUT
                readTimeout = READ_TIMEOUT
                setRequestProperty("User-Agent", userAgent)
                setRequestProperty("Accept", accept)
                setRequestProperty("Accept-Language", "en-IN,en;q=0.9")
            }
            try {
                val status = connection.responseCode
                if (status in 300..399) {
                    val location = connection.getHeaderField("Location") ?: throw IOException("Redirect without a location")
                    url = URL(url, location)
                    return@repeat
                }
                val stream = if (status >= 400) connection.errorStream else connection.inputStream
                val body = stream?.use { it.readCapped(limit) } ?: ByteArray(0)
                return result(url.toString(), status, body, connection.contentType)
            } finally {
                connection.disconnect()
            }
        }
        throw IOException("Too many redirects")
    }

    private fun InputStream.readCapped(limit: Int): ByteArray {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(16 * 1024)
        while (true) {
            val read = read(buffer)
            if (read < 0) break
            out.write(buffer, 0, read)
            if (out.size() > limit) throw IOException("Response too large")
        }
        return out.toByteArray()
    }

    private fun charsetOf(contentType: String?): Charset =
        contentType?.substringAfter("charset=", "")?.substringBefore(';')?.trim('"', ' ')?.takeIf { it.isNotEmpty() }
            ?.let { runCatching { Charset.forName(it) }.getOrNull() }
            ?: Charsets.UTF_8
}
