package org.example.project.models

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.kotlin.datetime.datetime
import kotlinx.datetime.LocalDateTime

// Tabla de la base de datos
object Productos : IntIdTable() {
    val nombre = varchar("nombre", 100)
    val descripcion = text("descripcion").nullable()
    val precio = decimal("precio", 10, 2)
    val cantidad = integer("cantidad")
    val categoria = varchar("categoria", 50)
    val fechaCreacion = datetime("fecha_creacion")
    val fechaActualizacion = datetime("fecha_actualizacion")
}

// Entidad DAO
class Producto(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Producto>(Productos)

    var nombre by Productos.nombre
    var descripcion by Productos.descripcion
    var precio by Productos.precio
    var cantidad by Productos.cantidad
    var categoria by Productos.categoria
    var fechaCreacion by Productos.fechaCreacion
    var fechaActualizacion by Productos.fechaActualizacion
}

// DTOs para la API
@Serializable
data class ProductoRequest(
    val nombre: String,
    val descripcion: String? = null,
    val precio: Double,
    val cantidad: Int,
    val categoria: String
)

@Serializable
data class ProductoResponse(
    val id: Int,
    val nombre: String,
    val descripcion: String? = null,
    val precio: Double,
    val cantidad: Int,
    val categoria: String,
    val fechaCreacion: String,
    val fechaActualizacion: String
)
