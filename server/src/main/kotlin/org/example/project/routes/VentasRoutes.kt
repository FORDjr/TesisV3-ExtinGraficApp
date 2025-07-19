package org.example.project.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.example.project.models.*
import org.example.project.services.VentasService

fun Route.ventasRoutes() {
    val ventasService = VentasService()

    route("/api/ventas") {

        // GET /api/ventas - Obtener todas las ventas
        get {
            try {
                val ventas = ventasService.obtenerTodasLasVentas()
                call.respond(HttpStatusCode.OK, ventas)
            } catch (e: Exception) {
                println("Error al obtener ventas: ${e.message}")
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }

        // GET /api/ventas/metricas - Obtener métricas del dashboard
        get("/metricas") {
            try {
                val metricas = ventasService.obtenerMetricas()
                call.respond(HttpStatusCode.OK, metricas)
            } catch (e: Exception) {
                println("Error al obtener métricas: ${e.message}")
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }

        // POST /api/ventas - Crear nueva venta
        post {
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

        // GET /api/ventas/{id} - Obtener venta específica
        get("/{id}") {
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

        // GET /api/ventas/productos/disponibles - Obtener productos disponibles para venta
        get("/productos/disponibles") {
            try {
                // Reutilizar el servicio de inventario para obtener productos con stock
                val inventarioService = org.example.project.services.InventarioService()
                val productos = inventarioService.obtenerTodosLosProductos()
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
