package org.example.project.services

import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.example.project.models.*
import org.example.project.security.PasswordUtils
import org.example.project.security.UserRole
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.neq

object UsuariosService {

    fun listar(): List<UsuarioResponse> = transaction {
        Usuarios.selectAll()
            .orderBy(Usuarios.id)
            .map { it.toResponse() }
    }

    fun obtener(id: Int): UsuarioResponse? = transaction {
        Usuarios.selectAll()
            .where { Usuarios.id eq id }
            .singleOrNull()
            ?.toResponse()
    }

    fun crear(req: CrearUsuarioRequest): UsuarioResponse {
        val rol = UserRole.from(req.rol)?.name ?: UserRole.USER.name
        val ahora = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

        return transaction {
            val existe = Usuarios.selectAll().where { Usuarios.email eq req.email.lowercase() }.count() > 0
            if (existe) throw IllegalArgumentException("El email ya está registrado")

            if (req.password.length < 6) throw IllegalArgumentException("La contraseña debe tener al menos 6 caracteres")

            val salt = PasswordUtils.generateSalt()
            val hashed = PasswordUtils.hashPassword(req.password, salt)

            val id = Usuarios.insertAndGetId {
                it[email] = req.email.lowercase()
                it[password] = hashed
                it[nombre] = req.nombre
                it[apellido] = req.apellido
                it[Usuarios.rol] = rol
                it[activo] = req.activo
                it[fechaCreacion] = ahora
                it[intentosFallidos] = 0
                it[bloqueadoHasta] = null
            }

            UsuarioResponse(
                id = id.value,
                email = req.email.lowercase(),
                nombre = req.nombre,
                apellido = req.apellido,
                rol = rol,
                activo = req.activo,
                fechaCreacion = ahora.toString(),
                intentosFallidos = 0,
                bloqueadoHasta = null
            )
        }
    }

    fun actualizar(id: Int, req: ActualizarUsuarioRequest): UsuarioResponse? {
        return transaction {
            val existe = Usuarios.selectAll().where { Usuarios.id eq id }.singleOrNull() ?: return@transaction null

            // Validaciones
            req.email?.let { nuevoEmail ->
                val existeEmail = Usuarios.selectAll()
                    .where { (Usuarios.email eq nuevoEmail.lowercase()) and (Usuarios.id neq id) }
                    .count() > 0
                if (existeEmail) throw IllegalArgumentException("El email ya está registrado")
            }

            if (req.password != null && req.password.length < 6) {
                throw IllegalArgumentException("La contraseña debe tener al menos 6 caracteres")
            }

            val nuevoRol = req.rol?.let { UserRole.from(it)?.name }

            Usuarios.update({ Usuarios.id eq id }) { row ->
                req.email?.let { row[Usuarios.email] = it.lowercase() }
                req.nombre?.let { row[Usuarios.nombre] = it }
                req.apellido?.let { row[Usuarios.apellido] = it }
                nuevoRol?.let { row[Usuarios.rol] = it }
                req.activo?.let { row[Usuarios.activo] = it }
                if (req.password != null) {
                    val salt = PasswordUtils.generateSalt()
                    row[Usuarios.password] = PasswordUtils.hashPassword(req.password, salt)
                    row[intentosFallidos] = 0
                    row[bloqueadoHasta] = null
                }
            }

            val actualizado = Usuarios.selectAll().where { Usuarios.id eq id }.single()
            actualizado.toResponse()
        }
    }

    fun cambiarEstado(id: Int, activo: Boolean): UsuarioResponse? = transaction {
        val existe = Usuarios.selectAll().where { Usuarios.id eq id }.singleOrNull() ?: return@transaction null
        Usuarios.update({ Usuarios.id eq id }) {
            it[Usuarios.activo] = activo
            it[intentosFallidos] = 0
            it[bloqueadoHasta] = null
        }
        existe.toResponse().copy(activo = activo, intentosFallidos = 0, bloqueadoHasta = null)
    }

    fun actualizarPropio(id: Int, req: ActualizarUsuarioRequest): UsuarioResponse? {
        // Ignora cambios de rol/estado para evitar elevación de privilegios
        val sanitized = req.copy(rol = null, activo = null)
        return actualizar(id, sanitized)
    }

    private fun ResultRow.toResponse(): UsuarioResponse = UsuarioResponse(
        id = this[Usuarios.id].value,
        email = this[Usuarios.email],
        nombre = this[Usuarios.nombre],
        apellido = this[Usuarios.apellido],
        rol = UserRole.from(this[Usuarios.rol])?.name ?: this[Usuarios.rol].uppercase(),
        activo = this[Usuarios.activo],
        fechaCreacion = this[Usuarios.fechaCreacion].toString(),
        intentosFallidos = this[Usuarios.intentosFallidos],
        bloqueadoHasta = this[Usuarios.bloqueadoHasta]?.toString()
    )
}
