package com.nexopp.lan

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class PairingTokenManagerTest {

    private lateinit var manager: PairingTokenManager

    @Before
    fun setup() {
        manager = PairingTokenManager(defaultSessionDurationMs = 5000L) // 5 seconds for test
    }

    @Test
    fun sessionToken_has256BitsOfEntropy() {
        val session = manager.createSession()
        // 32 bytes in hex = 64 characters
        assertEquals(64, session.sessionToken.length)
        assertTrue("Token must be hexadecimal", session.sessionToken.matches(Regex("^[0-9a-fA-F]{64}$")))
    }

    @Test
    fun pairingCode_hasCorrectFormatAndSafeAlphabet() {
        val session = manager.createSession()
        // Pattern XXXX-XXX using alphabet 23456789ABCDEFGHJKLMNPQRSTUVWXYZ (length 8: 4 + 1 + 3)
        assertEquals(8, session.pairingCode.length)
        assertTrue(
            "Pairing code must match safe alphabet pattern",
            session.pairingCode.matches(Regex("^[23456789ABCDEFGHJKLMNPQRSTUVWXYZ]{4}-[23456789ABCDEFGHJKLMNPQRSTUVWXYZ]{3}$"))
        )
    }

    @Test
    fun validateToken_returnsTrueForValidSession() {
        val session = manager.createSession()
        assertTrue(manager.validateToken(session.sessionToken))
        assertFalse(manager.validateToken("invalid-token-123456"))
        assertFalse(manager.validateToken(null))
        assertFalse(manager.validateToken(""))
    }

    @Test
    fun validatePairingCode_resolvesSessionToken() {
        val session = manager.createSession()
        // Exact match
        assertEquals(session.sessionToken, manager.validatePairingCode(session.pairingCode))
        // Lowercase
        assertEquals(session.sessionToken, manager.validatePairingCode(session.pairingCode.lowercase()))
        // Without hyphen
        assertEquals(session.sessionToken, manager.validatePairingCode(session.pairingCode.replace("-", "")))
        // Invalid code
        assertNull(manager.validatePairingCode("0000-000"))
    }

    @Test
    fun revokeSession_invalidatesTokenAndCode() {
        val session = manager.createSession()
        assertTrue(manager.validateToken(session.sessionToken))

        manager.revokeSession(session.sessionToken)

        assertFalse(manager.validateToken(session.sessionToken))
        assertNull(manager.validatePairingCode(session.pairingCode))
    }

    @Test
    fun revokeAll_clearsAllSessions() {
        val s1 = manager.createSession()
        val s2 = manager.createSession()

        assertTrue(manager.validateToken(s1.sessionToken))
        assertTrue(manager.validateToken(s2.sessionToken))

        manager.revokeAll()

        assertFalse(manager.validateToken(s1.sessionToken))
        assertFalse(manager.validateToken(s2.sessionToken))
    }

    @Test
    fun expiredSession_isRejected() {
        // Create session with 1ms duration
        val session = manager.createSession(durationMs = 1L)
        Thread.sleep(15L) // wait for expiration

        assertFalse(manager.validateToken(session.sessionToken))
        assertNull(manager.validatePairingCode(session.pairingCode))
    }
}
