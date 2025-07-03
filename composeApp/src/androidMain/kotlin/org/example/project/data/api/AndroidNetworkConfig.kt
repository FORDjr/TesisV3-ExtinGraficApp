package org.example.project.data.api

import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.HttpTimeout
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.example.project.*

/**
 * Configuración específica de red para Android
 * Optimizada para trabajar con VPN universitaria
 */
object AndroidNetworkConfig {

    fun createHttpClient(): HttpClient {
        return HttpClient(Android) {
            engine {
                // Configuraciones específicas para Android
                connectTimeout = CONNECTION_TIMEOUT.toInt()
                socketTimeout = READ_TIMEOUT.toInt()

                // Permitir conexiones a través de VPN
                proxy = null // No usar proxy automático
            }

            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    isLenient = true
                    ignoreUnknownKeys = true
                })
            }

            install(HttpTimeout) {
                requestTimeoutMillis = CONNECTION_TIMEOUT
                connectTimeoutMillis = CONNECTION_TIMEOUT
                socketTimeoutMillis = READ_TIMEOUT
            }

            install(Logging) {
                logger = Logger.DEFAULT
                level = LogLevel.INFO
            }
        }
    }
}
