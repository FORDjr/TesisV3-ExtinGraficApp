package org.example.project.security

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.auth.principal
import io.ktor.server.response.*

enum class UserRole {
    ADMIN, INVENTARIO, VENTAS, SUPERVISOR, USER;

    companion object {
        fun from(raw: String?): UserRole? = raw?.let { value ->
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
        }
    }
}

fun ApplicationCall.userId(): Int? = principal<JWTPrincipal>()
    ?.payload
    ?.getClaim("userId")
    ?.asInt()

fun ApplicationCall.userRole(): UserRole? = principal<JWTPrincipal>()
    ?.payload
    ?.getClaim("role")
    ?.asString()
    ?.let { UserRole.from(it) }

suspend fun ApplicationCall.requireRole(vararg allowed: UserRole): Boolean {
    val role = userRole()
    if (role == null || (allowed.isNotEmpty() && allowed.none { it == role || role == UserRole.ADMIN })) {
        respond(HttpStatusCode.Forbidden, mapOf("error" to "No autorizado"))
        return false
    }
    return true
}
