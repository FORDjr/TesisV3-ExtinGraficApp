package org.example.project.models

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.kotlin.datetime.datetime
import kotlinx.datetime.LocalDateTime

object ServiceRegistros : IntIdTable("service_registros") {
    val extintorId = reference("extintor_id", Extintores)
    val ordenId = reference("orden_id", OrdenesServicio).nullable()
    val tecnicoId = reference("tecnico_id", Usuarios).nullable()
    val pesoInicial = varchar("peso_inicial", 20).nullable()
    val observaciones = text("observaciones").nullable()
    val fechaRegistro = datetime("fecha_registro")
    val fechaProximoVencimiento = datetime("fecha_proximo_vencimiento").nullable()
    val numeroCertificado = varchar("numero_certificado", 60).nullable()
}

object ServiceRegistroProductos : IntIdTable("service_registro_productos") {
    val serviceId = reference("service_id", ServiceRegistros)
    val productoId = reference("producto_id", Productos)
    val cantidad = integer("cantidad")
}

object Alertas : IntIdTable("alertas") {
    val extintorId = reference("extintor_id", Extintores).nullable() // ahora nullable para alertas de stock
    val productoId = reference("producto_id", Productos).nullable() // NUEVO para stock
    val tipo = varchar("tipo", 50) // VENCIMIENTO, STOCK
    val fechaGenerada = datetime("fecha_generada")
    val enviada = bool("enviada").default(false)
    val fechaEnvio = datetime("fecha_envio").nullable()
    val reintentos = integer("reintentos").default(0)
}

class ServiceRegistro(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<ServiceRegistro>(ServiceRegistros)
    var extintorId by ServiceRegistros.extintorId
    var ordenId by ServiceRegistros.ordenId
    var tecnicoId by ServiceRegistros.tecnicoId
    var pesoInicial by ServiceRegistros.pesoInicial
    var observaciones by ServiceRegistros.observaciones
    var fechaRegistro by ServiceRegistros.fechaRegistro
    var fechaProximoVencimiento by ServiceRegistros.fechaProximoVencimiento
    var numeroCertificado by ServiceRegistros.numeroCertificado
}

class ServiceRegistroProducto(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<ServiceRegistroProducto>(ServiceRegistroProductos)
    var serviceId by ServiceRegistroProductos.serviceId
    var productoId by ServiceRegistroProductos.productoId
    var cantidad by ServiceRegistroProductos.cantidad
}

class Alerta(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Alerta>(Alertas)
    var extintorId by Alertas.extintorId
    var productoId by Alertas.productoId
    var tipo by Alertas.tipo
    var fechaGenerada by Alertas.fechaGenerada
    var enviada by Alertas.enviada
    var fechaEnvio by Alertas.fechaEnvio
    var reintentos by Alertas.reintentos
}

@Serializable
data class CrearServiceRegistroRequest(
    val extintorId: Int,
    val ordenId: Int? = null,
    val tecnicoId: Int? = null,
    val pesoInicial: String? = null,
    val observaciones: String? = null,
    val productos: List<ItemUsoProducto> = emptyList()
)

@Serializable
data class ItemUsoProducto(val productoId: Int, val cantidad: Int)

@Serializable
data class ServiceRegistroResponse(
    val id: Int,
    val extintorId: Int,
    val ordenId: Int?,
    val tecnicoId: Int?,
    val fechaRegistro: String,
    val fechaProximoVencimiento: String?,
    val numeroCertificado: String?,
    val productos: List<ItemUsoProducto>
)

@Serializable
data class AlertaResponse(
    val id: Int,
    val extintorId: Int? = null,
    val productoId: Int? = null,
    val tipo: String,
    val fechaGenerada: String,
    val enviada: Boolean,
    val reintentos: Int
)
