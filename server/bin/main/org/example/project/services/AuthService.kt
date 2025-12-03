package org.example.project.services

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.example.project.models.*
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.spec.PBEKeySpec
import javax.crypto.SecretKeyFactory

object AuthService {

    // Hash de contraseña usando PBKDF2
    private fun hashPassword(password: String, salt: ByteArray): String {
        val spec = PBEKeySpec(password.toCharArray(), salt, 100000, 256)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val hash = factory.generateSecret(spec).encoded
        return "${salt.joinToString("") { "%02x".format(it) }}:${hash.joinToString("") { "%02x".format(it) }}"
    }

    // Verificar contraseña
    private fun verifyPassword(password: String, hashedPassword: String): Boolean {
        val parts = hashedPassword.split(":")
        if (parts.size != 2) return false

        val salt = parts[0].chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        val hash = parts[1]

        val newHash = hashPassword(password, salt)
        return newHash == hashedPassword
    }

    // Generar salt aleatorio
    private fun generateSalt(): ByteArray {
        val salt = ByteArray(32)
        SecureRandom().nextBytes(salt)
        return salt
    }

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
                val salt = generateSalt()
                val hashedPassword = hashPassword(registro.password, salt)
                val ahora = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

                // Insertar usuario
                val userId = Usuarios.insertAndGetId {
                    it[email] = registro.email.lowercase()
                    it[password] = hashedPassword
                    it[nombre] = registro.nombre
                    it[apellido] = registro.apellido
                    it[rol] = "user"
                    it[activo] = true
                    it[fechaCreacion] = ahora
                }

                val usuario = UsuarioResponse(
                    id = userId.value,
                    email = registro.email.lowercase(),
                    nombre = registro.nombre,
                    apellido = registro.apellido,
                    rol = "user",
                    activo = true,
                    fechaCreacion = ahora.toString()
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

                if (!usuario[Usuarios.activo]) {
                    return@transaction LoginResponse(
                        success = false,
                        message = "Usuario inactivo"
                    )
                }

                val hashedPassword = usuario[Usuarios.password]
                if (!verifyPassword(login.password, hashedPassword)) {
                    return@transaction LoginResponse(
                        success = false,
                        message = "Email o contraseña incorrectos"
                    )
                }

                // Actualizar fecha de último acceso
                val ahora = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                Usuarios.update({ Usuarios.id eq usuario[Usuarios.id] }) {
                    it[fechaUltimoAcceso] = ahora
                }

                val usuarioResponse = UsuarioResponse(
                    id = usuario[Usuarios.id].value,
                    email = usuario[Usuarios.email],
                    nombre = usuario[Usuarios.nombre],
                    apellido = usuario[Usuarios.apellido],
                    rol = usuario[Usuarios.rol],
                    activo = usuario[Usuarios.activo],
                    fechaCreacion = usuario[Usuarios.fechaCreacion].toString()
                )

                // Generar token simple (en producción usar JWT)
                val token = generateSimpleToken(usuario[Usuarios.id].value)

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

    // Generar token simple (en producción usar JWT)
    private fun generateSimpleToken(userId: Int): String {
        val timestamp = System.currentTimeMillis()
        val data = "$userId:$timestamp"
        val hash = MessageDigest.getInstance("SHA-256").digest(data.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
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
                    rol = it[Usuarios.rol],
                    activo = it[Usuarios.activo],
                    fechaCreacion = it[Usuarios.fechaCreacion].toString()
                )
            }
        }
    }
}
