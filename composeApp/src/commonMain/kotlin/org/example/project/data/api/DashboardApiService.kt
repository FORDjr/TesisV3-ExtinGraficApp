package org.example.project.data.api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.example.project.data.model.DashboardResponse
import org.example.project.data.model.AlertaDto
import org.example.project.data.model.DashboardScope
import org.example.project.data.model.DashboardVentasBlock
import org.example.project.data.model.DashboardCrecimientoBlock
import org.example.project.data.model.DashboardInventarioBlock
import org.example.project.data.model.DashboardExtintoresBlock
import org.example.project.data.model.DashboardAlertasBlock
import org.example.project.preferredBaseUrl
import org.example.project.LOCAL_SERVER_URL
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable

class DashboardApiService {
    private val http = HttpClient {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        install(Logging) { level = LogLevel.INFO }
    }
    private val candidates = listOf(preferredBaseUrl(), LOCAL_SERVER_URL, "http://10.0.2.2:8080")
    private var working: String? = null

    private suspend fun base(): String {
        val current = working
        if (current != null) return current
        candidates.forEach { c ->
            try { val r = http.get("$c/health"); if (r.status.isSuccess()) { working = c; return c } } catch (_: Exception) {}
        }
        throw IllegalStateException("Sin servidor disponible")
    }

    suspend fun fetchDashboard(clienteId: Int? = null, sedeId: Int? = null): DashboardResponse {
        val b = base()
        val url = buildString {
            append(b).append("/api/dashboard")
            if (clienteId != null || sedeId != null) {
                append("?")
                val p = mutableListOf<String>()
                clienteId?.let { p += "clienteId=$it" }
                sedeId?.let { p += "sedeId=$it" }
                append(p.joinToString("&"))
            }
        }
        val payload = http.get(url).bodyAsText()
        return decodeDashboard(payload)
    }

    suspend fun listarAlertasPendientes(): List<AlertaDto> {
        val b = base()
        return try { http.get("$b/api/alertas?pendientes=true").body() } catch (e: Exception) { emptyList() }
    }

    suspend fun generarAlertasVenc(): Int {
        val b = base(); return try { http.post("$b/api/alertas/generar").body<Map<String,Int>>() ["creadas"] ?: 0 } catch (_: Exception) {0}
    }

    suspend fun generarAlertasStock(): Int {
        val b = base(); return try { http.post("$b/api/alertas/generarStock").body<Map<String,Int>>() ["creadas"] ?: 0 } catch (_: Exception) {0}
    }

    suspend fun reenviarAlertas(): Int {
        val b = base(); return try { http.post("$b/api/alertas/reenviar").body<Map<String,Int>>() ["reenviadas"] ?: 0 } catch (_: Exception) {0}
    }
}

private val relaxedJson = Json { ignoreUnknownKeys = true }

private fun decodeDashboard(payload: String): DashboardResponse {
    return try {
        relaxedJson.decodeFromString(DashboardResponse.serializer(), payload)
    } catch (_: SerializationException) {
        val legacy = relaxedJson.decodeFromString(LegacyDashboardResponse.serializer(), payload)
        legacy.toNew()
    }
}

@Serializable
private data class LegacyDashboardResponse(
    val timestamp: Long = 0L,
    val cacheTtlMs: Long = 0L,
    val clienteId: String? = null,
    val sedeId: String? = null,
    val extintoresTotal: Int = 0,
    val extintoresRojo: Int = 0,
    val extintoresAmarillo: Int = 0,
    val extintoresVerde: Int = 0,
    val extintoresVencen30: Int = 0,
    val stockCritico: Int = 0,
    val ventasMes: Long = 0L
)

private fun LegacyDashboardResponse.toNew(): DashboardResponse {
    val scope = DashboardScope(
        clienteId = clienteId?.toIntOrNull(),
        sedeId = sedeId?.toIntOrNull()
    )
    val ventasBlock = DashboardVentasBlock(
        hoy = ventasMes,
        mes = ventasMes,
        ordenesHoy = extintoresVencen30,
        ticketPromedio = if (extintoresVencen30 > 0) ventasMes / extintoresVencen30 else ventasMes,
        crecimiento = DashboardCrecimientoBlock(0, 0, 0, 0)
    )
    val inventario = DashboardInventarioBlock(
        totalProductos = extintoresTotal,
        stockCritico = stockCritico,
        extintores = DashboardExtintoresBlock(
            total = extintoresTotal,
            rojo = extintoresRojo,
            amarillo = extintoresAmarillo,
            verde = extintoresVerde,
            vencen30 = extintoresVencen30
        )
    )
    val alertas = DashboardAlertasBlock(
        pendientes = 0,
        porTipo = emptyList()
    )
    return DashboardResponse(
        generatedAt = timestamp,
        scope = scope,
        ventas = ventasBlock,
        inventario = inventario,
        alertas = alertas
    )
}
