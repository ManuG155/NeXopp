package com.nexopp.lan

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Collections

object LanIpHelper {

    /**
     * Resolves the primary local IPv4 address of the Android device on the Wi-Fi or LAN network.
     * Prefers non-loopback site-local / private addresses (192.168.x.x, 10.x.x.x, 172.16-31.x.x).
     */
    fun getLocalWifiIpAddress(context: Context? = null): String? {
        // Method 1: Scan NetworkInterface list (most reliable across Android versions)
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            // First check Wi-Fi interfaces specifically (wlan0, etc.)
            for (intf in interfaces) {
                if (intf.name.startsWith("wlan", ignoreCase = true) ||
                    intf.name.startsWith("eth", ignoreCase = true)
                ) {
                    val addresses = Collections.list(intf.inetAddresses)
                    for (addr in addresses) {
                        if (!addr.isLoopbackAddress && addr is Inet4Address) {
                            val ip = addr.hostAddress
                            if (isPrivateLanIp(ip)) {
                                return ip
                            }
                        }
                    }
                }
            }

            // Fallback to any non-loopback private IPv4 address
            for (intf in interfaces) {
                val addresses = Collections.list(intf.inetAddresses)
                for (addr in addresses) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        val ip = addr.hostAddress
                        if (isPrivateLanIp(ip)) {
                            return ip
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // Fall through to WifiManager fallback
        }

        // Method 2: Legacy WifiManager fallback
        if (context != null) {
            try {
                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                val ipInt = wifiManager?.connectionInfo?.ipAddress ?: 0
                if (ipInt != 0) {
                    return formatIpAddress(ipInt)
                }
            } catch (_: Exception) {
            }
        }

        return null
    }

    /**
     * Checks whether the given IPv4 address belongs to RFC 1918 private address ranges.
     */
    fun isPrivateLanIp(ip: String?): Boolean {
        if (ip == null || ip.isEmpty() || ip == "127.0.0.1") return false
        val parts = ip.split(".")
        if (parts.size != 4) return false
        val first = parts[0].toIntOrNull() ?: return false
        val second = parts[1].toIntOrNull() ?: return false

        return when {
            // 10.0.0.0 - 10.255.255.255
            first == 10 -> true
            // 172.16.0.0 - 172.31.255.255
            first == 172 && second in 16..31 -> true
            // 192.168.0.0 - 192.168.255.255
            first == 192 && second == 168 -> true
            else -> false
        }
    }

    /**
     * Checks if the device is currently connected to an active Wi-Fi or Ethernet network.
     */
    fun isWifiConnected(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }

    private fun formatIpAddress(ip: Int): String {
        return "${ip and 0xFF}.${ip shr 8 and 0xFF}.${ip shr 16 and 0xFF}.${ip shr 24 and 0xFF}"
    }
}
