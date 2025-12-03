package org.example.project.models

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

object Proveedores : IntIdTable("proveedores") {
    val nombre = varchar("nombre", 150)
    val contacto = varchar("contacto", 150).nullable()
    val telefono = varchar("telefono", 80).nullable()
    val email = varchar("email", 150).nullable()
    val activo = bool("activo").default(true)
    val fechaCreacion = datetime("fecha_creacion")
    val fechaActualizacion = datetime("fecha_actualizacion")
}

class Proveedor(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Proveedor>(Proveedores)

    var nombre by Proveedores.nombre
    var contacto by Proveedores.contacto
    var telefono by Proveedores.telefono
    var email by Proveedores.email
    var activo by Proveedores.activo
    var fechaCreacion by Proveedores.fechaCreacion
    var fechaActualizacion by Proveedores.fechaActualizacion
}

@Serializable
data class ProveedorResponse(
    val id: Int,
    val nombre: String,
    val contacto: String? = null,
    val telefono: String? = null,
    val email: String? = null,
    val activo: Boolean
)
