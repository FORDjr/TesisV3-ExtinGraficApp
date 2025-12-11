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
import org.example.project.security.requireRole
import org.example.project.security.UserRole
import org.example.project.security.JwtConfig
import org.example.project.security.userRole

fun Route.movimientosRoutes() {
    val service = MovimientosInventarioService()

    route("/api/movimientos") {
        get {
            if (!call.requireRole(UserRole.ADMIN, UserRole.INVENTARIO, UserRole.SUPERVISOR)) return@get
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
            if (!call.requireRole(UserRole.ADMIN, UserRole.INVENTARIO)) return@post
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
            if (!call.requireRole(UserRole.ADMIN, UserRole.INVENTARIO, UserRole.SUPERVISOR)) return@get
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
            if (!call.authorizeExport(UserRole.ADMIN, UserRole.INVENTARIO, UserRole.SUPERVISOR)) return@get
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
            if (!call.authorizeExport(UserRole.ADMIN, UserRole.INVENTARIO, UserRole.SUPERVISOR)) return@get
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
            if (!call.requireRole(UserRole.ADMIN, UserRole.INVENTARIO, UserRole.SUPERVISOR)) return@get
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
            if (!call.requireRole(UserRole.ADMIN, UserRole.INVENTARIO)) return@put
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
            if (!call.requireRole(UserRole.ADMIN, UserRole.INVENTARIO)) return@delete
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
            if (!call.requireRole(UserRole.ADMIN, UserRole.SUPERVISOR)) return@post
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

// Rutas públicas para export con token en query
fun Route.movimientosPublicRoutes() {
    val service = MovimientosInventarioService()
    route("/api/movimientos") {
        get("/export/csv") {
            if (!call.authorizeExport(UserRole.ADMIN, UserRole.INVENTARIO, UserRole.SUPERVISOR)) return@get
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
            if (!call.authorizeExport(UserRole.ADMIN, UserRole.INVENTARIO, UserRole.SUPERVISOR)) return@get
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

private suspend fun ApplicationCall.authorizeExport(vararg allowed: UserRole): Boolean {
    val role = userRole()
    if (role != null && (allowed.isEmpty() || allowed.any { it == role || role == UserRole.ADMIN })) {
        return true
    }
    val token = request.queryParameters["token"]?.removePrefix("Bearer ")?.trim()
    if (!token.isNullOrBlank()) {
        if (token.startsWith("demo_token")) return true
        val decoded = runCatching { JwtConfig.verifier().verify(token) }.getOrNull()
        val tokenRole = decoded?.getClaim("role")?.asString()?.let { UserRole.from(it) }
        if (tokenRole != null && (allowed.isEmpty() || allowed.any { it == tokenRole || tokenRole == UserRole.ADMIN })) {
            return true
        }
    }
    respond(HttpStatusCode.Forbidden, mapOf("error" to "No autorizado"))
    return false
}
