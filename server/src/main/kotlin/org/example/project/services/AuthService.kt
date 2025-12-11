package org.example.project.services

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.example.project.models.*
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.plus
import kotlinx.datetime.DateTimeUnit
import org.example.project.MAX_FAILED_ATTEMPTS
import org.example.project.LOCKOUT_MINUTES
import org.example.project.security.JwtConfig
import org.example.project.security.PasswordUtils
import org.example.project.security.UserRole

object AuthService {

    // Registrar nuevo usuario
    fun registrarUsuario(registro: UsuarioRegistro): RegistroResponse {
        return transaction {
            try {
                // Verificar si el email ya existe
                val existeUsuario = Usuarios.selectAll().where { Usuarios.email eq registro.email }.count() > 0
                if (existeUsuario) {
                    return@transaction RegistroResponse(
                        success = false,
                        message = "El email ya está registrado"
                    )
                }

                // Validar datos
                if (registro.email.isBlank() || registro.password.isBlank() ||
                    registro.nombre.isBlank() || registro.apellido.isBlank()) {
                    return@transaction RegistroResponse(
                        success = false,
                        message = "Todos los campos son requeridos"
                    )
                }

                if (registro.password.length < 6) {
                    return@transaction RegistroResponse(
                        success = false,
                        message = "La contraseña debe tener al menos 6 caracteres"
                    )
                }

                // Hash de la contraseña
                val salt = PasswordUtils.generateSalt()
                val hashedPassword = PasswordUtils.hashPassword(registro.password, salt)
                val ahora = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                val rol = UserRole.USER.name

                // Insertar usuario
                val userId = Usuarios.insertAndGetId {
                    it[email] = registro.email.lowercase()
                    it[password] = hashedPassword
                    it[nombre] = registro.nombre
                    it[apellido] = registro.apellido
                    it[Usuarios.rol] = rol
                    it[activo] = true
                    it[fechaCreacion] = ahora
                    it[intentosFallidos] = 0
                    it[bloqueadoHasta] = null
                }

                val usuario = UsuarioResponse(
                    id = userId.value,
                    email = registro.email.lowercase(),
                    nombre = registro.nombre,
                    apellido = registro.apellido,
                    rol = rol,
                    activo = true,
                    fechaCreacion = ahora.toString(),
                    intentosFallidos = 0,
                    bloqueadoHasta = null
                )

                RegistroResponse(
                    success = true,
                    message = "Usuario registrado exitosamente",
                    usuario = usuario
                )

            } catch (e: Exception) {
                RegistroResponse(
                    success = false,
                    message = "Error al registrar usuario: ${e.message}"
                )
            }
        }
    }

    // Login de usuario
    fun loginUsuario(login: UsuarioLogin): LoginResponse {
        return transaction {
            try {
                val usuario = Usuarios.selectAll()
                    .where { Usuarios.email eq login.email.lowercase() }
                    .singleOrNull()

                if (usuario == null) {
                    return@transaction LoginResponse(
                        success = false,
                        message = "Email o contraseña incorrectos"
                    )
                }

                val ahora = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

                if (!usuario[Usuarios.activo]) {
                    return@transaction LoginResponse(
                        success = false,
                        message = "Usuario inactivo"
                    )
                }

                val bloqueadoHasta = usuario[Usuarios.bloqueadoHasta]
                if (bloqueadoHasta != null && bloqueadoHasta > ahora) {
                    return@transaction LoginResponse(
                        success = false,
                        message = "Cuenta bloqueada hasta $bloqueadoHasta por intentos fallidos"
                    )
                }

                val hashedPassword = usuario[Usuarios.password]
                val passwordValida = PasswordUtils.verifyPassword(login.password, hashedPassword)
                if (!passwordValida) {
                    val intentos = usuario[Usuarios.intentosFallidos] + 1
                    val lockUntil = if (intentos >= MAX_FAILED_ATTEMPTS) {
                        Clock.System.now()
                            .plus(LOCKOUT_MINUTES, DateTimeUnit.MINUTE, TimeZone.currentSystemDefault())
                            .toLocalDateTime(TimeZone.currentSystemDefault())
                    } else null
                    Usuarios.update({ Usuarios.id eq usuario[Usuarios.id] }) {
                        it[Usuarios.intentosFallidos] = intentos
                        it[Usuarios.bloqueadoHasta] = lockUntil
                    }
                    val mensaje = if (lockUntil != null) {
                        "Cuenta bloqueada por $LOCKOUT_MINUTES minutos por intentos fallidos"
                    } else {
                        "Email o contraseña incorrectos"
                    }
                    return@transaction LoginResponse(
                        success = false,
                        message = mensaje
                    )
                }

                // Actualizar fecha de último acceso
                Usuarios.update({ Usuarios.id eq usuario[Usuarios.id] }) {
                    it[Usuarios.fechaUltimoAcceso] = ahora
                    it[Usuarios.intentosFallidos] = 0
                    it[Usuarios.bloqueadoHasta] = null
                }

                val rol = UserRole.from(usuario[Usuarios.rol])?.name ?: UserRole.USER.name
                val usuarioResponse = UsuarioResponse(
                    id = usuario[Usuarios.id].value,
                    email = usuario[Usuarios.email],
                    nombre = usuario[Usuarios.nombre],
                    apellido = usuario[Usuarios.apellido],
                    rol = rol,
                    activo = usuario[Usuarios.activo],
                    fechaCreacion = usuario[Usuarios.fechaCreacion].toString(),
                    intentosFallidos = usuario[Usuarios.intentosFallidos],
                    bloqueadoHasta = usuario[Usuarios.bloqueadoHasta]?.toString()
                )

                val token = JwtConfig.generateToken(
                    userId = usuario[Usuarios.id].value,
                    email = usuario[Usuarios.email],
                    role = rol
                )

                LoginResponse(
                    success = true,
                    message = "Login exitoso",
                    usuario = usuarioResponse,
                    token = token
                )

            } catch (e: Exception) {
                LoginResponse(
                    success = false,
                    message = "Error en el login: ${e.message}"
                )
            }
        }
    }

    // Obtener usuario por ID
    fun obtenerUsuario(userId: Int): UsuarioResponse? {
        return transaction {
            val usuario = Usuarios.selectAll()
                .where { Usuarios.id eq userId }
                .singleOrNull()

            usuario?.let {
                UsuarioResponse(
                    id = it[Usuarios.id].value,
                    email = it[Usuarios.email],
                    nombre = it[Usuarios.nombre],
                    apellido = it[Usuarios.apellido],
                    rol = UserRole.from(it[Usuarios.rol])?.name ?: it[Usuarios.rol].uppercase(),
                    activo = it[Usuarios.activo],
                    fechaCreacion = it[Usuarios.fechaCreacion].toString(),
                    intentosFallidos = it[Usuarios.intentosFallidos],
                    bloqueadoHasta = it[Usuarios.bloqueadoHasta]?.toString()
                )
            }
        }
    }
}
