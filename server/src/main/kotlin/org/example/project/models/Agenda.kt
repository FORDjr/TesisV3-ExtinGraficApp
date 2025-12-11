package org.example.project.models

import kotlinx.serialization.Serializable

@Serializable
data class AgendaEventResponse(
    val id: Int,
    val title: String,
    val date: String,
    val rawDateTime: String? = null,
    val daysToExpire: Long? = null,
    val color: String = "gris",
    val type: String = "EXTINTOR",
    val referenceId: Int? = null,
    val estado: String? = null,
    val cliente: String? = null,
    val sede: String? = null,
    val descripcion: String? = null,
    val codigo: String? = null,
    val alertaId: Int? = null
)

@Serializable
data class RecalculoExtintoresResponse(
    val total: Int,
    val actualizados: Int,
    val vencidos: Int,
    val porVencer: Int,
    val vigentes: Int
)
