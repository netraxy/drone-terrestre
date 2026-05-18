package com.clement.droneterrestre

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSpecifier
import android.os.Build
import androidx.annotation.RequiresApi

data class WifiNet(
    val ssid: String,
    val level: Int,        // dBm
    val secured: Boolean
)

class WifiConnector(private val context: Context) {

    private val wifiManager = context.applicationContext
        .getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val connectivityManager = context.applicationContext
        .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private var currentCallback: ConnectivityManager.NetworkCallback? = null

    @SuppressLint("MissingPermission")
    fun scan(): List<WifiNet> {
        return try {
            wifiManager.startScan()
            wifiManager.scanResults
                .filter { it.SSID.isNotBlank() }
                .distinctBy { it.SSID }
                .sortedByDescending { it.level }
                .map {
                    WifiNet(
                        ssid = it.SSID,
                        level = it.level,
                        secured = isSecured(it)
                    )
                }
        } catch (e: SecurityException) {
            emptyList()
        }
    }

    private fun isSecured(s: ScanResult): Boolean {
        val caps = s.capabilities
        return caps.contains("WPA") || caps.contains("WEP") || caps.contains("PSK") || caps.contains("EAP")
    }

    /**
     * Connecte le téléphone au réseau WiFi donné.
     * Sur Android 10+, Android affiche un dialog système pour que l'utilisateur confirme.
     * Une fois connecté, onConnected(network) est appelé. Le trafic de l'app est
     * automatiquement routé via ce réseau (binding).
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    fun connect(
        ssid: String,
        password: String,
        secured: Boolean,
        onConnected: () -> Unit,
        onError: (String) -> Unit
    ) {
        disconnect() // ferme toute connexion en cours

        val specifierBuilder = WifiNetworkSpecifier.Builder().setSsid(ssid)
        if (secured && password.isNotBlank()) {
            specifierBuilder.setWpa2Passphrase(password)
        }
        val specifier = specifierBuilder.build()

        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .setNetworkSpecifier(specifier)
            .build()

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                connectivityManager.bindProcessToNetwork(network)
                onConnected()
            }
            override fun onUnavailable() {
                onError("Connexion impossible (mauvais mot de passe ou réseau hors de portée)")
            }
            override fun onLost(network: Network) {
                connectivityManager.bindProcessToNetwork(null)
            }
        }
        currentCallback = callback

        try {
            connectivityManager.requestNetwork(request, callback, 30_000)
        } catch (e: Exception) {
            onError("Erreur : ${e.message}")
        }
    }

    fun disconnect() {
        currentCallback?.let {
            try {
                connectivityManager.unregisterNetworkCallback(it)
            } catch (_: Exception) {}
        }
        currentCallback = null
        try {
            connectivityManager.bindProcessToNetwork(null)
        } catch (_: Exception) {}
    }
}
