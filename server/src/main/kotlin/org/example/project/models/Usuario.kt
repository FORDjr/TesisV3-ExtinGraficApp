package org.example.project.models

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
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
    val intentosFallidos = integer("intentos_fallidos").default(0)
    val bloqueadoHasta = datetime("bloqueado_hasta").nullable()
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
    val fechaCreacion: String,
    val intentosFallidos: Int = 0,
    val bloqueadoHasta: String? = null
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

class Usuario(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Usuario>(Usuarios)
    var email by Usuarios.email
    var password by Usuarios.password
    var nombre by Usuarios.nombre
    var apellido by Usuarios.apellido
    var rol by Usuarios.rol
    var activo by Usuarios.activo
    var fechaCreacion by Usuarios.fechaCreacion
    var fechaUltimoAcceso by Usuarios.fechaUltimoAcceso
    var intentosFallidos by Usuarios.intentosFallidos
    var bloqueadoHasta by Usuarios.bloqueadoHasta
}
