package org.example.project.models

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.kotlin.datetime.datetime
import kotlinx.datetime.*

/* -------------------- Tablas base -------------------- */
object Clientes : IntIdTable("clientes") {
    val nombre = varchar("nombre", 150)
    val rut = varchar("rut", 20).uniqueIndex()
    val activo = bool("activo").default(true)
    val fechaCreacion = datetime("fecha_creacion")
    val fechaActualizacion = datetime("fecha_actualizacion")
}

object Sedes : IntIdTable("sedes") {
    val clienteId = reference("cliente_id", Clientes)
    val nombre = varchar("nombre", 150)
    val direccion = varchar("direccion", 255).nullable()
    val comuna = varchar("comuna", 120).nullable()
    val lat = double("lat").nullable()
    val lon = double("lon").nullable()
    val fechaCreacion = datetime("fecha_creacion")
    val fechaActualizacion = datetime("fecha_actualizacion")
}

object Extintores : IntIdTable("extintores") {
    val codigoQr = varchar("codigo_qr", 100).uniqueIndex()
    val clienteId = reference("cliente_id", Clientes)
    val sedeId = reference("sede_id", Sedes).nullable()
    val tipo = varchar("tipo", 50)              // Polvo, CO2, etc.
    val agente = varchar("agente", 50)          // ABC, CO2, etc.
    val capacidad = varchar("capacidad", 30)    // 5kg, 10lb, etc.
    val ubicacion = varchar("ubicacion", 255).nullable()
    val estadoLogistico = enumerationByName("estado_logistico", 30, EstadoLogisticoExtintor::class)
        .default(EstadoLogisticoExtintor.DISPONIBLE)
    val fechaFabricacion = datetime("fecha_fabricacion").nullable()
    val fechaUltimaRecarga = datetime("fecha_ultima_recarga").nullable()
    val fechaProximoVencimiento = datetime("fecha_proximo_vencimiento").nullable()
    val estado = enumerationByName("estado", 20, EstadoExtintor::class).default(EstadoExtintor.VIGENTE)
    val fechaCreacion = datetime("fecha_creacion")
    val fechaActualizacion = datetime("fecha_actualizacion")
}

object OrdenesServicio : IntIdTable("ordenes_servicio") {
    val fechaProgramada = datetime("fecha_programada")
    val estado = enumerationByName("estado", 30, EstadoOrdenServicio::class)
    val tecnicoId = reference("tecnico_id", Usuarios).nullable()
    val clienteId = reference("cliente_id", Clientes)
    val sedeId = reference("sede_id", Sedes).nullable()
    val creadoPor = reference("creado_por", Usuarios).nullable()
    val fechaCreacion = datetime("fecha_creacion")
    val fechaActualizacion = datetime("fecha_actualizacion")
}

object OrdenServicioExtintores : IntIdTable("orden_servicio_extintores") {
    val ordenId = reference("orden_id", OrdenesServicio)
    val extintorId = reference("extintor_id", Extintores)
}

object Certificados : IntIdTable("certificados") {
    val extintorId = reference("extintor_id", Extintores)
    val numero = varchar("numero", 50)
    val fechaEmision = datetime("fecha_emision")
    val fechaProximoVencimiento = datetime("fecha_proximo_vencimiento").nullable()
    val pdfPath = varchar("pdf_path", 255).nullable()
}

/* -------------------- Enums -------------------- */

enum class EstadoOrdenServicio { PLANIFICADA, EN_PROGRESO, CERRADA, CANCELADA }
enum class EstadoExtintor { VIGENTE, POR_VENCER, VENCIDO }
enum class EstadoLogisticoExtintor { DISPONIBLE, TALLER, TERRENO, PRESTAMO, FUERA_SERVICIO }

/* -------------------- Entidades -------------------- */
class Cliente(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Cliente>(Clientes)
    var nombre by Clientes.nombre
    var rut by Clientes.rut
    var activo by Clientes.activo
    var fechaCreacion by Clientes.fechaCreacion
    var fechaActualizacion by Clientes.fechaActualizacion
}

class Sede(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Sede>(Sedes)
    var clienteId by Sedes.clienteId
    var nombre by Sedes.nombre
    var direccion by Sedes.direccion
    var comuna by Sedes.comuna
    var lat by Sedes.lat
    var lon by Sedes.lon
    var fechaCreacion by Sedes.fechaCreacion
    var fechaActualizacion by Sedes.fechaActualizacion
}

class Extintor(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Extintor>(Extintores)
    var codigoQr by Extintores.codigoQr
    var clienteId by Extintores.clienteId
    var sedeId by Extintores.sedeId
    var tipo by Extintores.tipo
    var agente by Extintores.agente
    var capacidad by Extintores.capacidad
    var ubicacion by Extintores.ubicacion
    var estadoLogistico by Extintores.estadoLogistico
    var fechaFabricacion by Extintores.fechaFabricacion
    var fechaUltimaRecarga by Extintores.fechaUltimaRecarga
    var fechaProximoVencimiento by Extintores.fechaProximoVencimiento
    var estado by Extintores.estado
    var fechaCreacion by Extintores.fechaCreacion
    var fechaActualizacion by Extintores.fechaActualizacion
}

class OrdenServicio(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<OrdenServicio>(OrdenesServicio)
    var fechaProgramada by OrdenesServicio.fechaProgramada
    var estado by OrdenesServicio.estado
    var tecnicoId by OrdenesServicio.tecnicoId
    var clienteId by OrdenesServicio.clienteId
    var sedeId by OrdenesServicio.sedeId
    var creadoPor by OrdenesServicio.creadoPor
    var fechaCreacion by OrdenesServicio.fechaCreacion
    var fechaActualizacion by OrdenesServicio.fechaActualizacion
}

class OrdenServicioExtintor(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<OrdenServicioExtintor>(OrdenServicioExtintores)
    var ordenId by OrdenServicioExtintores.ordenId
    var extintorId by OrdenServicioExtintores.extintorId
}

class Certificado(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Certificado>(Certificados)
    var extintorId by Certificados.extintorId
    var numero by Certificados.numero
    var fechaEmision by Certificados.fechaEmision
    var fechaProximoVencimiento by Certificados.fechaProximoVencimiento
    var pdfPath by Certificados.pdfPath
}

/* -------------------- DTOs -------------------- */
@Serializable
data class ClienteRequest(val nombre: String, val rut: String)
@Serializable
data class ClienteResponse(val id: Int, val nombre: String, val rut: String, val activo: Boolean)

@Serializable
data class SedeRequest(val clienteId: Int, val nombre: String, val direccion: String? = null, val comuna: String? = null, val lat: Double? = null, val lon: Double? = null)
@Serializable
data class SedeResponse(val id: Int, val clienteId: Int, val nombre: String, val direccion: String? = null, val comuna: String? = null)

@Serializable
data class ExtintorRequest(
    val codigoQr: String,
    val clienteId: Int,
    val sedeId: Int? = null,
    val tipo: String,
    val agente: String,
    val capacidad: String,
    val ubicacion: String? = null,
    val estadoLogistico: EstadoLogisticoExtintor? = null,
    val fechaProximoVencimiento: String? = null, // ISO
)

@Serializable
data class ExtintorResponse(
    val id: Int,
    val codigoQr: String,
    val clienteId: Int,
    val sedeId: Int? = null,
    val tipo: String,
    val agente: String,
    val capacidad: String,
    val ubicacion: String? = null,
    val estadoLogistico: EstadoLogisticoExtintor,
    val fechaProximoVencimiento: String?,
    val diasParaVencer: Long?,
    val color: String,
    val estado: EstadoExtintor
)

@Serializable
data class CrearOrdenServicioRequest(
    val fechaProgramada: String, // ISO datetime
    val tecnicoId: Int? = null,
    val clienteId: Int,
    val sedeId: Int? = null,
    val extintores: List<Int>,
    val creadoPor: Int? = null
)

@Serializable
data class OrdenServicioResponse(
    val id: Int,
    val fechaProgramada: String,
    val estado: EstadoOrdenServicio,
    val tecnicoId: Int?,
    val clienteId: Int,
    val sedeId: Int?,
    val extintores: List<Int>
)

@Serializable
data class ExtintorUpdateRequest(
    val sedeId: Int? = null,
    val tipo: String? = null,
    val agente: String? = null,
    val capacidad: String? = null,
    val ubicacion: String? = null,
    val estadoLogistico: EstadoLogisticoExtintor? = null,
    val fechaProximoVencimiento: String? = null
)

@Serializable
data class CertificadoResponse(
    val id: Int,
    val extintorId: Int,
    val numero: String,
    val fechaEmision: String,
    val fechaProximoVencimiento: String?,
    val pdfPath: String?
)

/* -------------------- Utilidades dominio -------------------- */
private const val LIMITE_ROJO = 30L
private const val LIMITE_AMARILLO = 60L

fun calcularColor(fecha: LocalDateTime?): Pair<String, Long?> {
    if (fecha == null) return "gris" to null
    val hoy = Clock.System.now().toLocalDateTime(TimeZone.UTC)
    val diff = fecha.toInstant(TimeZone.UTC).epochSeconds - hoy.toInstant(TimeZone.UTC).epochSeconds
    val dias = diff / 86400
    val color = when {
        dias <= LIMITE_ROJO -> "rojo"
        dias <= LIMITE_AMARILLO -> "amarillo"
        else -> "verde"
    }
    return color to dias
}
