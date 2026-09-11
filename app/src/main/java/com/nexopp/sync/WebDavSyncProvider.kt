package com.nexopp.sync

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

/**
 * Standard WebDAV / Nextcloud / ownCloud synchronization provider.
 * Uses HTTP Basic Authentication and standard WebDAV PROPFIND / PUT / GET methods.
 */
class WebDavSyncProvider(
    private val serverUrl: String,
    private val username: String,
    private val passwordOrToken: String,
    private val remoteFolder: String = "FiXmyNotes"
) : SyncProvider {

    override val id: String = "webdav_nextcloud"
    override val displayName: String = "Nextcloud / WebDAV"

    private val baseEndpoint: String
        get() {
            var url = serverUrl.trim()
            if (!url.endsWith("/")) url += "/"
            val folder = remoteFolder.trim().trim('/')
            return if (folder.isNotEmpty()) "$url$folder/" else url
        }

    private val authHeader: String
        get() {
            val userPass = "$username:$passwordOrToken"
            return "Basic " + Base64.encodeToString(userPass.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        }

    override suspend fun testConnection(): SyncConnectionResult = withContext(Dispatchers.IO) {
        if (serverUrl.isBlank()) {
            return@withContext SyncConnectionResult(false, "La URL del servidor no puede estar vacía.")
        }
        try {
            ensureRemoteFolderExists()
            val url = URL(baseEndpoint)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "PROPFIND"
                setRequestProperty("Authorization", authHeader)
                setRequestProperty("Depth", "0")
                connectTimeout = 10000
                readTimeout = 10000
            }

            val code = conn.responseCode
            conn.disconnect()

            if (code in 200..299 || code == 207) { // 207 Multi-Status
                SyncConnectionResult(true, "Conexión a WebDAV/Nextcloud establecida con éxito.")
            } else if (code == 401 || code == 403) {
                SyncConnectionResult(false, "Error de autenticación ($code). Verifica usuario y contraseña/token de aplicación.")
            } else {
                SyncConnectionResult(false, "El servidor respondió con código HTTP $code.")
            }
        } catch (e: Exception) {
            SyncConnectionResult(false, "Error de conexión: ${e.localizedMessage}")
        }
    }

    override suspend fun listRemoteFiles(): List<RemoteFileInfo> = withContext(Dispatchers.IO) {
        val result = mutableListOf<RemoteFileInfo>()
        try {
            val url = URL(baseEndpoint)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "PROPFIND"
                setRequestProperty("Authorization", authHeader)
                setRequestProperty("Depth", "1")
                setRequestProperty("Content-Type", "application/xml; charset=utf-8")
                connectTimeout = 12000
                readTimeout = 12000
            }

            val code = conn.responseCode
            if (code in 200..299 || code == 207) {
                val xml = conn.inputStream.bufferedReader().readText()
                result.addAll(parsePropfindXml(xml))
            }
            conn.disconnect()
        } catch (_: Exception) {}
        result
    }

    override suspend fun uploadFile(localFile: File, remoteFileName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            ensureRemoteFolderExists()
            val url = URL("$baseEndpoint$remoteFileName")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "PUT"
                doOutput = true
                setRequestProperty("Authorization", authHeader)
                setRequestProperty("Content-Type", "application/octet-stream")
                connectTimeout = 15000
                readTimeout = 30000
                setFixedLengthStreamingMode(localFile.length())
            }

            localFile.inputStream().use { input ->
                conn.outputStream.use { output ->
                    input.copyTo(output)
                }
            }

            val code = conn.responseCode
            conn.disconnect()
            code in 200..299
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun downloadFile(remoteFileName: String, targetLocalFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("$baseEndpoint$remoteFileName")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Authorization", authHeader)
                connectTimeout = 15000
                readTimeout = 30000
            }

            val code = conn.responseCode
            if (code in 200..299) {
                val tempFile = File(targetLocalFile.parentFile, "${targetLocalFile.name}.downloading")
                conn.inputStream.use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                tempFile.renameTo(targetLocalFile)
                conn.disconnect()
                true
            } else {
                conn.disconnect()
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun deleteRemoteFile(remoteFileName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("$baseEndpoint$remoteFileName")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "DELETE"
                setRequestProperty("Authorization", authHeader)
                connectTimeout = 10000
                readTimeout = 10000
            }
            val code = conn.responseCode
            conn.disconnect()
            code in 200..299 || code == 404
        } catch (_: Exception) {
            false
        }
    }

    private fun ensureRemoteFolderExists() {
        try {
            val url = URL(baseEndpoint)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "MKCOL"
                setRequestProperty("Authorization", authHeader)
                connectTimeout = 8000
                readTimeout = 8000
            }
            conn.responseCode
            conn.disconnect()
        } catch (_: Exception) {}
    }

    /**
     * Parses standard WebDAV 207 Multi-Status XML response.
     */
    private fun parsePropfindXml(xml: String): List<RemoteFileInfo> {
        val list = mutableListOf<RemoteFileInfo>()
        // Simple regex/tag based parser to extract href, getcontentlength, getlastmodified, and getetag
        val responseBlocks = xml.split("</d:response>", "</D:response>", "</response>")

        for (block in responseBlocks) {
            val href = extractTag(block, "href") ?: continue
            val filename = href.trimEnd('/').substringAfterLast('/')
            if (filename.isEmpty() || !filename.endsWith(".xopp")) continue

            val contentLengthStr = extractTag(block, "getcontentlength") ?: "0"
            val sizeBytes = contentLengthStr.toLongOrNull() ?: 0L
            val lastModifiedStr = extractTag(block, "getlastmodified") ?: ""
            val etag = extractTag(block, "getetag")

            var lastModified = System.currentTimeMillis()
            if (lastModifiedStr.isNotEmpty()) {
                try {
                    // HTTP-date format: Sun, 06 Nov 1994 08:49:37 GMT
                    val sdf = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US)
                    val d = sdf.parse(lastModifiedStr)
                    if (d != null) lastModified = d.time
                } catch (_: Exception) {}
            }

            list.add(
                RemoteFileInfo(
                    filename = filename,
                    remotePath = href,
                    sizeBytes = sizeBytes,
                    lastModified = lastModified,
                    etag = etag
                )
            )
        }
        return list
    }

    private fun extractTag(text: String, tagName: String): String? {
        val patterns = listOf(
            "<$tagName>(.*?)</$tagName>",
            "<d:$tagName>(.*?)</d:$tagName>",
            "<D:$tagName>(.*?)</D:$tagName>"
        )
        for (p in patterns) {
            val regex = Regex(p, RegexOption.DOT_MATCHES_ALL)
            val match = regex.find(text)
            if (match != null) {
                return match.groupValues[1].trim()
            }
        }
        return null
    }
}
