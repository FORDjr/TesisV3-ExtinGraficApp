package org.example.project.data.api

import kotlinx.serialization.Serializable

@Serializable
data class RemoteExtintor(
    val id: Int,
    val codigoQr: String,
    val clienteId: Int,
    val sedeId: Int? = null,
    val tipo: String,
    val agente: String,
    val capacidad: String,
    val ubicacion: String? = null,
    val estadoLogistico: String? = null,
    val fechaProximoVencimiento: String? = null,
    val diasParaVencer: Long? = null,
    val color: String = "verde",
    val estado: String? = null
)

@Serializable
data class RemoteOrdenServicio(
    val id: Int,
    val fechaProgramada: String,
    val estado: String,
    val tecnicoId: Int? = null,
    val clienteId: Int,
    val sedeId: Int? = null,
    val extintores: List<Int> = emptyList()
)

@Serializable
data class RemoteCliente(
    val id: Int,
    val nombre: String,
    val rut: String = "",
    val activo: Boolean = true
)

@Serializable
data class RemoteSede(
    val id: Int,
    val clienteId: Int,
    val nombre: String,
    val direccion: String? = null,
    val comuna: String? = null
)

@Serializable
data class ItemUsoProductoDto(val productoId: Int, val cantidad: Int)

@Serializable
data class CrearServiceRegistroRequest(
    val extintorId: Int,
    val ordenId: Int? = null,
    val tecnicoId: Int? = null,
    val pesoInicial: String? = null,
    val observaciones: String? = null,
    val productos: List<ItemUsoProductoDto> = emptyList()
)

@Serializable
data class ServiceRegistroResponse(
    val id: Int,
    val extintorId: Int,
    val ordenId: Int? = null,
    val tecnicoId: Int? = null,
    val fechaRegistro: String,
    val fechaProximoVencimiento: String? = null,
    val numeroCertificado: String? = null,
    val productos: List<ItemUsoProductoDto> = emptyList()
)

@Serializable
data class CrearExtintorRequest(
    val codigoQr: String,
    val clienteId: Int,
    val sedeId: Int? = null,
    val tipo: String,
    val agente: String,
    val capacidad: String,
    val ubicacion: String? = null,
    val estadoLogistico: String? = null
)

@Serializable
data class ActualizarExtintorRequest(
    val ubicacion: String? = null,
    val estadoLogistico: String? = null
)
