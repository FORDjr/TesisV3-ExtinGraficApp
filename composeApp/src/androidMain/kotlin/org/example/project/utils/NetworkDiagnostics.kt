package org.example.project.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import java.net.InetAddress

/**
 * Utilidades para diagnosticar problemas de red en Android
 * Especialmente útil para debugging de conexiones VPN
 */
object NetworkDiagnostics {

    fun getNetworkInfo(context: Context): NetworkInfo {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetwork
        val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)

        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val wifiInfo = wifiManager.connectionInfo

        return NetworkInfo(
            isConnected = activeNetwork != null,
            isWifi = networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ?: false,
            isCellular = networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ?: false,
            isVPN = networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) ?: false,
            wifiSSID = wifiInfo.ssid?.replace("\"", "") ?: "Desconocido",
            localIP = getLocalIPAddress(),
            dnsServers = getDNSServers(context)
        )
    }

    private fun getLocalIPAddress(): String {
        return try {
            val interfaces = java.util.Collections.list(java.net.NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                val addrs = java.util.Collections.list(intf.inetAddresses)
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress && addr is java.net.Inet4Address) {
                        return addr.hostAddress ?: "Desconocido"
                    }
                }
            }
            "No encontrado"
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    private fun getDNSServers(context: Context): List<String> {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetwork = connectivityManager.activeNetwork
            val linkProperties = connectivityManager.getLinkProperties(activeNetwork)
            linkProperties?.dnsServers?.map { it.hostAddress } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun testConnectivity(urls: List<String>): ConnectivityTestResult {
        val results = mutableMapOf<String, Boolean>()

        for (url in urls) {
            results[url] = try {
                val cleanUrl = url.replace("http://", "").replace("https://", "").split(":")[0]
                val address = InetAddress.getByName(cleanUrl)
                address.isReachable(5000) // 5 segundos timeout
            } catch (e: Exception) {
                false
            }
        }

        return ConnectivityTestResult(results)
    }
}

data class NetworkInfo(
    val isConnected: Boolean,
    val isWifi: Boolean,
    val isCellular: Boolean,
    val isVPN: Boolean,
    val wifiSSID: String,
    val localIP: String,
    val dnsServers: List<String>
)

data class ConnectivityTestResult(
    val results: Map<String, Boolean>
) {
    fun getWorkingUrls(): List<String> = results.filter { it.value }.keys.toList()
    fun getFailedUrls(): List<String> = results.filter { !it.value }.keys.toList()
}
