package org.example.project.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.example.project.models.*
import org.example.project.services.AuthService
import io.ktor.server.auth.*
import org.example.project.security.userId

fun Route.authRoutes() {
    route("/api/auth") {

        // Registro de usuario
        post("/registro") {
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
                        message = "Error en el login: ${e.message}"
                    )
                )
            }
        }

        // Verificar token (opcional)
        get("/verify") {
            call.respond(HttpStatusCode.OK, mapOf("message" to "Token válido"))
        }

        authenticate("auth-jwt") {
            get("/me") {
                val userId = call.userId()
                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Token inválido"))
                    return@get
                }
                val user = AuthService.obtenerUsuario(userId)
                if (user == null) call.respond(HttpStatusCode.NotFound, mapOf("error" to "Usuario no encontrado"))
                else call.respond(user)
            }
        }
    }
}
