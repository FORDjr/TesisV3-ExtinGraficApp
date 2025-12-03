package org.example.project.data.api

import io.ktor.client.HttpClient
import org.example.project.network.provideHttpClient

/**
 * Configuración específica de red para Android (obsoleto: se centraliza en shared/provideHttpClient)
 */
@Deprecated("Usar provideHttpClient() desde shared")
object AndroidNetworkConfig {

    fun createHttpClient(): HttpClient = provideHttpClient()
}
