package org.example.project.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import org.example.project.models.*
import org.example.project.services.VentasService
import org.example.project.services.InventarioService
import org.example.project.security.requireRole
import org.example.project.security.UserRole
import org.example.project.security.JwtConfig
import org.example.project.security.userRole

fun Route.ventasRoutes() {
    val ventasService = VentasService()

    route("/api/ventas") {

        // GET /api/ventas - Obtener todas las ventas
        get {
            if (!call.requireRole(UserRole.ADMIN, UserRole.VENTAS, UserRole.SUPERVISOR)) return@get
            try {
                val filtros = parseFiltros(call)
                val limit = call.request.queryParameters["limit"]?.toIntOrNull()?.coerceIn(1, 500) ?: 200
                val offset = call.request.queryParameters["offset"]?.toIntOrNull()?.coerceAtLeast(0) ?: 0

                val resumen = if (tieneFiltros(filtros)) {
                    val ventas = ventasService.listarVentas(filtros, limit, offset)
                    VentasListResponse(ventas = ventas, metricas = ventasService.obtenerMetricas())
                } else {
                    ventasService.obtenerVentasConMetricas()
                }
                call.respond(HttpStatusCode.OK, resumen)
            } catch (e: Exception) {
                println("Error al obtener ventas: ${e.message}")
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }

        // GET /api/ventas/metricas - Obtener métricas del dashboard
        get("/metricas") {
            if (!call.requireRole(UserRole.ADMIN, UserRole.VENTAS, UserRole.SUPERVISOR)) return@get
            try {
                val metricas = ventasService.obtenerMetricas()
                call.respond(HttpStatusCode.OK, metricas)
            } catch (e: Exception) {
                println("Error al obtener métricas: ${e.message}")
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }

        // GET /api/ventas/reporte - Reporte filtrado
        get("/reporte") {
            if (!call.requireRole(UserRole.ADMIN, UserRole.VENTAS, UserRole.SUPERVISOR)) return@get
            try {
                val filtros = parseFiltros(call)
                val limit = call.request.queryParameters["limit"]?.toIntOrNull()?.coerceIn(1, 500) ?: 200
                val offset = call.request.queryParameters["offset"]?.toIntOrNull()?.coerceAtLeast(0) ?: 0
                val ventas = ventasService.listarVentas(filtros, limit, offset)
                call.respond(HttpStatusCode.OK, ventas)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
            }
        }

        // GET /api/ventas/export/csv
        get("/export/csv") {
            if (!call.authorizeExport(UserRole.ADMIN, UserRole.VENTAS, UserRole.SUPERVISOR)) return@get
            try {
                val filtros = parseFiltros(call)
                val csv = ventasService.exportarVentasCsv(filtros)
                call.response.headers.append(
                    HttpHeaders.ContentDisposition,
                    "attachment; filename=\"ventas-${System.currentTimeMillis()}.csv\""
                )
                call.respondText(csv, ContentType.parse("text/csv"))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Error al exportar CSV")))
            }
        }

        // GET /api/ventas/export/pdf
        get("/export/pdf") {
            if (!call.authorizeExport(UserRole.ADMIN, UserRole.VENTAS, UserRole.SUPERVISOR)) return@get
            try {
                val filtros = parseFiltros(call)
                val pdf = ventasService.exportarVentasPdf(filtros)
                call.response.headers.append(
                    HttpHeaders.ContentDisposition,
                    "attachment; filename=\"ventas-${System.currentTimeMillis()}.pdf\""
                )
                call.respondBytes(pdf, ContentType.Application.Pdf)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Error al exportar PDF")))
            }
        }

        // POST /api/ventas - Crear nueva venta
        post {
            if (!call.requireRole(UserRole.ADMIN, UserRole.VENTAS)) return@post
            try {
                val request = call.receive<VentaRequest>()
                println("Creando nueva venta para: ${request.cliente}")

                val venta = ventasService.crearVenta(request)
                call.respond(HttpStatusCode.Created, venta)
            } catch (e: IllegalArgumentException) {
                println("Error de validación: ${e.message}")
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
            } catch (e: Exception) {
                println("Error al crear venta: ${e.message}")
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }

        // POST /api/ventas/{id}/devolucion - Devolución parcial
        post("/{id}/devolucion") {
            if (!call.requireRole(UserRole.ADMIN, UserRole.VENTAS)) return@post
            try {
                val ventaId = call.parameters["id"] ?: throw IllegalArgumentException("ID de venta requerido")
                val request = call.receive<DevolucionParcialRequest>()
                val venta = ventasService.registrarDevolucionParcial(ventaId, request)
                call.respond(HttpStatusCode.OK, venta)
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
            } catch (e: Exception) {
                println("Error al registrar devolución: ${e.message}")
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }

        // GET /api/ventas/{id} - Obtener venta específica
        get("/{id}") {
            if (!call.requireRole(UserRole.ADMIN, UserRole.VENTAS, UserRole.SUPERVISOR)) return@get
            try {
                val ventaId = call.parameters["id"] ?: throw IllegalArgumentException("ID de venta requerido")
                val venta = ventasService.obtenerVentaPorId(ventaId)

                if (venta != null) {
                    call.respond(HttpStatusCode.OK, venta)
                } else {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Venta no encontrada"))
                }
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
            } catch (e: Exception) {
                println("Error al obtener venta: ${e.message}")
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }

        // PATCH /api/ventas/{id}/estado - Actualizar estado de venta
        patch("/{id}/estado") {
            if (!call.requireRole(UserRole.ADMIN, UserRole.VENTAS, UserRole.SUPERVISOR)) return@patch
            try {
                val ventaId = call.parameters["id"] ?: throw IllegalArgumentException("ID de venta requerido")
                val request = call.receive<ActualizarEstadoRequest>()

                val venta = ventasService.actualizarEstadoVenta(ventaId, request.estado)
                call.respond(HttpStatusCode.OK, venta)
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
            } catch (e: Exception) {
                println("Error al actualizar estado: ${e.message}")
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }

        // GET /api/ventas/{id}/comprobante/pdf
        get("/{id}/comprobante/pdf") {
            if (!call.authorizeExport(UserRole.ADMIN, UserRole.VENTAS, UserRole.SUPERVISOR)) return@get
            try {
                val ventaId = call.parameters["id"] ?: throw IllegalArgumentException("ID de venta requerido")
                val pdf = ventasService.generarComprobantePdf(ventaId)
                call.response.headers.append(
                    HttpHeaders.ContentDisposition,
                    "attachment; filename=\"venta-$ventaId.pdf\""
                )
                call.respondBytes(pdf, ContentType.Application.Pdf)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Error al generar comprobante PDF")))
            }
        }

        // GET /api/ventas/{id}/comprobante/csv
        get("/{id}/comprobante/csv") {
            if (!call.authorizeExport(UserRole.ADMIN, UserRole.VENTAS, UserRole.SUPERVISOR)) return@get
            try {
                val ventaId = call.parameters["id"] ?: throw IllegalArgumentException("ID de venta requerido")
                val csv = ventasService.generarComprobanteCsv(ventaId)
                call.response.headers.append(
                    HttpHeaders.ContentDisposition,
                    "attachment; filename=\"venta-$ventaId.csv\""
                )
                call.respondText(csv, ContentType.parse("text/csv"))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Error al generar comprobante CSV")))
            }
        }

        // GET /api/ventas/productos/disponibles - Obtener productos disponibles para venta
        get("/productos/disponibles") {
            if (!call.requireRole(UserRole.ADMIN, UserRole.VENTAS, UserRole.INVENTARIO, UserRole.SUPERVISOR)) return@get
            try {
                val inventarioService = InventarioService()
                val productos = inventarioService
                    .listarProductos(
                        filtros = InventarioService.Filtros(estado = EstadoProducto.ACTIVO),
                        limit = 200
                    ).items
                    .filter { it.cantidad > 0 } // Solo productos con stock

                call.respond(HttpStatusCode.OK, productos)
            } catch (e: Exception) {
                println("Error al obtener productos disponibles: ${e.message}")
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }

        // Endpoint de prueba para debugging
        post("/test") {
            try {
                println("=== TEST VENTAS DEBUGGING ===")
                println("Content-Type: ${call.request.contentType()}")

                val jsonText = call.receiveText()
                println("JSON recibido: $jsonText")

                val json = kotlinx.serialization.json.Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                }
                val venta = json.decodeFromString<VentaRequest>(jsonText)
                println("Venta parseada: $venta")

                call.respond(HttpStatusCode.OK, mapOf(
                    "success" to true,
                    "received" to jsonText,
                    "parsed" to venta
                ))
            } catch (e: Exception) {
                println("Error en test: ${e.message}")
                call.respond(HttpStatusCode.BadRequest, mapOf(
                    "error" to e.message,
                    "type" to e::class.simpleName
                ))
            }
        }
    }
}

// Rutas públicas con validación de token en query (para descargas directas)
fun Route.ventasPublicRoutes() {
    val ventasService = VentasService()
    route("/api/ventas") {
        get("/export/csv") {
            if (!call.authorizeExport(UserRole.ADMIN, UserRole.VENTAS, UserRole.SUPERVISOR)) return@get
            try {
                val filtros = parseFiltros(call)
                val csv = ventasService.exportarVentasCsv(filtros)
                call.response.headers.append(
                    HttpHeaders.ContentDisposition,
                    "attachment; filename=\"ventas-${System.currentTimeMillis()}.csv\""
                )
                call.respondText(csv, ContentType.parse("text/csv"))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Error al exportar CSV")))
            }
        }
        get("/export/pdf") {
            if (!call.authorizeExport(UserRole.ADMIN, UserRole.VENTAS, UserRole.SUPERVISOR)) return@get
            try {
                val filtros = parseFiltros(call)
                val pdf = ventasService.exportarVentasPdf(filtros)
                call.response.headers.append(
                    HttpHeaders.ContentDisposition,
                    "attachment; filename=\"ventas-${System.currentTimeMillis()}.pdf\""
                )
                call.respondBytes(pdf, ContentType.Application.Pdf)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Error al exportar PDF")))
            }
        }
        get("/{id}/comprobante/pdf") {
            if (!call.authorizeExport(UserRole.ADMIN, UserRole.VENTAS, UserRole.SUPERVISOR)) return@get
            try {
                val ventaId = call.parameters["id"] ?: throw IllegalArgumentException("ID de venta requerido")
                val pdf = ventasService.generarComprobantePdf(ventaId)
                call.response.headers.append(
                    HttpHeaders.ContentDisposition,
                    "attachment; filename=\"venta-$ventaId.pdf\""
                )
                call.respondBytes(pdf, ContentType.Application.Pdf)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Error al generar comprobante PDF")))
            }
        }
        get("/{id}/comprobante/csv") {
            if (!call.authorizeExport(UserRole.ADMIN, UserRole.VENTAS, UserRole.SUPERVISOR)) return@get
            try {
                val ventaId = call.parameters["id"] ?: throw IllegalArgumentException("ID de venta requerido")
                val csv = ventasService.generarComprobanteCsv(ventaId)
                call.response.headers.append(
                    HttpHeaders.ContentDisposition,
                    "attachment; filename=\"venta-$ventaId.csv\""
                )
                call.respondText(csv, ContentType.parse("text/csv"))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Error al generar comprobante CSV")))
            }
        }
    }
}

private fun parseFiltros(call: ApplicationCall): VentasService.Filtros {
    val desde = call.request.queryParameters["desde"]?.let { parseDateOrDateTimeStart(it) }
    val hasta = call.request.queryParameters["hasta"]?.let { parseDateOrDateTimeEnd(it) }
    val vendedorId = call.request.queryParameters["vendedorId"]?.toIntOrNull()
    val cliente = call.request.queryParameters["cliente"]
    val estadoParam = call.request.queryParameters["estado"]?.uppercase()
    val estado = try { estadoParam?.let { EstadoVenta.valueOf(it) } } catch (_: Exception) { null }

    return VentasService.Filtros(
        desde = desde,
        hasta = hasta,
        vendedorId = vendedorId,
        cliente = cliente,
        estado = estado
    )
}

private fun tieneFiltros(filtros: VentasService.Filtros): Boolean =
    filtros.cliente != null || filtros.desde != null || filtros.hasta != null || filtros.vendedorId != null || filtros.estado != null

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

private suspend fun ApplicationCall.authorizeExport(vararg allowed: UserRole): Boolean {
    val role = userRole()
    if (role != null && (allowed.isEmpty() || allowed.any { it == role || role == UserRole.ADMIN })) {
        return true
    }
    val token = request.queryParameters["token"]?.removePrefix("Bearer ")?.trim()
    if (!token.isNullOrBlank()) {
        if (token.startsWith("demo_token")) return true // modo demo
        val decoded = runCatching { JwtConfig.verifier().verify(token) }.getOrNull()
        val tokenRole = decoded?.getClaim("role")?.asString()?.let { UserRole.from(it) }
        if (tokenRole != null && (allowed.isEmpty() || allowed.any { it == tokenRole || tokenRole == UserRole.ADMIN })) {
            return true
        }
    }
    respond(HttpStatusCode.Forbidden, mapOf("error" to "No autorizado"))
    return false
}
