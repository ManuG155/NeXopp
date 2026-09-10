package com.nexopp.lan

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.net.URLDecoder
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

data class LanReceivedMessage(
    val type: String,
    val payload: String,
    val timestamp: Long = System.currentTimeMillis()
)

sealed class LanServerStatus {
    object Stopped : LanServerStatus()
    data class Listening(val port: Int, val localIp: String, val session: PairingSession) : LanServerStatus()
    data class Connected(val port: Int, val localIp: String, val clientAddress: String, val session: PairingSession) : LanServerStatus()
    data class Error(val message: String) : LanServerStatus()
}

class LanServer(
    val pairingTokenManager: PairingTokenManager = PairingTokenManager()
) {
    private var serverSocket: ServerSocket? = null
    private var scope: CoroutineScope? = null
    private var serverJob: Job? = null

    private val _status = MutableStateFlow<LanServerStatus>(LanServerStatus.Stopped)
    val status: StateFlow<LanServerStatus> = _status.asStateFlow()

    private val _incomingMessages = MutableSharedFlow<LanReceivedMessage>(extraBufferCapacity = 64)
    val incomingMessages: SharedFlow<LanReceivedMessage> = _incomingMessages.asSharedFlow()

    private val activeWsClients = ConcurrentHashMap.newKeySet<Socket>()
    private var currentSession: PairingSession? = null

    /**
     * Starts the local HTTP & WebSocket server.
     * Searches for an available port starting from [initialPort] (default 8080).
     */
    @Synchronized
    fun start(localIp: String, initialPort: Int = 8080): Boolean {
        stop()

        val job = SupervisorJob()
        val coroutineScope = CoroutineScope(Dispatchers.IO + job)
        scope = coroutineScope

        // Try ports starting from initialPort
        var boundSocket: ServerSocket? = null
        var port = initialPort
        while (port < initialPort + 20) {
            try {
                boundSocket = ServerSocket(port)
                break
            } catch (_: Exception) {
                port++
            }
        }

        if (boundSocket == null) {
            _status.value = LanServerStatus.Error("No se pudo enlazar ningún puerto local entre $initialPort y ${initialPort + 20}")
            return false
        }

        serverSocket = boundSocket
        val session = pairingTokenManager.createSession()
        currentSession = session
        _status.value = LanServerStatus.Listening(port, localIp, session)

        serverJob = coroutineScope.launch {
            try {
                while (isActive) {
                    val clientSocket = boundSocket.accept()
                    launch {
                        handleClient(clientSocket, port, localIp, session)
                    }
                }
            } catch (_: SocketException) {
                // Expected when ServerSocket is closed on stop()
            } catch (e: Exception) {
                if (isActive) {
                    _status.value = LanServerStatus.Error("Error en servidor local: ${e.localizedMessage}")
                }
            }
        }

        return true
    }

    /**
     * Stops the local server, disconnects all clients, cancels background coroutines, and revokes tokens.
     */
    @Synchronized
    fun stop() {
        try {
            for (client in activeWsClients) {
                try {
                    LanWebSocketFrame.writeServerCloseFrame(client.getOutputStream(), reason = "Server stopped")
                    client.close()
                } catch (_: Exception) {}
            }
            activeWsClients.clear()
        } catch (_: Exception) {}

        try {
            serverSocket?.close()
        } catch (_: Exception) {}
        serverSocket = null

        serverJob?.cancel()
        serverJob = null

        scope?.cancel()
        scope = null

        pairingTokenManager.revokeAll()
        currentSession = null
        _status.value = LanServerStatus.Stopped
    }

    /**
     * Broadcasts a message to all connected PC WebSocket clients.
     */
    fun sendMessage(type: String, payload: String) {
        val json = JSONObject().apply {
            put("type", type)
            put("payload", payload)
            put("timestamp", System.currentTimeMillis())
        }.toString()

        val deadClients = mutableListOf<Socket>()
        for (client in activeWsClients) {
            try {
                LanWebSocketFrame.writeServerTextFrame(client.getOutputStream(), json)
            } catch (e: Exception) {
                deadClients.add(client)
            }
        }

        if (deadClients.isNotEmpty()) {
            activeWsClients.removeAll(deadClients)
            updateConnectionStatusAfterClientLeave()
        }
    }

    private suspend fun handleClient(
        socket: Socket,
        port: Int,
        localIp: String,
        session: PairingSession
    ) {
        val inputStream = socket.getInputStream()
        val outputStream = socket.getOutputStream()
        val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))

        val requestLine = reader.readLine() ?: run {
            socket.close()
            return
        }

        val parts = requestLine.split(" ")
        if (parts.size < 2) {
            socket.close()
            return
        }

        val method = parts[0]
        val fullPath = parts[1]
        val path = fullPath.substringBefore("?")
        val query = fullPath.substringAfter("?", "")

        val headers = mutableMapOf<String, String>()
        var line: String? = reader.readLine()
        while (!line.isNullOrEmpty()) {
            val colon = line.indexOf(':')
            if (colon != -1) {
                val key = line.substring(0, colon).trim().lowercase()
                val value = line.substring(colon + 1).trim()
                headers[key] = value
            }
            line = reader.readLine()
        }

        val isWebSocketUpgrade = headers["upgrade"]?.equals("websocket", ignoreCase = true) == true

        if (isWebSocketUpgrade && path == "/ws") {
            handleWebSocketUpgrade(socket, headers, query, inputStream, outputStream, port, localIp, session)
        } else {
            handleHttpRoute(socket, method, path, query, outputStream, session)
            socket.close()
        }
    }

    private suspend fun handleWebSocketUpgrade(
        socket: Socket,
        headers: Map<String, String>,
        query: String,
        inputStream: java.io.InputStream,
        outputStream: OutputStream,
        port: Int,
        localIp: String,
        session: PairingSession
    ) {
        val queryParams = parseQueryParams(query)
        val token = queryParams["token"]

        // Validate token
        if (!pairingTokenManager.validateToken(token)) {
            val response = "HTTP/1.1 401 Unauthorized\r\nContent-Type: text/plain\r\nConnection: close\r\n\r\nToken de emparejamiento inválido o expirado"
            outputStream.write(response.toByteArray(Charsets.UTF_8))
            outputStream.flush()
            socket.close()
            return
        }

        val secKey = headers["sec-websocket-key"]
        if (secKey.isNullOrBlank()) {
            val response = "HTTP/1.1 400 Bad Request\r\nContent-Type: text/plain\r\nConnection: close\r\n\r\nFalta encabezado Sec-WebSocket-Key"
            outputStream.write(response.toByteArray(Charsets.UTF_8))
            outputStream.flush()
            socket.close()
            return
        }

        val acceptKey = LanWebSocketFrame.computeSecWebSocketAccept(secKey)
        val handshakeResponse = "HTTP/1.1 101 Switching Protocols\r\n" +
                "Upgrade: websocket\r\n" +
                "Connection: Upgrade\r\n" +
                "Sec-WebSocket-Accept: $acceptKey\r\n\r\n"

        outputStream.write(handshakeResponse.toByteArray(Charsets.UTF_8))
        outputStream.flush()

        activeWsClients.add(socket)
        val clientAddress = socket.inetAddress?.hostAddress ?: "PC"
        _status.value = LanServerStatus.Connected(port, localIp, clientAddress, session)

        // Send initial connected confirmation frame to PC
        LanWebSocketFrame.writeServerTextFrame(
            outputStream,
            JSONObject().apply {
                put("type", "STATUS")
                put("payload", "CONNECTED")
                put("timestamp", System.currentTimeMillis())
            }.toString()
        )

        // Listen for WebSocket frames
        try {
            while (true) {
                val frame = LanWebSocketFrame.readClientFrame(inputStream) ?: break
                when (frame.opcode) {
                    LanWebSocketFrame.OPCODE_TEXT -> {
                        val text = frame.text
                        val msg = parseIncomingJson(text)
                        _incomingMessages.emit(msg)
                    }
                    LanWebSocketFrame.OPCODE_PING -> {
                        LanWebSocketFrame.writeServerPongFrame(outputStream, frame.payload)
                    }
                    LanWebSocketFrame.OPCODE_CLOSE -> {
                        LanWebSocketFrame.writeServerCloseFrame(outputStream)
                        break
                    }
                }
            }
        } catch (_: Exception) {
        } finally {
            activeWsClients.remove(socket)
            try { socket.close() } catch (_: Exception) {}
            updateConnectionStatusAfterClientLeave()
        }
    }

    private fun handleHttpRoute(
        socket: Socket,
        method: String,
        path: String,
        query: String,
        outputStream: OutputStream,
        session: PairingSession
    ) {
        val queryParams = parseQueryParams(query)

        when {
            // Main SPA landing page
            path == "/" -> {
                val html = LanWebClientHtml.getHtml(initialToken = null)
                sendHttpResponse(outputStream, 200, "OK", "text/html; charset=utf-8", html)
            }

            // QR Direct Connect URL with token embedded: /connect/{token}
            path.startsWith("/connect/") -> {
                val tokenFromUrl = path.removePrefix("/connect/").trim()
                val html = LanWebClientHtml.getHtml(initialToken = tokenFromUrl)
                sendHttpResponse(outputStream, 200, "OK", "text/html; charset=utf-8", html)
            }

            // API: Pairing code check
            path == "/api/pair" -> {
                val code = queryParams["code"]
                val validToken = pairingTokenManager.validatePairingCode(code)
                val json = JSONObject().apply {
                    if (validToken != null) {
                        put("success", true)
                        put("sessionToken", validToken)
                    } else {
                        put("success", false)
                        put("error", "Código incorrecto o expirado")
                    }
                }.toString()
                sendHttpResponse(outputStream, 200, "OK", "application/json; charset=utf-8", json)
            }

            // API: Status check
            path == "/api/status" -> {
                val json = JSONObject().apply {
                    put("status", "ok")
                    put("service", "NeXopp-Tablet")
                    put("version", "1.0")
                }.toString()
                sendHttpResponse(outputStream, 200, "OK", "application/json; charset=utf-8", json)
            }

            else -> {
                sendHttpResponse(outputStream, 404, "Not Found", "text/plain; charset=utf-8", "404 Not Found")
            }
        }
    }

    private fun sendHttpResponse(
        outputStream: OutputStream,
        code: Int,
        statusText: String,
        contentType: String,
        body: String
    ) {
        val bodyBytes = body.toByteArray(Charsets.UTF_8)
        val response = "HTTP/1.1 $code $statusText\r\n" +
                "Content-Type: $contentType\r\n" +
                "Content-Length: ${bodyBytes.size}\r\n" +
                "Connection: close\r\n\r\n"
        outputStream.write(response.toByteArray(Charsets.UTF_8))
        outputStream.write(bodyBytes)
        outputStream.flush()
    }

    private fun parseQueryParams(query: String): Map<String, String> {
        if (query.isBlank()) return emptyMap()
        val result = mutableMapOf<String, String>()
        val pairs = query.split("&")
        for (pair in pairs) {
            val idx = pair.indexOf("=")
            if (idx > 0) {
                val key = URLDecoder.decode(pair.substring(0, idx), "UTF-8")
                val value = URLDecoder.decode(pair.substring(idx + 1), "UTF-8")
                result[key] = value
            }
        }
        return result
    }

    private fun parseIncomingJson(text: String): LanReceivedMessage {
        return try {
            val json = JSONObject(text)
            val type = json.optString("type", "MESSAGE")
            val payload = json.optString("payload", text)
            val ts = json.optLong("timestamp", System.currentTimeMillis())
            LanReceivedMessage(type = type, payload = payload, timestamp = ts)
        } catch (_: Exception) {
            LanReceivedMessage(type = "MESSAGE", payload = text)
        }
    }

    private fun updateConnectionStatusAfterClientLeave() {
        val session = currentSession
        val s = serverSocket
        if (session != null && s != null && !s.isClosed) {
            if (activeWsClients.isEmpty()) {
                val cur = _status.value
                val port = s.localPort
                val ip = when (cur) {
                    is LanServerStatus.Listening -> cur.localIp
                    is LanServerStatus.Connected -> cur.localIp
                    else -> "0.0.0.0"
                }
                _status.value = LanServerStatus.Listening(port, ip, session)
            }
        }
    }
}
