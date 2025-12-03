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
    val fechaProximoVencimiento: String? = null,
    val diasParaVencer: Long? = null,
    val color: String = "verde"
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
