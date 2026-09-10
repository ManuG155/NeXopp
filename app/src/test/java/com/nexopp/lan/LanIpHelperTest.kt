package com.nexopp.lan

import org.junit.Assert.*
import org.junit.Test

class LanIpHelperTest {

    @Test
    fun isPrivateLanIp_classA() {
        assertTrue(LanIpHelper.isPrivateLanIp("10.0.0.1"))
        assertTrue(LanIpHelper.isPrivateLanIp("10.254.1.50"))
    }

    @Test
    fun isPrivateLanIp_classB() {
        assertTrue(LanIpHelper.isPrivateLanIp("172.16.0.1"))
        assertTrue(LanIpHelper.isPrivateLanIp("172.25.10.20"))
        assertTrue(LanIpHelper.isPrivateLanIp("172.31.255.254"))
        assertFalse(LanIpHelper.isPrivateLanIp("172.15.0.1"))
        assertFalse(LanIpHelper.isPrivateLanIp("172.32.0.1"))
    }

    @Test
    fun isPrivateLanIp_classC() {
        assertTrue(LanIpHelper.isPrivateLanIp("192.168.1.1"))
        assertTrue(LanIpHelper.isPrivateLanIp("192.168.0.105"))
        assertTrue(LanIpHelper.isPrivateLanIp("192.168.100.250"))
        assertFalse(LanIpHelper.isPrivateLanIp("192.169.1.1"))
    }

    @Test
    fun isPrivateLanIp_loopbackAndPublic_rejected() {
        assertFalse(LanIpHelper.isPrivateLanIp("127.0.0.1"))
        assertFalse(LanIpHelper.isPrivateLanIp("8.8.8.8"))
        assertFalse(LanIpHelper.isPrivateLanIp("1.1.1.1"))
        assertFalse(LanIpHelper.isPrivateLanIp(null))
        assertFalse(LanIpHelper.isPrivateLanIp(""))
        assertFalse(LanIpHelper.isPrivateLanIp("not-an-ip"))
    }
}
