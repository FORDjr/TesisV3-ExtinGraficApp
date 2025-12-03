package org.example.project

/**
 * Platform-specific URL provided via build config / environment.
 * Returns null when no explicit configuration is available.
 */
expect fun platformConfiguredBaseUrl(): String?
