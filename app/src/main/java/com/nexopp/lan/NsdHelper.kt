package com.nexopp.lan

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo

/**
 * Handles local network service registration via Android NSD (mDNS / DNS-SD).
 * Advertises "_nexopp._tcp" for zero-conf discovery on the LAN.
 * All errors are handled gracefully without breaking the primary QR / direct IP flow.
 */
class NsdHelper(context: Context) {

    private val nsdManager: NsdManager? =
        context.applicationContext.getSystemService(Context.NSD_SERVICE) as? NsdManager

    private var registrationListener: NsdManager.RegistrationListener? = null
    private var isRegistered = false

    @Synchronized
    fun registerService(port: Int, serviceName: String = "FiXmyNotes-Tablet") {
        unregisterService()

        val manager = nsdManager ?: return
        val serviceInfo = NsdServiceInfo().apply {
            this.serviceName = serviceName
            this.serviceType = "_nexopp._tcp"
            this.port = port
        }

        val listener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(NsdServiceInfo: NsdServiceInfo) {
                isRegistered = true
            }

            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                isRegistered = false
            }

            override fun onServiceUnregistered(arg0: NsdServiceInfo) {
                isRegistered = false
            }

            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                isRegistered = false
            }
        }

        registrationListener = listener
        try {
            manager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, listener)
        } catch (_: Exception) {
            isRegistered = false
        }
    }

    @Synchronized
    fun unregisterService() {
        val listener = registrationListener ?: return
        val manager = nsdManager ?: return

        try {
            manager.unregisterService(listener)
        } catch (_: Exception) {
        } finally {
            registrationListener = null
            isRegistered = false
        }
    }
}
