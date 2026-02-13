package com.example.silentguardian

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log

class ContextManager(private val context: Context) {

    fun isTrustedEnvironment(): Boolean {
        val sharedPref = context.getSharedPreferences("SilentGuardianVault", Context.MODE_PRIVATE)
        val trustedSSID = sharedPref.getString("trusted_ssid", "") ?: ""

        if (trustedSSID.isEmpty()) return false

        // Removing quotes is critical as Android returns SSIDs wrapped in ""
        val currentSSID = getCurrentWifiSSID().replace("\"", "").trim()
        val cleanTrusted = trustedSSID.replace("\"", "").trim()

        Log.d("VaultSecurity", "Comparing: Current[$currentSSID] vs Trusted[$cleanTrusted]")

        if (currentSSID == "<unknown ssid>" || currentSSID == "unknown_network") {
            return false
        }

        return currentSSID.equals(cleanTrusted, ignoreCase = true)
    }

    /**
     * Function is now PUBLIC so HomeFragment can use it for UI updates.
     */
    fun getCurrentWifiSSID(): String {
        return try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val info = wifiManager.connectionInfo
            info.ssid ?: "unknown_network"
        } catch (e: Exception) {
            "error_retrieving"
        }
    }
}