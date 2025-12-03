package org.example.project.data.api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.example.project.data.model.ExtintorResponse
import org.example.project.data.model.CalendarEvent
import org.example.project.preferredBaseUrl
import org.example.project.LOCAL_SERVER_URL

class CalendarApiService {
    companion object {
        private const val EXTINTORES_PATH = "/api/extintores"
        private const val ALERTAS_PATH = "/api/alertas" // opcional futuro
        private val EMULATOR_URL = "http://10.0.2.2:8080"
        private val CONNECTION_URLS: List<String> = listOf(
            preferredBaseUrl(),
            LOCAL_SERVER_URL,
            EMULATOR_URL
        )
    }

    private val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = false
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 15000L
            connectTimeoutMillis = 10000L
            socketTimeoutMillis = 15000L
        }
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.INFO
        }
    }

    private var workingUrl: String? = null

    private suspend fun testConnectionQuick(url: String): Boolean = try {
        val resp = httpClient.get("$url/health")
        resp.status.isSuccess()
    } catch (_: Exception) { false }

    private suspend fun findWorkingUrl(): String? {
        workingUrl?.let { if (testConnectionQuick(it)) return it else workingUrl = null }
        for (u in CONNECTION_URLS) {
            if (testConnectionQuick(u)) { workingUrl = u; return u }
            // fallback probando directamente extintores
            try {
                val r = httpClient.get("$u$EXTINTORES_PATH")
                if (r.status.isSuccess()) { workingUrl = u; return u }
            } catch (_: Exception) {}
        }
        return null
    }

    private suspend fun <T> withUrl(block: suspend (String) -> T): T {
        val base = findWorkingUrl() ?: throw IllegalStateException("Sin servidor disponible")
        return try { block(base) } catch (e: Exception) {
            workingUrl = null
            val nuevo = findWorkingUrl() ?: throw e
            block(nuevo)
        }
    }

    suspend fun obtenerExtintores(): List<ExtintorResponse> = try {
        withUrl { url ->
            httpClient.get("$url$EXTINTORES_PATH").body()
        }
    } catch (e: Exception) {
        println("CalendarApiService error extintores: ${e.message}")
        emptyList()
    }

    suspend fun obtenerEventosCalendario(): List<CalendarEvent> {
        val extintores = obtenerExtintores()
        return extintores.filter { it.fechaProximoVencimiento != null }.map {
            val fecha = it.fechaProximoVencimiento ?: ""
            CalendarEvent(
                id = it.id,
                title = "Venc. ${it.codigoQr}",
                date = fecha.take(10),
                rawDateTime = fecha,
                daysToExpire = it.diasParaVencer,
                color = it.color,
                type = "EXTINTOR"
            )
        }
    }

    fun close() = httpClient.close()
}

