package org.example.project.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.example.project.models.ActualizarEstadoProductoRequest
import org.example.project.models.EstadoProducto
import org.example.project.models.ProductoRequest
import org.example.project.services.InventarioService

fun Route.inventarioRoutes() {
    val inventarioService = InventarioService()

    route("/api/inventario") {

        // GET /api/inventario - Obtener todos los productos
        get {
            try {
                val search = call.request.queryParameters["search"] ?: call.request.queryParameters["q"]
                val codigo = call.request.queryParameters["codigo"]
                val categoria = call.request.queryParameters["categoria"]
                val estadoParam = call.request.queryParameters["estado"]?.uppercase()
                val estado = try { estadoParam?.let { EstadoProducto.valueOf(it) } } catch (_: Exception) { null }
                if (estadoParam != null && estado == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Estado inválido: $estadoParam"))
                    return@get
                }
                val criticos = call.request.queryParameters["criticos"]?.toBooleanStrictOrNull() ?: false
                val limit = call.request.queryParameters["limit"]?.toIntOrNull()?.coerceIn(1, 200) ?: 50
                val offset = call.request.queryParameters["offset"]?.toIntOrNull()?.coerceAtLeast(0) ?: 0

                val page = inventarioService.listarProductos(
                    filtros = InventarioService.Filtros(
                        search = search,
                        codigo = codigo,
                        categoria = categoria,
                        estado = estado,
                        criticos = criticos
                    ),
                    limit = limit,
                    offset = offset
                )
                call.respond(HttpStatusCode.OK, page)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }

        // Endpoint de prueba para debugging JSON específico
        post("/test-json") {
            try {
                println("=== TEST JSON DEBUGGING ===")
                println("Content-Type: ${call.request.contentType()}")

                // Mostrar headers de forma correcta
                call.request.headers.forEach { name, values ->
                    println("Header: $name = ${values.joinToString(", ")}")
                }

                // Intentar recibir como texto primero
                val jsonText = call.receiveText()
                println("JSON como texto: $jsonText")

                // Intentar parsear manualmente
                val json = kotlinx.serialization.json.Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                }
                val producto = json.decodeFromString<ProductoRequest>(jsonText)
                println("Producto parseado: $producto")

                call.respond(HttpStatusCode.OK, mapOf(
                    "success" to true,
                    "received" to jsonText,
                    "parsed" to producto
                ))
            } catch (e: Exception) {
                println("Error en test-json: ${e.message}")
                e.printStackTrace()
                call.respond(HttpStatusCode.BadRequest, mapOf(
                    "error" to e.message,
                    "type" to e::class.simpleName
                ))
            }
        }

        // GET /api/inventario/categorias - Lista de categorías únicas
        get("/categorias") {
            try {
                val incluirInactivos = call.request.queryParameters["incluirInactivos"]?.toBooleanStrictOrNull() ?: false
                val categorias = inventarioService.obtenerCategorias(onlyActive = !incluirInactivos)
                call.respond(HttpStatusCode.OK, categorias)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "Error obteniendo categorías")))
            }
        }

        // POST /api/inventario - Crear un nuevo producto
        post {
            try {
                println("=== DEBUG POST /api/inventario ===")

                // Verificar Content-Type
                val contentType = call.request.contentType()
                println("Content-Type recibido: $contentType")

                // Verificar headers
                call.request.headers.forEach { name, values ->
                    println("Header: $name = ${values.joinToString(", ")}")
                }

                println("Intentando deserializar JSON...")
                val request = call.receive<ProductoRequest>()
                println("✅ JSON deserializado exitosamente: $request")

                println("Creando producto...")
                val producto = inventarioService.crearProducto(request)
                println("✅ Producto creado: $producto")

                call.respond(HttpStatusCode.Created, producto)
            } catch (e: kotlinx.serialization.SerializationException) {
                println("❌ Error de serialización: ${e.message}")
                e.printStackTrace()
                call.respond(HttpStatusCode.BadRequest, mapOf(
                    "error" to "Error de serialización JSON: ${e.message}",
                    "type" to "SerializationException",
                    "help" to "Verifica que el JSON sea válido y que todos los campos requeridos estén presentes"
                ))
            } catch (e: io.ktor.server.plugins.BadRequestException) {
                println("❌ BadRequestException: ${e.message}")
                e.printStackTrace()
                call.respond(HttpStatusCode.BadRequest, mapOf(
                    "error" to "Request malformado: ${e.message}",
                    "type" to "BadRequestException",
                    "help" to "Verifica el Content-Type y formato del JSON"
                ))
            } catch (e: Exception) {
                println("❌ Error general: ${e.message}")
                e.printStackTrace()
                call.respond(HttpStatusCode.BadRequest, mapOf(
                    "error" to "Error procesando request: ${e.message}",
                    "type" to e::class.simpleName,
                    "help" to "Error inesperado al procesar la petición"
                ))
            }
        }

        // GET /api/inventario/{id} - Obtener un producto por ID
        get("/{id}") {
            try {
                val id = call.parameters["id"]?.toIntOrNull()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
                    return@get
                }

                val producto = inventarioService.obtenerProductoPorId(id)
                if (producto != null) {
                    call.respond(HttpStatusCode.OK, producto)
                } else {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Producto no encontrado"))
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }

        // PUT /api/inventario/{id} - Actualizar un producto
        put("/{id}") {
            try {
                val id = call.parameters["id"]?.toIntOrNull()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
                    return@put
                }

                val request = call.receive<ProductoRequest>()
                val producto = inventarioService.actualizarProducto(id, request)
                if (producto != null) {
                    call.respond(HttpStatusCode.OK, producto)
                } else {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Producto no encontrado"))
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
            }
        }

        // DELETE /api/inventario/{id} - Eliminar un producto
        delete("/{id}") {
            try {
                val id = call.parameters["id"]?.toIntOrNull()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
                    return@delete
                }

                val eliminado = inventarioService.eliminarProducto(id)
                if (eliminado) {
                    call.respond(HttpStatusCode.OK, mapOf("message" to "Producto desactivado exitosamente"))
                } else {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Producto no encontrado"))
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }

        // PATCH /api/inventario/{id}/stock - Actualizar solo el stock de un producto
        patch("/{id}/stock") {
            try {
                val id = call.parameters["id"]?.toIntOrNull()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
                    return@patch
                }

                val request = call.receive<Map<String, Int>>()
                val nuevaCantidad = request["cantidad"]
                if (nuevaCantidad == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Cantidad requerida"))
                    return@patch
                }

                val producto = inventarioService.actualizarStock(id, nuevaCantidad)
                if (producto != null) {
                    call.respond(HttpStatusCode.OK, producto)
                } else {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Producto no encontrado"))
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
            }
        }

        // PATCH /api/inventario/{id}/estado - Cambiar estado ACTIVO/INACTIVO
        patch("/{id}/estado") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
                return@patch
            }
            val body = try { call.receive<ActualizarEstadoProductoRequest>() } catch (_: Exception) { null }
            if (body == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Cuerpo inválido"))
                return@patch
            }

            val actualizado = inventarioService.cambiarEstado(id, body.estado)
            if (actualizado == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Producto no encontrado"))
            } else {
                call.respond(actualizado)
            }
        }

        // GET críticos
        get("/criticos") {
            try {
                val criticos = inventarioService.productosCriticos()
                call.respond(HttpStatusCode.OK, criticos)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }
    }
}
