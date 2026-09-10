package com.nexopp.lan

import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.util.Base64

/**
 * Implements RFC 6455 WebSocket frame reading, writing, and handshake negotiation.
 * Compatible with Java 17 / Android API 26+ without external dependencies.
 */
object LanWebSocketFrame {

    private const val WEBSOCKET_GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11"

    const val OPCODE_CONTINUATION = 0x0
    const val OPCODE_TEXT = 0x1
    const val OPCODE_BINARY = 0x2
    const val OPCODE_CLOSE = 0x8
    const val OPCODE_PING = 0x9
    const val OPCODE_PONG = 0xA

    data class Frame(
        val fin: Boolean,
        val opcode: Int,
        val payload: ByteArray
    ) {
        val text: String
            get() = String(payload, Charsets.UTF_8)

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as Frame
            return fin == other.fin && opcode == other.opcode && payload.contentEquals(other.payload)
        }

        override fun hashCode(): Int {
            var result = fin.hashCode()
            result = 31 * result + opcode
            result = 31 * result + payload.contentHashCode()
            return result
        }
    }

    /**
     * Computes the Sec-WebSocket-Accept response header value according to RFC 6455.
     * Value = Base64(SHA-1(Sec-WebSocket-Key + WEBSOCKET_GUID))
     */
    fun computeSecWebSocketAccept(clientKey: String): String {
        val sha1 = MessageDigest.getInstance("SHA-1")
        val input = clientKey.trim() + WEBSOCKET_GUID
        val hash = sha1.digest(input.toByteArray(Charsets.ISO_8859_1))
        return Base64.getEncoder().encodeToString(hash)
    }

    /**
     * Reads a single RFC 6455 frame from the client input stream.
     * Client frames MUST be masked according to RFC 6455 section 5.1.
     * Returns null if the stream is closed or EOF is reached.
     */
    fun readClientFrame(inputStream: InputStream): Frame? {
        val b0 = inputStream.read()
        if (b0 == -1) return null
        val fin = (b0 and 0x80) != 0
        val opcode = b0 and 0x0F

        val b1 = inputStream.read()
        if (b1 == -1) return null
        val masked = (b1 and 0x80) != 0
        var payloadLen = (b1 and 0x7F).toLong()

        if (payloadLen == 126L) {
            val lenBytes = readExactBytes(inputStream, 2) ?: return null
            payloadLen = ((lenBytes[0].toInt() and 0xFF) shl 8 or (lenBytes[1].toInt() and 0xFF)).toLong()
        } else if (payloadLen == 127L) {
            val lenBytes = readExactBytes(inputStream, 8) ?: return null
            val buffer = ByteBuffer.wrap(lenBytes)
            payloadLen = buffer.long
        }

        if (payloadLen < 0 || payloadLen > 16 * 1024 * 1024) {
            // Guard against abnormally huge payloads (limit to 16MB)
            throw IllegalArgumentException("Payload length $payloadLen exceeds 16MB limit")
        }

        val maskKey = if (masked) {
            readExactBytes(inputStream, 4) ?: return null
        } else {
            null
        }

        val payload = readExactBytes(inputStream, payloadLen.toInt()) ?: return null

        // Unmask client payload
        if (maskKey != null) {
            for (i in payload.indices) {
                payload[i] = (payload[i].toInt() xor (maskKey[i % 4].toInt() and 0xFF)).toByte()
            }
        }

        return Frame(fin, opcode, payload)
    }

    /**
     * Writes an unmasked text frame from server to client per RFC 6455.
     */
    @Synchronized
    fun writeServerTextFrame(outputStream: OutputStream, text: String) {
        val payload = text.toByteArray(Charsets.UTF_8)
        writeServerFrame(outputStream, OPCODE_TEXT, payload)
    }

    /**
     * Writes an unmasked close frame from server to client.
     */
    @Synchronized
    fun writeServerCloseFrame(outputStream: OutputStream, code: Short = 1000, reason: String = "") {
        val reasonBytes = reason.toByteArray(Charsets.UTF_8)
        val payload = ByteArray(2 + reasonBytes.size)
        payload[0] = ((code.toInt() ushr 8) and 0xFF).toByte()
        payload[1] = (code.toInt() and 0xFF).toByte()
        System.arraycopy(reasonBytes, 0, payload, 2, reasonBytes.size)
        writeServerFrame(outputStream, OPCODE_CLOSE, payload)
    }

    /**
     * Writes an unmasked pong frame from server to client.
     */
    @Synchronized
    fun writeServerPongFrame(outputStream: OutputStream, pingPayload: ByteArray) {
        writeServerFrame(outputStream, OPCODE_PONG, pingPayload)
    }

    private fun writeServerFrame(outputStream: OutputStream, opcode: Int, payload: ByteArray) {
        // FIN = 1, Opcode
        val b0 = (0x80 or (opcode and 0x0F)).toByte()
        outputStream.write(b0.toInt())

        // Mask bit is 0 for server-to-client frames
        val len = payload.size
        when {
            len <= 125 -> {
                outputStream.write(len)
            }
            len <= 65535 -> {
                outputStream.write(126)
                outputStream.write((len ushr 8) and 0xFF)
                outputStream.write(len and 0xFF)
            }
            else -> {
                outputStream.write(127)
                val buffer = ByteBuffer.allocate(8).putLong(len.toLong())
                outputStream.write(buffer.array())
            }
        }

        if (payload.isNotEmpty()) {
            outputStream.write(payload)
        }
        outputStream.flush()
    }

    private fun readExactBytes(inputStream: InputStream, count: Int): ByteArray? {
        val bytes = ByteArray(count)
        var offset = 0
        while (offset < count) {
            val read = inputStream.read(bytes, offset, count - offset)
            if (read == -1) return null
            offset += read
        }
        return bytes
    }
}
