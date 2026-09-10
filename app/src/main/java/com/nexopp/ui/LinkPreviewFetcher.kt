package com.nexopp.ui

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern

enum class LinkMediaType {
    VIDEO,
    ARTICLE,
    IMAGE,
    GENERIC
}

data class LinkMetadata(
    val url: String,
    val title: String?,
    val description: String?,
    val siteName: String?,
    val imageUrl: String?,
    val mediaType: LinkMediaType = LinkMediaType.GENERIC
)

object LinkPreviewFetcher {
    private val cache = ConcurrentHashMap<String, LinkMetadata>()

    private val OG_TITLE_PATTERN = Pattern.compile("<meta\\s+[^>]*property=[\"']og:title[\"'][^>]*content=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE)
    private val OG_TITLE_REVERSE = Pattern.compile("<meta\\s+[^>]*content=[\"']([^\"']+)[\"'][^>]*property=[\"']og:title[\"']", Pattern.CASE_INSENSITIVE)
    private val HTML_TITLE_PATTERN = Pattern.compile("<title>([^<]+)</title>", Pattern.CASE_INSENSITIVE)

    private val OG_DESC_PATTERN = Pattern.compile("<meta\\s+[^>]*property=[\"']og:description[\"'][^>]*content=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE)
    private val META_DESC_PATTERN = Pattern.compile("<meta\\s+[^>]*name=[\"']description[\"'][^>]*content=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE)

    private val OG_IMAGE_PATTERN = Pattern.compile("<meta\\s+[^>]*property=[\"']og:image[\"'][^>]*content=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE)
    private val OG_SITE_PATTERN = Pattern.compile("<meta\\s+[^>]*property=[\"']og:site_name[\"'][^>]*content=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE)

    private val YOUTUBE_PATTERN = Pattern.compile("(?:youtu\\.be/|youtube\\.com/(?:watch\\?v=|embed/|v/))([A-Za-z0-9_-]{11})")

    suspend fun fetchPreview(rawUrl: String): LinkMetadata? = withContext(Dispatchers.IO) {
        val cleanUrl = rawUrl.trim()
        if (cleanUrl.isBlank()) return@withContext null

        val targetUrl = if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
            "https://$cleanUrl"
        } else cleanUrl

        cache[targetUrl]?.let { return@withContext it }

        // Fast-path for YouTube
        val ytMatcher = YOUTUBE_PATTERN.matcher(targetUrl)
        if (ytMatcher.find()) {
            val videoId = ytMatcher.group(1)
            val thumbUrl = "https://img.youtube.com/vi/$videoId/hqdefault.jpg"
            val meta = LinkMetadata(
                url = targetUrl,
                title = "Video de YouTube",
                description = "Reproducción multimedia en NeXopp",
                siteName = "YouTube",
                imageUrl = thumbUrl,
                mediaType = LinkMediaType.VIDEO
            )
            cache[targetUrl] = meta
            return@withContext meta
        }

        runCatching {
            val urlObj = URL(targetUrl)
            val connection = (urlObj.openConnection() as HttpURLConnection).apply {
                connectTimeout = 3500
                readTimeout = 3500
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "Mozilla/5.0 (Android; Tablet) NeXopp/1.0")
                setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            }

            val contentType = connection.contentType?.lowercase() ?: ""
            if (!contentType.contains("text/html") && !contentType.contains("application/xhtml")) {
                val host = urlObj.host ?: "Enlace"
                val meta = LinkMetadata(
                    url = targetUrl,
                    title = host,
                    description = targetUrl,
                    siteName = host,
                    imageUrl = null,
                    mediaType = if (contentType.startsWith("image/")) LinkMediaType.IMAGE else LinkMediaType.GENERIC
                )
                cache[targetUrl] = meta
                return@withContext meta
            }

            val reader = BufferedReader(InputStreamReader(connection.inputStream))
            val sb = StringBuilder()
            var line: String?
            var bytesRead = 0
            // Read only first ~64KB of HTML to extract metadata efficiently
            while (reader.readLine().also { line = it } != null && bytesRead < 65536) {
                sb.append(line).append("\n")
                bytesRead += line!!.length
                if (sb.contains("</head>", ignoreCase = true)) break
            }
            reader.close()
            connection.disconnect()

            val html = sb.toString()

            val title = extractMatch(OG_TITLE_PATTERN, html)
                ?: extractMatch(OG_TITLE_REVERSE, html)
                ?: extractMatch(HTML_TITLE_PATTERN, html)?.trim()
                ?: urlObj.host

            val desc = extractMatch(OG_DESC_PATTERN, html)
                ?: extractMatch(META_DESC_PATTERN, html)

            val img = extractMatch(OG_IMAGE_PATTERN, html)
            val site = extractMatch(OG_SITE_PATTERN, html) ?: urlObj.host

            val mediaType = when {
                targetUrl.contains("vimeo.com") || targetUrl.contains("youtube") -> LinkMediaType.VIDEO
                else -> LinkMediaType.ARTICLE
            }

            val meta = LinkMetadata(
                url = targetUrl,
                title = decodeHtmlEntities(title),
                description = desc?.let { decodeHtmlEntities(it).take(180) },
                siteName = site,
                imageUrl = img,
                mediaType = mediaType
            )
            cache[targetUrl] = meta
            meta
        }.getOrNull()
    }

    private fun extractMatch(pattern: Pattern, input: String): String? {
        val matcher = pattern.matcher(input)
        return if (matcher.find()) matcher.group(1)?.trim() else null
    }

    private fun decodeHtmlEntities(text: String): String {
        return text
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&nbsp;", " ")
    }
}
