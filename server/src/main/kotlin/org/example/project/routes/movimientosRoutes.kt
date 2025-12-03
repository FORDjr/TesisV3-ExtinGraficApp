package org.example.project.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import org.example.project.models.ActualizarMovimientoInventarioRequest
import org.example.project.models.AprobarMovimientoRequest
import org.example.project.models.CrearMovimientoInventarioRequest
import org.example.project.models.EstadoAprobacionMovimiento
import org.example.project.models.TipoMovimientoInventario
import org.example.project.services.MovimientosInventarioService

fun Route.movimientosRoutes() {
    val service = MovimientosInventarioService()

    route("/api/movimientos") {
        get {
            try {
                val productoId = call.request.queryParameters["productoId"]?.toIntOrNull()
                val tipoParam = call.request.queryParameters["tipo"]?.uppercase()
                val tipo = try { tipoParam?.let { TipoMovimientoInventario.valueOf(it) } } catch (_: Exception) { null }
                if (tipoParam != null && tipo == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Tipo de movimiento inválido"))
                    return@get
                }
                val desde = call.request.queryParameters["desde"]?.let { parseDateOrDateTimeStart(it) }
                val hasta = call.request.queryParameters["hasta"]?.let { parseDateOrDateTimeEnd(it) }
                val estadoParam = call.request.queryParameters["estado"]?.uppercase()
                val estado = try { estadoParam?.let { EstadoAprobacionMovimiento.valueOf(it) } } catch (_: Exception) { null }
                if (estadoParam != null && estado == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Estado de aprobación inválido"))
                    return@get
                }
                val limit = call.request.queryParameters["limit"]?.toIntOrNull()?.coerceIn(1, 200) ?: 50
                val offset = call.request.queryParameters["offset"]?.toIntOrNull()?.coerceAtLeast(0) ?: 0

                val filtros = MovimientosInventarioService.Filtros(
                    productoId = productoId,
                    tipo = tipo,
                    desde = desde,
                    hasta = hasta,
                    estado = estado
                )
                val page = service.listarMovimientos(filtros, limit, offset)
                call.respond(HttpStatusCode.OK, page)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Error al listar movimientos")))
            }
        }

        post {
            try {
                val request = call.receive<CrearMovimientoInventarioRequest>()
                val movimiento = service.crearMovimiento(request)
                call.respond(HttpStatusCode.Created, movimiento)
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Error al crear movimiento")))
            }
        }

        get("/kardex") {
            try {
                val productoId = call.request.queryParameters["productoId"]?.toIntOrNull()
                if (productoId == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "productoId es requerido"))
                    return@get
                }
                val tipoParam = call.request.queryParameters["tipo"]?.uppercase()
                val tipo = try { tipoParam?.let { TipoMovimientoInventario.valueOf(it) } } catch (_: Exception) { null }
                val desde = call.request.queryParameters["desde"]?.let { parseDateOrDateTimeStart(it) }
                val hasta = call.request.queryParameters["hasta"]?.let { parseDateOrDateTimeEnd(it) }
                val estadoParam = call.request.queryParameters["estado"]?.uppercase()
                val estado = try { estadoParam?.let { EstadoAprobacionMovimiento.valueOf(it) } } catch (_: Exception) { null }
                if (tipoParam != null && tipo == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Tipo inválido"))
                    return@get
                }
                if (estadoParam != null && estado == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Estado de aprobación inválido"))
                    return@get
                }
                val kardex = service.obtenerKardex(productoId, tipo, desde, hasta, estado)
                call.respond(kardex)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Error al generar kardex")))
            }
        }

        get("/export/csv") {
            try {
                val filtros = extractFiltros(call)
                val csv = service.exportarCsv(filtros)
                call.response.headers.append(
                    HttpHeaders.ContentDisposition,
                    "attachment; filename=\"kardex-${System.currentTimeMillis()}.csv\""
                )
                call.respondText(csv, ContentType.parse("text/csv"))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Error al exportar CSV")))
            }
        }

        get("/export/pdf") {
            try {
                val filtros = extractFiltros(call)
                val pdf = service.exportarPdf(filtros)
                call.response.headers.append(
                    HttpHeaders.ContentDisposition,
                    "attachment; filename=\"kardex-${System.currentTimeMillis()}.pdf\""
                )
                call.respondBytes(pdf, ContentType.Application.Pdf)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Error al exportar PDF")))
            }
        }

        get("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
                return@get
            }
            val movimiento = service.obtenerMovimiento(id)
            if (movimiento == null) call.respond(HttpStatusCode.NotFound, mapOf("error" to "No encontrado"))
            else call.respond(movimiento)
        }

        put("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
                return@put
            }
            val request = try { call.receive<ActualizarMovimientoInventarioRequest>() } catch (_: Exception) { null }
            if (request == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Body inválido"))
                return@put
            }
            val actualizado = service.actualizarMovimiento(id, request)
            if (actualizado == null) call.respond(HttpStatusCode.NotFound, mapOf("error" to "No encontrado"))
            else call.respond(actualizado)
        }

        delete("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
                return@delete
            }
            try {
                val eliminado = service.eliminarMovimiento(id)
                if (eliminado) call.respond(mapOf("deleted" to true))
                else call.respond(HttpStatusCode.NotFound, mapOf("error" to "No encontrado"))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Error al eliminar movimiento")))
            }
        }

        post("/{id}/aprobar") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
                return@post
            }
            val body = try { call.receive<AprobarMovimientoRequest>() } catch (_: Exception) { null }
            if (body == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Body inválido"))
                return@post
            }
            try {
                val res = service.resolverMovimiento(id, body.aprobado, body.usuarioId, body.observaciones)
                call.respond(res)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Error al resolver movimiento")))
            }
        }
    }
}

private fun extractFiltros(call: ApplicationCall): MovimientosInventarioService.Filtros {
    val productoId = call.request.queryParameters["productoId"]?.toIntOrNull()
    val tipoParam = call.request.queryParameters["tipo"]?.uppercase()
    val tipo = try { tipoParam?.let { TipoMovimientoInventario.valueOf(it) } } catch (_: Exception) { null }
    val desde = call.request.queryParameters["desde"]?.let { parseDateOrDateTimeStart(it) }
    val hasta = call.request.queryParameters["hasta"]?.let { parseDateOrDateTimeEnd(it) }
    val estadoParam = call.request.queryParameters["estado"]?.uppercase()
    val estado = try { estadoParam?.let { EstadoAprobacionMovimiento.valueOf(it) } } catch (_: Exception) { null }
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
        val date = LocalDate.parse(input) // YYYY-MM-DD
        LocalDateTime(date.year, date.monthNumber, date.dayOfMonth, 0, 0, 0)
    }

private fun parseDateOrDateTimeEnd(input: String): LocalDateTime =
    try {
        LocalDateTime.parse(input)
    } catch (_: Exception) {
        val date = LocalDate.parse(input) // YYYY-MM-DD
        // Fin de día inclusivo
        LocalDateTime(date.year, date.monthNumber, date.dayOfMonth, 23, 59, 59, 999_999_999)
    }
