package org.example.project

import org.example.project.shared.BuildConfig as SharedBuildConfig

actual fun platformConfiguredBaseUrl(): String? =
    SharedBuildConfig.BASE_API_URL.takeIf { !it.isNullOrBlank() }
