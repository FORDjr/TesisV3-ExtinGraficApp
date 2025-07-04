package org.example.project.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.example.project.models.*
import org.example.project.services.AuthService

fun Route.authRoutes() {
    route("/auth") {

        // Registro de usuario
        post("/register") {
            try {
                val registro = call.receive<UsuarioRegistro>()
                val response = AuthService.registrarUsuario(registro)

                if (response.success) {
                    call.respond(HttpStatusCode.Created, response)
                } else {
                    call.respond(HttpStatusCode.BadRequest, response)
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    RegistroResponse(
                        success = false,
                        message = "Error en el formato de datos: ${e.message}"
                    )
                )
            }
        }

        // Login de usuario
        post("/login") {
            try {
                val login = call.receive<UsuarioLogin>()
                val response = AuthService.loginUsuario(login)

                if (response.success) {
                    call.respond(HttpStatusCode.OK, response)
                } else {
                    call.respond(HttpStatusCode.Unauthorized, response)
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    LoginResponse(
                        success = false,
                        message = "Error en el formato de datos: ${e.message}"
                    )
                )
            }
        }

        // Obtener perfil de usuario (requiere autenticación)
        get("/profile/{id}") {
            try {
                val userId = call.parameters["id"]?.toIntOrNull()
                if (userId == null) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "ID de usuario inválido")
                    )
                    return@get
                }

                val usuario = AuthService.obtenerUsuario(userId)
                if (usuario != null) {
                    call.respond(HttpStatusCode.OK, usuario)
                } else {
                    call.respond(
                        HttpStatusCode.NotFound,
                        mapOf("error" to "Usuario no encontrado")
                    )
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to "Error interno del servidor: ${e.message}")
                )
            }
        }

        // Ruta de prueba para verificar que las rutas están funcionando
        get("/test") {
            call.respond(HttpStatusCode.OK, mapOf("message" to "API de autenticación funcionando"))
        }
    }
}
