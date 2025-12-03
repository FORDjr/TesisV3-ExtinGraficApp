package org.example.project.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ExtintorResponse(
    val id: Int,
    val codigoQr: String,
    val clienteId: Int,
    val sedeId: Int? = null,
    val tipo: String,
    val agente: String,
    val capacidad: String,
    val fechaProximoVencimiento: String? = null,
    val diasParaVencer: Long? = null,
    val color: String = "gris"
)

