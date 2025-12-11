package org.example.project.data.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.example.project.LOCAL_SERVER_URL
import org.example.project.preferredBaseUrl

class HealthApiService {
    private val client = HttpClient {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        install(Logging) { level = LogLevel.INFO }
    }

    private val bases = listOf(preferredBaseUrl(), LOCAL_SERVER_URL, "http://10.0.2.2:8080")

    private suspend fun <T> firstSuccessful(block: suspend (String) -> T): T? {
        bases.forEach { base ->
            try {
                return block(base)
            } catch (_: Exception) {
            }
        }
        return null
    }

    suspend fun ping(): Boolean = firstSuccessful { base ->
        client.get("$base/health").status.isSuccess()
    } ?: false

    suspend fun db(): Boolean = firstSuccessful { base ->
        client.get("$base/health/db").status.isSuccess()
    } ?: false

    suspend fun info(): HealthInfo? = firstSuccessful { base ->
        client.get("$base/health/info").body()
    }

    fun close() = client.close()
}

@Serializable
data class HealthInfo(
    val status: String,
    val version: String,
    val environment: String,
    val db: String,
    val timestamp: Long
)
