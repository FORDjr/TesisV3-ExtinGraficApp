package org.example.project.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.example.project.models.ActualizarUsuarioRequest
import org.example.project.models.CambiarEstadoUsuarioRequest
import org.example.project.models.CrearUsuarioRequest
import org.example.project.security.UserRole
import org.example.project.security.requireRole
import org.example.project.security.userId
import org.example.project.services.UsuariosService

fun Route.usuariosRoutes() {
    route("/api/usuarios") {
        get("/me") {
            val uid = call.userId()
            if (uid == null) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Token inválido"))
                return@get
            }
            val usuario = UsuariosService.obtener(uid)
            if (usuario == null) call.respond(HttpStatusCode.NotFound, mapOf("error" to "Usuario no encontrado"))
            else call.respond(usuario)
        }

        put("/me") {
            val uid = call.userId()
            if (uid == null) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Token inválido"))
                return@put
            }
            val body = try { call.receive<ActualizarUsuarioRequest>() } catch (_: Exception) { null }
            if (body == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Cuerpo inválido"))
                return@put
            }
            try {
                val actualizado = UsuariosService.actualizarPropio(uid, body)
                if (actualizado == null) call.respond(HttpStatusCode.NotFound, mapOf("error" to "Usuario no encontrado"))
                else call.respond(actualizado)
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "Error al actualizar perfil")))
            }
        }

        get {
            if (!call.requireRole(UserRole.ADMIN)) return@get
            call.respond(UsuariosService.listar())
        }

        get("/{id}") {
            if (!call.requireRole(UserRole.ADMIN)) return@get
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
                return@get
            }
            val usuario = UsuariosService.obtener(id)
            if (usuario == null) call.respond(HttpStatusCode.NotFound, mapOf("error" to "Usuario no encontrado"))
            else call.respond(usuario)
        }

        post {
            if (!call.requireRole(UserRole.ADMIN)) return@post
            try {
                val body = call.receive<CrearUsuarioRequest>()
                val creado = UsuariosService.crear(body)
                call.respond(HttpStatusCode.Created, creado)
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "Error al crear usuario")))
            }
        }

        put("/{id}") {
            if (!call.requireRole(UserRole.ADMIN)) return@put
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
                return@put
            }
            val body = try { call.receive<ActualizarUsuarioRequest>() } catch (_: Exception) { null }
            if (body == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Cuerpo inválido"))
                return@put
            }
            try {
                val actualizado = UsuariosService.actualizar(id, body)
                if (actualizado == null) call.respond(HttpStatusCode.NotFound, mapOf("error" to "Usuario no encontrado"))
                else call.respond(actualizado)
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "Error al actualizar usuario")))
            }
        }

        patch("/{id}/estado") {
            if (!call.requireRole(UserRole.ADMIN)) return@patch
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
                return@patch
            }
            val body = try { call.receive<CambiarEstadoUsuarioRequest>() } catch (_: Exception) { null }
            if (body == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Cuerpo inválido"))
                return@patch
            }
            val actualizado = UsuariosService.cambiarEstado(id, body.activo)
            if (actualizado == null) call.respond(HttpStatusCode.NotFound, mapOf("error" to "Usuario no encontrado"))
            else call.respond(actualizado)
        }
    }
}
