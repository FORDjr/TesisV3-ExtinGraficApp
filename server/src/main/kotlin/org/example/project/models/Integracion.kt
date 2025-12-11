package org.example.project.models

import kotlinx.serialization.Serializable
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

enum class IntegrationScope { INVENTARIO_READ, MOVIMIENTOS_READ, MOVIMIENTOS_WRITE, REPORTES_READ }

object Integraciones : IntIdTable("integraciones_api") {
    val nombre = varchar("nombre", 120)
    val token = varchar("token", 160).uniqueIndex()
    val scopes = text("scopes") // CSV en mayúsculas
    val activo = bool("activo").default(true)
    val creadoEn = datetime("creado_en")
    val creadoPor = varchar("creado_por", 120).nullable()
    val ultimoUso = datetime("ultimo_uso").nullable()
    val hits = integer("hits").default(0)
}

class Integracion(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Integracion>(Integraciones)

    var nombre by Integraciones.nombre
    var token by Integraciones.token
    var scopes by Integraciones.scopes
    var activo by Integraciones.activo
    var creadoEn by Integraciones.creadoEn
    var creadoPor by Integraciones.creadoPor
    var ultimoUso by Integraciones.ultimoUso
    var hits by Integraciones.hits

    fun scopeSet(): Set<IntegrationScope> = scopes.split(',')
        .mapNotNull { raw -> IntegrationScope.entries.firstOrNull { it.name == raw.trim().uppercase() } }
        .toSet()

    fun touch() {
        ultimoUso = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        hits += 1
    }
}

@Serializable
data class IntegrationStatusResponse(
    val nombre: String,
    val scopes: Set<IntegrationScope>,
    val activo: Boolean,
    val creadoEn: String,
    val ultimoUso: String?,
    val hits: Int
)

@Serializable
data class IntegrationInventarioResumen(
    val totalProductos: Int,
    val activos: Int,
    val inactivos: Int,
    val stockCritico: Int,
    val pendientesAprobacion: Int
)
