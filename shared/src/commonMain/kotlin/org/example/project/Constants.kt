package org.example.project

// Configuración de URLs (solo cliente)
const val LOCAL_SERVER_URL = "http://190.12.170.98:1609" // IP pública expuesta (desktop/iOS)
const val ANDROID_HOST_URL = "http://190.12.170.98:1609" // Android usa la misma IP pública
const val TUNNEL_BASE_URL = "https://shantae-nonimaginational-rima.ngrok-free.dev" // URL pública ngrok

// Timeouts HTTP
const val HTTP_CONNECTION_TIMEOUT = 30000L
const val HTTP_READ_TIMEOUT = 60000L

private var runtimeOverride: String? = null

fun overrideBaseUrl(url: String?) {
    runtimeOverride = url?.takeIf { it.isNotBlank() }
}

// URL preferida (override -> build config -> túnel -> default por plataforma)
fun preferredBaseUrl(): String {
    runtimeOverride?.let { return it }
    platformConfiguredBaseUrl()?.takeIf { it.isNotBlank() && it != "null" }?.let { return it }
    if (TUNNEL_BASE_URL.isNotBlank()) return TUNNEL_BASE_URL
    val platformName = getPlatform().name.lowercase()
    return if (platformName.contains("android")) ANDROID_HOST_URL else LOCAL_SERVER_URL
}
