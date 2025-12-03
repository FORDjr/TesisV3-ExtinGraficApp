package org.example.project

actual fun platformConfiguredBaseUrl(): String? =
    System.getenv("SERVER_BASE_URL") ?: System.getProperty("SERVER_BASE_URL")
