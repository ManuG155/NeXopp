package com.nexopp.lan

import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class LanWebSocketFrameTest {

    @Test
    fun computeSecWebSocketAccept_matchesRfc6455SpecExample() {
        // RFC 6455 Section 1.3 official test vector
        val clientKey = "dGhlIHNhbXBsZSBub25jZQ=="
        val expectedAccept = "s3pPLMBiTxaQ9kYGzzhZRbK+xOo="

        val accept = LanWebSocketFrame.computeSecWebSocketAccept(clientKey)
        assertEquals(expectedAccept, accept)
    }

    @Test
    fun writeServerTextFrame_createsValidUnmaskedFrame() {
        val out = ByteArrayOutputStream()
        val message = "HELLO_FROM_TABLET"
        LanWebSocketFrame.writeServerTextFrame(out, message)

        val bytes = out.toByteArray()
        assertTrue(bytes.isNotEmpty())

        // Byte 0: FIN = 1, opcode = 1 (TEXT) -> 0x81
        assertEquals(0x81.toByte(), bytes[0])

        // Byte 1: Mask = 0, length = 17
        assertEquals(message.length.toByte(), bytes[1])

        // Remaining bytes: message ASCII/UTF-8
        val payloadStr = String(bytes, 2, message.length, Charsets.UTF_8)
        assertEquals(message, payloadStr)
    }

    @Test
    fun readClientFrame_unmasksPayloadCorrectly() {
        val text = "HELLO_FROM_PC"
        val payload = text.toByteArray(Charsets.UTF_8)
        val maskKey = byteArrayOf(0x12, 0x34, 0x56, 0x78)

        // Construct masked client frame per RFC 6455
        val frameBytes = ByteArrayOutputStream().apply {
            // Byte 0: FIN = 1, Opcode = 1
            write(0x81)
            // Byte 1: Mask bit = 1, len = payload.size
            write(0x80 or payload.size)
            // Mask key (4 bytes)
            write(maskKey)
            // Masked payload
            for (i in payload.indices) {
                write(payload[i].toInt() xor (maskKey[i % 4].toInt() and 0xFF))
            }
        }.toByteArray()

        val inputStream = ByteArrayInputStream(frameBytes)
        val frame = LanWebSocketFrame.readClientFrame(inputStream)

        assertNotNull(frame)
        assertTrue(frame!!.fin)
        assertEquals(LanWebSocketFrame.OPCODE_TEXT, frame.opcode)
        assertEquals(text, frame.text)
    }

    @Test
    fun writeServerCloseFrame_createsValidCloseOpcode() {
        val out = ByteArrayOutputStream()
        LanWebSocketFrame.writeServerCloseFrame(out, code = 1000, reason = "Normal Closure")

        val bytes = out.toByteArray()
        // Byte 0: FIN = 1, opcode = 0x8 (CLOSE) -> 0x88
        assertEquals(0x88.toByte(), bytes[0])
    }
}
