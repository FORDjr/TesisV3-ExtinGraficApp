package org.example.project

// Configuración de URLs (solo cliente)
const val LOCAL_SERVER_URL = "http://localhost:8080" // Desktop/iOS
const val ANDROID_HOST_URL = "http://10.0.2.2:8080" // Emulador Android
const val TUNNEL_BASE_URL = "" // URL funcional del túnel (fallback estático)

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
