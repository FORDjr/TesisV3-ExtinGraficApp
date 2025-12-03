package org.example.project.services

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.example.project.models.*
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction

class AlertService(private val extintoresService: ExtintoresService = ExtintoresService()) {

    fun generarAlertasVencimiento(): Int = transaction {
        val ahora = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        val candidatos = extintoresService.extintoresParaAlerta(30)
        var creadas = 0
        candidatos.forEach { extResp ->
            val extIdEntity = org.jetbrains.exposed.dao.id.EntityID(extResp.id, Extintores)
            val existe = Alerta.find { (Alertas.extintorId eq extIdEntity) and (Alertas.tipo eq "VENCIMIENTO") and (Alertas.enviada eq false) }
                .empty().not()
            if (!existe) {
                Alerta.new {
                    extintorId = extIdEntity
                    productoId = null
                    tipo = "VENCIMIENTO"
                    fechaGenerada = ahora
                    enviada = false
                    reintentos = 0
                }
                creadas++
            }
        }
        creadas
    }

    fun generarAlertasStock(): Int = transaction {
        val ahora = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        var creadas = 0
        Producto.all().forEach { p ->
            if (p.cantidad <= p.stockMinimo) {
                val prodIdEntity = org.jetbrains.exposed.dao.id.EntityID(p.id.value, Productos)
                val existe = Alerta.find { (Alertas.productoId eq prodIdEntity) and (Alertas.tipo eq "STOCK") and (Alertas.enviada eq false) }
                    .empty().not()
                if (!existe) {
                    Alerta.new {
                        extintorId = null
                        productoId = prodIdEntity
                        tipo = "STOCK"
                        fechaGenerada = ahora
                        enviada = false
                        reintentos = 0
                    }
                    creadas++
                }
            }
        }
        creadas
    }

    fun reenviarPendientes(maxDias: Long = 5): Int = transaction {
        val ahoraInstant = Clock.System.now() // Instant actual
        var marcadas = 0
        Alerta.all().filter { !it.enviada }.forEach { a ->
            // fechaGenerada es LocalDateTime (kotlinx) => convertir a Instant asumiendo UTC
            val genInstant: Instant = a.fechaGenerada.toInstant(TimeZone.UTC)
            val diffDias = (ahoraInstant.epochSeconds - genInstant.epochSeconds) / 86400
            if (diffDias >= maxDias) {
                a.reintentos = a.reintentos + 1
                // Aquí podrías integrar email/notification real
                marcadas++
            }
        }
        marcadas
    }

    fun listar(pendientes: Boolean? = null): List<AlertaResponse> = transaction {
        val query = Alerta.all().let { all ->
            when (pendientes) {
                true -> all.filter { !it.enviada }
                false -> all.filter { it.enviada }
                else -> all.toList()
            }
        }
        query.map { a ->
            AlertaResponse(
                id = a.id.value,
                extintorId = a.extintorId?.value,
                productoId = a.productoId?.value,
                tipo = a.tipo,
                fechaGenerada = a.fechaGenerada.toString(),
                enviada = a.enviada,
                reintentos = a.reintentos
            )
        }
    }

    fun marcarEnviada(id: Int): AlertaResponse? = transaction {
        val ahora = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        Alerta.findById(id)?.let { a ->
            a.enviada = true
            a.fechaEnvio = ahora
            AlertaResponse(
                id = a.id.value,
                extintorId = a.extintorId?.value,
                productoId = a.productoId?.value,
                tipo = a.tipo,
                fechaGenerada = a.fechaGenerada.toString(),
                enviada = a.enviada,
                reintentos = a.reintentos
            )
        }
    }
}
