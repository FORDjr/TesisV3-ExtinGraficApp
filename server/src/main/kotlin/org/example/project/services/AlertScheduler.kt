package org.example.project.services

import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.concurrent.thread

class AlertScheduler(private val service: ExtintoresService) {
    @Volatile private var running = false

    fun start(intervalMinutes: Long = 60) {
        if (running) return
        running = true
        thread(name = "extintores-alert-scheduler", isDaemon = true) {
            println("[AlertScheduler] Iniciado intervalo=${intervalMinutes}m")
            while (running) {
                try {
                    val ahora = Clock.System.now().toLocalDateTime(TimeZone.UTC)
                    val lista = service.extintoresParaAlerta(30)
                    if (lista.isNotEmpty()) {
                        println("[AlertScheduler] ${lista.size} extintores con vencimiento <=30d sin orden. Timestamp=$ahora")
                        // Placeholder notificación: futuro enviar email/WhatsApp
                        lista.take(5).forEach { println("  - #${it.id} QR=${it.codigoQr} dias=${it.diasParaVencer}") }
                    }
                } catch (e: Exception) {
                    println("[AlertScheduler][ERROR] ${e.message}")
                }
                repeat((intervalMinutes * 6).toInt()) { // dormir en pasos de 10s para permitir parada suave
                    if (!running) return@thread
                    Thread.sleep(10_000)
                }
            }
        }
    }

    fun stop() { running = false }
}

