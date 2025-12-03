package org.example.project.services

import kotlinx.datetime.*
import org.example.project.models.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

class ExtintoresService {

    /* ---------------- Clientes ---------------- */
    fun crearCliente(req: ClienteRequest): ClienteResponse = transaction {
        val ahora = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        val c = Cliente.new {
            nombre = req.nombre
            rut = req.rut
            activo = true
            fechaCreacion = ahora
            fechaActualizacion = ahora
        }
        ClienteResponse(c.id.value, c.nombre, c.rut, c.activo)
    }

    fun listarClientes(): List<ClienteResponse> = transaction {
        Cliente.all().map { ClienteResponse(it.id.value, it.nombre, it.rut, it.activo) }
    }

    /* ---------------- Sedes ---------------- */
    fun crearSede(req: SedeRequest): SedeResponse = transaction {
        val ahora = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        val s = Sede.new {
            clienteId = EntityID(req.clienteId, Clientes)
            nombre = req.nombre
            direccion = req.direccion
            comuna = req.comuna
            lat = req.lat
            lon = req.lon
            fechaCreacion = ahora
            fechaActualizacion = ahora
        }
        SedeResponse(s.id.value, s.clienteId.value, s.nombre, s.direccion, s.comuna)
    }

    fun listarSedes(clienteId: Int?): List<SedeResponse> = transaction {
        val query = if (clienteId != null) Sede.find { Sedes.clienteId eq clienteId } else Sede.all()
        query.map { SedeResponse(it.id.value, it.clienteId.value, it.nombre, it.direccion, it.comuna) }
    }

    /* ---------------- Extintores ---------------- */
    fun crearExtintor(req: ExtintorRequest): ExtintorResponse = transaction {
        val ahora = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        val fechaVenc = req.fechaProximoVencimiento?.let { LocalDateTime.parse(it) }
        val e = Extintor.new {
            codigoQr = req.codigoQr
            clienteId = EntityID(req.clienteId, Clientes)
            sedeId = req.sedeId?.let { EntityID(it, Sedes) }
            tipo = req.tipo
            agente = req.agente
            capacidad = req.capacidad
            fechaProximoVencimiento = fechaVenc
            estado = EstadoExtintor.VIGENTE
            fechaCreacion = ahora
            fechaActualizacion = ahora
        }
        val (color, dias) = calcularColor(e.fechaProximoVencimiento)
        val estadoActual = estadoDesdeDias(dias)
        e.estado = estadoActual
        ExtintorResponse(
            e.id.value,
            e.codigoQr,
            e.clienteId.value,
            e.sedeId?.value,
            e.tipo,
            e.agente,
            e.capacidad,
            e.fechaProximoVencimiento?.toString(),
            dias,
            color,
            estadoActual
        )
    }

    fun listarExtintores(clienteId: Int?, sedeId: Int?, color: String?, page: Int? = null, size: Int? = null): List<ExtintorResponse> = transaction {
        var lista = Extintor.all().toList()
        if (clienteId != null) lista = lista.filter { it.clienteId.value == clienteId }
        if (sedeId != null) lista = lista.filter { it.sedeId?.value == sedeId }
        val mapped = lista.map {
            val (c, dias) = calcularColor(it.fechaProximoVencimiento)
            val estadoActual = estadoDesdeDias(dias)
            if (it.estado != estadoActual) it.estado = estadoActual
            ExtintorResponse(
                it.id.value,
                it.codigoQr,
                it.clienteId.value,
                it.sedeId?.value,
                it.tipo,
                it.agente,
                it.capacidad,
                it.fechaProximoVencimiento?.toString(),
                dias,
                c,
                estadoActual
            )
        }.let { list -> color?.let { col -> list.filter { it.color == col } } ?: list }
        if (page != null && size != null && page >=0 && size>0) {
            val from = page * size
            val to = minOf(from + size, mapped.size)
            if (from >= mapped.size) emptyList() else mapped.subList(from, to)
        } else mapped
    }

    /* ---------------- Ordenes de Servicio ---------------- */
    fun crearOrden(req: CrearOrdenServicioRequest): OrdenServicioResponse = transaction {
        val ahora = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        val fechaProg = LocalDateTime.parse(req.fechaProgramada)
        val orden = OrdenServicio.new {
            fechaProgramada = fechaProg
            estado = EstadoOrdenServicio.PLANIFICADA
            tecnicoId = req.tecnicoId?.let { EntityID(it, Usuarios) }
            clienteId = EntityID(req.clienteId, Clientes)
            sedeId = req.sedeId?.let { EntityID(it, Sedes) }
            creadoPor = req.creadoPor?.let { EntityID(it, Usuarios) }
            fechaCreacion = ahora
            fechaActualizacion = ahora
        }
        // Asociar extintores
        req.extintores.forEach { extId ->
            OrdenServicioExtintor.new {
                ordenId = orden.id
                extintorId = EntityID(extId, Extintores)
            }
        }
        OrdenServicioResponse(orden.id.value, orden.fechaProgramada.toString(), orden.estado, orden.tecnicoId?.value, orden.clienteId.value, orden.sedeId?.value, req.extintores)
    }

    fun listarOrdenes(clienteId: Int?, estado: EstadoOrdenServicio?): List<OrdenServicioResponse> = transaction {
        var lista = OrdenServicio.all().toList()
        if (clienteId != null) lista = lista.filter { it.clienteId.value == clienteId }
        if (estado != null) lista = lista.filter { it.estado == estado }
        lista.map { ord ->
            val exts = OrdenServicioExtintor.find { OrdenServicioExtintores.ordenId eq ord.id }.map { it.extintorId.value }
            OrdenServicioResponse(ord.id.value, ord.fechaProgramada.toString(), ord.estado, ord.tecnicoId?.value, ord.clienteId.value, ord.sedeId?.value, exts)
        }
    }

    fun actualizarExtintor(id: Int, update: ExtintorUpdateRequest): ExtintorResponse? = transaction {
        val ext = Extintor.findById(id) ?: return@transaction null
        val ahora = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        update.sedeId?.let { ext.sedeId = EntityID(it, Sedes) }
        update.tipo?.let { ext.tipo = it }
        update.agente?.let { ext.agente = it }
        update.capacidad?.let { ext.capacidad = it }
        update.fechaProximoVencimiento?.let { ext.fechaProximoVencimiento = LocalDateTime.parse(it) }
        ext.fechaActualizacion = ahora
        val (color, dias) = calcularColor(ext.fechaProximoVencimiento)
        val estadoActual = estadoDesdeDias(dias)
        if (ext.estado != estadoActual) ext.estado = estadoActual
        ExtintorResponse(
            ext.id.value,
            ext.codigoQr,
            ext.clienteId.value,
            ext.sedeId?.value,
            ext.tipo,
            ext.agente,
            ext.capacidad,
            ext.fechaProximoVencimiento?.toString(),
            dias,
            color,
            estadoActual
        )
    }

    fun actualizarEstadoOrden(id: Int, estado: EstadoOrdenServicio): OrdenServicioResponse? = transaction {
        val ord = OrdenServicio.findById(id) ?: return@transaction null
        val ahora = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        ord.estado = estado
        ord.fechaActualizacion = ahora
        val exts = OrdenServicioExtintor.find { OrdenServicioExtintores.ordenId eq ord.id }.map { it.extintorId.value }
        OrdenServicioResponse(ord.id.value, ord.fechaProgramada.toString(), ord.estado, ord.tecnicoId?.value, ord.clienteId.value, ord.sedeId?.value, exts)
    }

    /* ---------------- Certificados ---------------- */
    fun emitirCertificado(extintorId: Int): CertificadoResponse? = transaction {
        val ext = Extintor.findById(extintorId) ?: return@transaction null
        val ahora = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        val numero = "C-${ahora.date}-${extintorId}-${System.currentTimeMillis()%10000}".replace(":","-")
        val pdfPath = try {
            PdfGenerator.ensureDir()
            PdfGenerator.generarCertificado(numero, ext)
        } catch (e: Exception) {
            println("[PDF][ERROR] ${e.message}")
            null
        }
        val cert = Certificado.new {
            this.extintorId = EntityID(ext.id.value, Extintores)
            this.numero = numero
            this.fechaEmision = ahora
            this.fechaProximoVencimiento = ext.fechaProximoVencimiento
            this.pdfPath = pdfPath
        }
        CertificadoResponse(cert.id.value, ext.id.value, cert.numero, cert.fechaEmision.toString(), cert.fechaProximoVencimiento?.toString(), cert.pdfPath)
    }

    fun listarCertificados(extintorId: Int?): List<CertificadoResponse> = transaction {
        val certs = if (extintorId != null) Certificado.find { Certificados.extintorId eq extintorId } else Certificado.all()
        certs.map { CertificadoResponse(it.id.value, it.extintorId.value, it.numero, it.fechaEmision.toString(), it.fechaProximoVencimiento?.toString(), it.pdfPath) }
    }

    fun obtenerCertificado(id: Int): CertificadoResponse? = transaction {
        Certificado.findById(id)?.let {
            CertificadoResponse(it.id.value, it.extintorId.value, it.numero, it.fechaEmision.toString(), it.fechaProximoVencimiento?.toString(), it.pdfPath)
        }
    }

    fun obtenerRutaPdfCertificado(id: Int): String? = transaction {
        Certificado.findById(id)?.pdfPath
    }

    fun extintoresParaAlerta(dias: Long = 30): List<ExtintorResponse> = transaction {
        val ahora = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        // calcular fecha límite correctamente (no se usa directamente en el filtro, pero queda documentado)
        val limiteDate = ahora.date.plus(DatePeriod(days = dias.toInt()))
        val limite = LocalDateTime(limiteDate, ahora.time)
        Extintor.all().filter { ex ->
            val fv = ex.fechaProximoVencimiento
            if (fv == null) false else {
                val diffDias = (fv.toInstant(TimeZone.UTC).epochSeconds - ahora.toInstant(TimeZone.UTC).epochSeconds) / 86400
                diffDias in 0..dias &&
                        OrdenServicioExtintor.find { OrdenServicioExtintores.extintorId eq ex.id }
                            .map { it.ordenId }
                            .none { ord ->
                                val orden = OrdenServicio.findById(ord.value)
                                orden != null && orden.estado != EstadoOrdenServicio.CANCELADA && orden.fechaProgramada >= ahora
                            }
            }
        }.map {
            val (c, d) = calcularColor(it.fechaProximoVencimiento)
            val estadoActual = estadoDesdeDias(d)
            if (it.estado != estadoActual) it.estado = estadoActual
            ExtintorResponse(
                it.id.value,
                it.codigoQr,
                it.clienteId.value,
                it.sedeId?.value,
                it.tipo,
                it.agente,
                it.capacidad,
                it.fechaProximoVencimiento?.toString(),
                d,
                c,
                estadoActual
            )
        }
    }

    fun obtenerOrden(id: Int): OrdenServicioResponse? = transaction {
        val ord = OrdenServicio.findById(id) ?: return@transaction null
        val exts = OrdenServicioExtintor.find { OrdenServicioExtintores.ordenId eq ord.id }.map { it.extintorId.value }
        OrdenServicioResponse(ord.id.value, ord.fechaProgramada.toString(), ord.estado, ord.tecnicoId?.value, ord.clienteId.value, ord.sedeId?.value, exts)
    }

    fun obtenerExtintor(id: Int): ExtintorResponse? = transaction {
        val e = Extintor.findById(id) ?: return@transaction null
        val (color, dias) = calcularColor(e.fechaProximoVencimiento)
        val estadoActual = estadoDesdeDias(dias)
        if (e.estado != estadoActual) e.estado = estadoActual
        ExtintorResponse(
            e.id.value,
            e.codigoQr,
            e.clienteId.value,
            e.sedeId?.value,
            e.tipo,
            e.agente,
            e.capacidad,
            e.fechaProximoVencimiento?.toString(),
            dias,
            color,
            estadoActual
        )
    }

    private fun estadoDesdeDias(dias: Long?): EstadoExtintor = when {
        dias == null -> EstadoExtintor.VIGENTE
        dias <= 0 -> EstadoExtintor.VENCIDO
        dias <= 30 -> EstadoExtintor.POR_VENCER
        else -> EstadoExtintor.VIGENTE
    }
}
