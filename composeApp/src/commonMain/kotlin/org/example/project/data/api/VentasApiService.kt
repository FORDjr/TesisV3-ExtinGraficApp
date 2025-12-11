package org.example.project.data.api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.builtins.ListSerializer
import org.example.project.data.models.*
import org.example.project.data.model.InventarioPage
import org.example.project.data.model.Producto
import org.example.project.LOCAL_SERVER_URL
import org.example.project.preferredBaseUrl
import kotlin.math.roundToLong
import org.example.project.data.auth.AuthManager

class VentasApiService {
    companion object {
        private const val API_PATH_VENTAS = "/api/ventas"
        private const val API_PATH_INVENTARIO = "/api/inventario"
        private const val EMULATOR_URL = "http://10.0.2.2:8080"
        private val CONNECTION_URLS = listOf(
            preferredBaseUrl(),
            LOCAL_SERVER_URL,
            EMULATOR_URL
        )
        val json = Json { prettyPrint = true; isLenient = true; ignoreUnknownKeys = true }
    }

    private val httpClient = HttpClient {
        install(ContentNegotiation) { json(VentasApiService.json) }
        install(Logging) { logger = Logger.DEFAULT; level = LogLevel.INFO }
        install(io.ktor.client.plugins.DefaultRequest) {
            val token = AuthManager.getToken()
            if (token.isNotBlank()) {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
        }
    }

    private fun primaryUrls(): List<String> = CONNECTION_URLS

    private suspend fun <T> firstSuccessful(block: suspend (String) -> T): Result<T> {
        val errors = mutableListOf<String>()
        for (base in primaryUrls()) {
            try {
                return Result.success(block(base))
            } catch (e: Exception) {
                errors += "$base -> ${e.message ?: "Error desconocido"}"
            }
        }
        return Result.failure(Exception(errors.joinToString(" | ")))
    }

    suspend fun obtenerVentas(): Result<VentasListResponse> = firstSuccessful { base ->
        val payload = httpClient.get(base + API_PATH_VENTAS).bodyAsText()
        decodeVentasList(payload)
    }

    suspend fun obtenerMetricas(): Result<MetricasVentas> = firstSuccessful { base ->
        val payload = httpClient.get(base + API_PATH_VENTAS + "/metricas").bodyAsText()
        decodeMetricas(payload)
    }

    suspend fun crearVenta(nuevaVenta: NuevaVentaRequest): Result<Venta> = firstSuccessful { base ->
        val resp = httpClient.post(base + API_PATH_VENTAS) {
            contentType(ContentType.Application.Json)
            setBody(nuevaVenta)
        }
        if (resp.status == HttpStatusCode.Created || resp.status == HttpStatusCode.OK) {
            val payload = resp.bodyAsText()
            decodeVenta(payload)
        } else {
            throw IllegalStateException("HTTP ${resp.status}")
        }
    }

    suspend fun obtenerVentaPorId(id: String): Result<Venta> = firstSuccessful { base ->
        val payload = httpClient.get("$base$API_PATH_VENTAS/$id").bodyAsText()
        decodeVenta(payload)
    }

    suspend fun descargarComprobantePdf(id: String): Result<ByteArray> = firstSuccessful { base ->
        httpClient.get("$base$API_PATH_VENTAS/$id/comprobante/pdf").body()
    }

    suspend fun actualizarEstadoVenta(id: String, nuevoEstado: EstadoVenta): Result<Venta> {
        return firstSuccessful { base ->
            val resp = httpClient.patch("$base$API_PATH_VENTAS/$id/estado") {
                contentType(ContentType.Application.Json)
                setBody(mapOf("estado" to nuevoEstado))
            }
            if (resp.status == HttpStatusCode.OK) {
                val payload = resp.bodyAsText()
                decodeVenta(payload)
            } else throw IllegalStateException("HTTP ${resp.status}")
        }
    }

    suspend fun registrarDevolucionParcial(id: String, request: DevolucionParcialRequest): Result<Venta> =
        firstSuccessful { base ->
            val resp = httpClient.post("$base$API_PATH_VENTAS/$id/devolucion") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            if (resp.status == HttpStatusCode.OK) {
                decodeVenta(resp.bodyAsText())
            } else {
                throw IllegalStateException("HTTP ${resp.status}")
            }
        }

    suspend fun obtenerProductosParaVenta(): Result<List<Producto>> = firstSuccessful { base ->
        val resp: InventarioPage = httpClient.get(base + API_PATH_INVENTARIO) {
            parameter("estado", "ACTIVO")
            parameter("limit", 200)
        }.body()
        resp.items
    }
}

private fun decodeVentasList(payload: String): VentasListResponse {
    val jsonConfig = VentasApiService.json
    return try {
        jsonConfig.decodeFromString(VentasListResponse.serializer(), payload)
    } catch (_: SerializationException) {
        val legacyList = jsonConfig.decodeFromString(ListSerializer(LegacyVenta.serializer()), payload)
        val ventas = legacyList.map { it.toNew() }
        val metricas = MetricasVentas(
            ventasHoy = ventas.sumOf { it.total },
            ordenesHoy = ventas.size,
            ticketPromedio = if (ventas.isNotEmpty()) ventas.sumOf { it.total } / ventas.size else 0,
            ventasMes = ventas.sumOf { it.total },
            crecimientoVentasHoy = 0,
            crecimientoOrdenes = 0,
            crecimientoTicket = 0,
            crecimientoMes = 0
        )
        VentasListResponse(ventas = ventas, metricas = metricas)
    }
}

private fun decodeMetricas(payload: String): MetricasVentas {
    val jsonConfig = VentasApiService.json
    return try {
        jsonConfig.decodeFromString(MetricasVentas.serializer(), payload)
    } catch (_: SerializationException) {
        val legacy = jsonConfig.decodeFromString(LegacyMetricas.serializer(), payload)
        legacy.toNew()
    }
}

private fun decodeVenta(payload: String): Venta {
    val jsonConfig = VentasApiService.json
    return try {
        jsonConfig.decodeFromString(Venta.serializer(), payload)
    } catch (_: SerializationException) {
        val legacy = jsonConfig.decodeFromString(LegacyVenta.serializer(), payload)
        legacy.toNew()
    }
}

@Serializable
private data class LegacyVenta(
    val id: String = "",
    val cliente: String,
    val fecha: String,
    val total: Double,
    val estado: EstadoVenta,
    val metodoPago: MetodoPago,
    val observaciones: String? = null,
    val productos: List<LegacyProductoVenta> = emptyList()
)

@Serializable
private data class LegacyProductoVenta(
    val id: Int,
    val nombre: String,
    val cantidad: Int,
    val precio: Double,
    val subtotal: Double
)

@Serializable
private data class LegacyMetricas(
    val ventasHoy: Double = 0.0,
    val ordenesHoy: Int = 0,
    val ticketPromedio: Double = 0.0,
    val ventasMes: Double = 0.0,
    val crecimientoVentasHoy: Double = 0.0,
    val crecimientoOrdenes: Double = 0.0,
    val crecimientoTicket: Double = 0.0,
    val crecimientoMes: Double = 0.0
)

private fun LegacyMetricas.toNew(): MetricasVentas = MetricasVentas(
    ventasHoy = ventasHoy.roundToLong(),
    ordenesHoy = ordenesHoy,
    ticketPromedio = ticketPromedio.roundToLong(),
    ventasMes = ventasMes.roundToLong(),
    crecimientoVentasHoy = crecimientoVentasHoy.toInt(),
    crecimientoOrdenes = crecimientoOrdenes.toInt(),
    crecimientoTicket = crecimientoTicket.toInt(),
    crecimientoMes = crecimientoMes.toInt()
)

private fun LegacyVenta.toNew(): Venta = Venta(
    id = id,
    numero = id,
    cliente = cliente,
    fecha = fecha,
    subtotal = productos.sumOf { it.subtotal.roundToLong() },
    impuestos = 0L,
    total = total.roundToLong(),
    descuento = 0L,
    estado = estado,
    metodoPago = metodoPago,
    vendedorId = null,
    observaciones = observaciones,
    clienteFormal = null,
    totalDevuelto = 0L,
    productos = productos.map { it.toNew() }
)

private fun LegacyProductoVenta.toNew(): ProductoVenta = ProductoVenta(
    id = id,
    nombre = nombre,
    cantidad = cantidad,
    precio = precio.roundToLong(),
    subtotal = subtotal.roundToLong(),
    descuento = 0L,
    iva = 0.0,
    devuelto = 0
)
