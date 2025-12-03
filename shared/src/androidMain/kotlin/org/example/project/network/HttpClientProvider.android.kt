package org.example.project.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.HttpTimeout
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.example.project.HTTP_CONNECTION_TIMEOUT
import org.example.project.HTTP_READ_TIMEOUT

fun provideHttpClient(): HttpClient = HttpClient(Android) {
    engine {
        connectTimeout = HTTP_CONNECTION_TIMEOUT.toInt()
        socketTimeout = HTTP_READ_TIMEOUT.toInt()
    }
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true; isLenient = true; prettyPrint = false })
    }
    install(HttpTimeout) {
        requestTimeoutMillis = HTTP_CONNECTION_TIMEOUT
        connectTimeoutMillis = HTTP_CONNECTION_TIMEOUT
        socketTimeoutMillis = HTTP_READ_TIMEOUT
    }
    install(Logging) {
        logger = Logger.DEFAULT
        level = LogLevel.INFO
    }
}
