package org.example.project.data.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.example.project.LOCAL_SERVER_URL
import org.example.project.preferredBaseUrl

class MaintenanceApiService {
    private val json = Json { ignoreUnknownKeys = true }
    private val http = HttpClient {
        install(ContentNegotiation) { json(json) }
        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    println("MaintenanceApiService: $message")
                }
            }
            level = LogLevel.INFO
        }
    }

    private val candidates = listOf(preferredBaseUrl(), LOCAL_SERVER_URL, "http://10.0.2.2:8080")

    private suspend fun <T> firstSuccessful(block: suspend (String) -> T): T {
        val errors = mutableListOf<String>()
        for (base in candidates) {
            try {
                return block(base)
            } catch (e: Exception) {
                errors += "${base}: ${e.message}"
            }
        }
        throw IllegalStateException("Sin servidor disponible: ${errors.joinToString(" | ")}")
    }

    suspend fun obtenerExtintores(): List<RemoteExtintor> = firstSuccessful { base ->
        http.get("$base/api/extintores").body()
    }

    suspend fun obtenerOrdenes(): List<RemoteOrdenServicio> = firstSuccessful { base ->
        http.get("$base/api/ordenes").body()
    }

    suspend fun obtenerClientes(): List<RemoteCliente> = firstSuccessful { base ->
        http.get("$base/api/clientes").body()
    }

    suspend fun obtenerSedes(): List<RemoteSede> = firstSuccessful { base ->
        http.get("$base/api/sedes").body()
    }
}
