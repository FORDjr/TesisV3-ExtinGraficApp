package org.example.project.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import org.example.project.models.CrearMovimientoInventarioRequest
import org.example.project.models.EstadoAprobacionMovimiento
import org.example.project.models.IntegrationScope
import org.example.project.models.TipoMovimientoInventario
import org.example.project.services.IntegracionesService
import org.example.project.services.MovimientosInventarioService

fun Route.integracionesRoutes() {
    val service = IntegracionesService()

    route("/api/integraciones/{token}") {

        get("/status") {
            val integ = call.validateIntegration(service) ?: return@get
            call.respond(service.estado(integ))
        }

        get("/inventario/resumen") {
            val integ = call.validateIntegration(service, setOf(IntegrationScope.INVENTARIO_READ)) ?: return@get
            call.respond(service.resumenInventario())
        }

        get("/movimientos") {
            val integ = call.validateIntegration(service, setOf(IntegrationScope.MOVIMIENTOS_READ)) ?: return@get
            val filtros = extractIntegrationFilters(call) ?: return@get
            val limit = call.request.queryParameters["limit"]?.toIntOrNull()?.coerceIn(1, 200) ?: 50
            val offset = call.request.queryParameters["offset"]?.toIntOrNull()?.coerceAtLeast(0) ?: 0
            val page = service.listarMovimientos(filtros, limit, offset)
            call.respond(page)
        }

        post("/movimientos") {
            val integ = call.validateIntegration(service, setOf(IntegrationScope.MOVIMIENTOS_WRITE)) ?: return@post
            val request = try { call.receive<CrearMovimientoInventarioRequest>() } catch (_: Exception) { null }
            if (request == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Body inválido"))
                return@post
            }
            try {
                val creado = service.crearMovimiento(request)
                call.respond(HttpStatusCode.Created, creado)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "No se pudo registrar el movimiento")))
            }
        }

        get("/reportes/kardex.csv") {
            val integ = call.validateIntegration(service, setOf(IntegrationScope.REPORTES_READ)) ?: return@get
            val filtros = extractIntegrationFilters(call) ?: return@get
            val csv = service.exportarCsv(filtros)
            call.response.headers.append(
                HttpHeaders.ContentDisposition,
                "attachment; filename=\"kardex-ext-${System.currentTimeMillis()}.csv\""
            )
            call.respondText(csv, ContentType.parse("text/csv"))
        }
    }
}

private suspend fun ApplicationCall.validateIntegration(
    service: IntegracionesService,
    scopes: Set<IntegrationScope> = emptySet()
): org.example.project.models.Integracion? {
    val token = parameters["token"]
    if (token.isNullOrBlank()) {
        respond(HttpStatusCode.BadRequest, mapOf("error" to "Token requerido en la ruta"))
        return null
    }
    return try {
        service.validarToken(token, scopes)
    } catch (e: Exception) {
        val message = e.message ?: "Token inválido"
        val status = if (message.contains("permiso", ignoreCase = true)) HttpStatusCode.Forbidden else HttpStatusCode.Unauthorized
        respond(status, mapOf("error" to message))
        null
    }
}

private suspend fun extractIntegrationFilters(call: ApplicationCall): MovimientosInventarioService.Filtros? {
    val productoId = call.request.queryParameters["productoId"]?.toIntOrNull()
    val tipoParam = call.request.queryParameters["tipo"]?.uppercase()
    val tipo = try { tipoParam?.let { TipoMovimientoInventario.valueOf(it) } } catch (_: Exception) { null }
    if (tipoParam != null && tipo == null) {
        call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Tipo de movimiento inválido"))
        return null
    }
    val desde = call.request.queryParameters["desde"]?.let { parseDateOrDateTimeStart(it) }
    val hasta = call.request.queryParameters["hasta"]?.let { parseDateOrDateTimeEnd(it) }
    val estadoParam = call.request.queryParameters["estado"]?.uppercase()
    val estado = try { estadoParam?.let { EstadoAprobacionMovimiento.valueOf(it) } } catch (_: Exception) { null }
    if (estadoParam != null && estado == null) {
        call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Estado de aprobación inválido"))
        return null
    }
    return MovimientosInventarioService.Filtros(
        productoId = productoId,
        tipo = tipo,
        desde = desde,
        hasta = hasta,
        estado = estado
    )
}

private fun parseDateOrDateTimeStart(input: String): LocalDateTime =
    try {
        LocalDateTime.parse(input)
    } catch (_: Exception) {
        val date = LocalDate.parse(input)
        LocalDateTime(date.year, date.monthNumber, date.dayOfMonth, 0, 0, 0)
    }

private fun parseDateOrDateTimeEnd(input: String): LocalDateTime =
    try {
        LocalDateTime.parse(input)
    } catch (_: Exception) {
        val date = LocalDate.parse(input)
        LocalDateTime(date.year, date.monthNumber, date.dayOfMonth, 23, 59, 59, 999_999_999)
    }
