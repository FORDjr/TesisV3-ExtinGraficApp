package org.example.project.models

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.kotlin.datetime.datetime
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

// Tabla de usuarios en la base de datos
object Usuarios : IntIdTable("usuarios") {
    val email = varchar("email", 255).uniqueIndex()
    val password = varchar("password", 255) // Hash de la contraseña
    val nombre = varchar("nombre", 100)
    val apellido = varchar("apellido", 100)
    val rol = varchar("rol", 50).default("user") // user, admin, etc.
    val activo = bool("activo").default(true)
    val fechaCreacion = datetime("fecha_creacion")
    val fechaUltimoAcceso = datetime("fecha_ultimo_acceso").nullable()
}

// DTOs para la API
@Serializable
data class UsuarioRegistro(
    val email: String,
    val password: String,
    val nombre: String,
    val apellido: String = "" // Hacer apellido opcional con valor por defecto
)

@Serializable
data class UsuarioLogin(
    val email: String,
    val password: String
)

@Serializable
data class UsuarioResponse(
    val id: Int,
    val email: String,
    val nombre: String,
    val apellido: String,
    val rol: String,
    val activo: Boolean,
    val fechaCreacion: String
)

@Serializable
data class LoginResponse(
    val success: Boolean,
    val message: String,
    val usuario: UsuarioResponse? = null,
    val token: String? = null
)

@Serializable
data class RegistroResponse(
    val success: Boolean,
    val message: String,
    val usuario: UsuarioResponse? = null
)
