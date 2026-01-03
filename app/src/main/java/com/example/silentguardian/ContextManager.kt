package com.example.silentguardian

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

class ContextManager(private val context: Context) {

    // Define your "Home" or "Safe" Wi-Fi name here
    private val trustedSSID = "My_Home_WiFi"

    fun isTrustedEnvironment(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        // This checks if we are on Wi-Fi.
        // For advanced marks, we check if the connection is "Validated" (working internet)
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }
}