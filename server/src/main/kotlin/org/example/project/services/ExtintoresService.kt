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
            ubicacion = req.ubicacion
            estadoLogistico = req.estadoLogistico ?: EstadoLogisticoExtintor.DISPONIBLE
            fechaProximoVencimiento = fechaVenc
            estado = EstadoExtintor.VIGENTE
            fechaCreacion = ahora
            fechaActualizacion = ahora
        }
        e.refreshEstado(ahora).first
    }

    fun listarExtintores(clienteId: Int?, sedeId: Int?, color: String?, page: Int? = null, size: Int? = null): List<ExtintorResponse> = transaction {
        var lista = Extintor.all().toList()
        if (clienteId != null) lista = lista.filter { it.clienteId.value == clienteId }
        if (sedeId != null) lista = lista.filter { it.sedeId?.value == sedeId }
        val mapped = lista.map { it.refreshEstado().first }
            .let { list -> color?.let { col -> list.filter { it.color == col } } ?: list }
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
        update.ubicacion?.let { ext.ubicacion = it }
        update.estadoLogistico?.let { ext.estadoLogistico = it }
        update.fechaProximoVencimiento?.let { ext.fechaProximoVencimiento = LocalDateTime.parse(it) }
        ext.fechaActualizacion = ahora
        ext.refreshEstado(ahora).first
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
    fun emitirCertificado(extintorId: Int, supervisorId: Int? = null): CertificadoResponse? = transaction {
        val ext = Extintor.findById(extintorId) ?: return@transaction null
        val ahora = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        val numero = "C-${ahora.date}-${extintorId}-${System.currentTimeMillis()%10000}".replace(":","-")
        val cliente = Cliente.findById(ext.clienteId)
        val sede = ext.sedeId?.let { Sede.findById(it) }
        val supervisor = supervisorId?.let { Usuario.findById(it) }
        val pdfPath = try {
            PdfGenerator.ensureDir()
            PdfGenerator.generarCertificado(numero, ext, cliente, sede, supervisor)
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
        }.map { it.refreshEstado(ahora).first }
    }

    fun obtenerOrden(id: Int): OrdenServicioResponse? = transaction {
        val ord = OrdenServicio.findById(id) ?: return@transaction null
        val exts = OrdenServicioExtintor.find { OrdenServicioExtintores.ordenId eq ord.id }.map { it.extintorId.value }
        OrdenServicioResponse(ord.id.value, ord.fechaProgramada.toString(), ord.estado, ord.tecnicoId?.value, ord.clienteId.value, ord.sedeId?.value, exts)
    }

    fun obtenerExtintor(id: Int): ExtintorResponse? = transaction {
        val e = Extintor.findById(id) ?: return@transaction null
        e.refreshEstado().first
    }

    fun recalcularEstados(): RecalculoExtintoresResponse = transaction {
        val ahora = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        var actualizados = 0
        val respuestas = Extintor.all().map { ext ->
            val (resp, changed) = ext.refreshEstado(ahora)
            if (changed) actualizados++
            resp
        }
        RecalculoExtintoresResponse(
            total = respuestas.size,
            actualizados = actualizados,
            vencidos = respuestas.count { it.estado == EstadoExtintor.VENCIDO },
            porVencer = respuestas.count { it.estado == EstadoExtintor.POR_VENCER },
            vigentes = respuestas.count { it.estado == EstadoExtintor.VIGENTE }
        )
    }

    fun agendaEventos(maxDias: Int = 60): List<AgendaEventResponse> = transaction {
        val ahora = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        val ahoraInstant = ahora.toInstant(TimeZone.UTC)
        val extEvents = Extintor.all().mapNotNull { ext ->
            val fecha = ext.fechaProximoVencimiento ?: return@mapNotNull null
            val (resp, _) = ext.refreshEstado(ahora)
            val diffDias = (fecha.toInstant(TimeZone.UTC).epochSeconds - ahoraInstant.epochSeconds) / 86400
            if (diffDias > maxDias) return@mapNotNull null
            val clienteNombre = Cliente.findById(ext.clienteId)?.nombre ?: "Cliente #${ext.clienteId.value}"
            val sedeNombre = ext.sedeId?.let { sid -> Sede.findById(sid)?.nombre }
            val descripcion = buildString {
                append("Vence el ${fecha.date}")
                append(" • ")
                append(clienteNombre)
                sedeNombre?.let { append(" / $it") }
            }
            AgendaEventResponse(
                id = ext.id.value,
                title = "Venc. ${ext.codigoQr}",
                date = fecha.date.toString(),
                rawDateTime = fecha.toString(),
                daysToExpire = diffDias,
                color = resp.color,
                type = "EXTINTOR",
                referenceId = ext.id.value,
                estado = resp.estado.name,
                cliente = clienteNombre,
                sede = sedeNombre,
                descripcion = descripcion,
                codigo = ext.codigoQr
            )
        }

        val ordenEvents = OrdenServicio.all().mapNotNull { ord ->
            val diffDias = (ord.fechaProgramada.toInstant(TimeZone.UTC).epochSeconds - ahoraInstant.epochSeconds) / 86400
            if (diffDias > maxDias) return@mapNotNull null
            val clienteNombre = Cliente.findById(ord.clienteId)?.nombre ?: "Cliente #${ord.clienteId.value}"
            val sedeNombre = ord.sedeId?.let { sid -> Sede.findById(sid)?.nombre }
            val descripcion = buildString {
                append("Programada para ${ord.fechaProgramada.date}")
                append(" • ")
                append(clienteNombre)
                sedeNombre?.let { append(" / $it") }
            }
            AgendaEventResponse(
                id = 100000 + ord.id.value,
                title = "Orden ${ord.id.value}",
                date = ord.fechaProgramada.date.toString(),
                rawDateTime = ord.fechaProgramada.toString(),
                daysToExpire = diffDias,
                color = when (ord.estado) {
                    EstadoOrdenServicio.PLANIFICADA -> "amarillo"
                    EstadoOrdenServicio.EN_PROGRESO -> "verde"
                    EstadoOrdenServicio.CERRADA -> "gris"
                    EstadoOrdenServicio.CANCELADA -> "gris"
                },
                type = "ORDEN",
                referenceId = ord.id.value,
                estado = ord.estado.name,
                cliente = clienteNombre,
                sede = sedeNombre,
                descripcion = descripcion
            )
        }

        val alertaEvents = Alerta.all().mapNotNull { alerta ->
            val fecha = alerta.fechaGenerada
            val diffDias = (fecha.toInstant(TimeZone.UTC).epochSeconds - ahoraInstant.epochSeconds) / 86400
            if (diffDias > maxDias) return@mapNotNull null
            var clienteNombre: String? = null
            var sedeNombre: String? = null
            var codigoQr: String? = null
            var descripcion = "Alerta sin detalle"

            val extId = alerta.extintorId?.value
            if (extId != null) {
                val ext = Extintor.findById(extId)
                clienteNombre = ext?.clienteId?.let { cid -> Cliente.findById(cid)?.nombre }
                sedeNombre = ext?.sedeId?.let { sid -> Sede.findById(sid)?.nombre }
                codigoQr = ext?.codigoQr
                descripcion = ext?.fechaProximoVencimiento?.let { fv ->
                    "Extintor ${ext.codigoQr} vence el ${fv.date}"
                } ?: "Extintor ${ext?.codigoQr ?: extId}"
            } else if (alerta.productoId != null) {
                val prod = Producto.findById(alerta.productoId!!.value)
                codigoQr = prod?.codigo
                descripcion = "Stock crítico: ${prod?.nombre ?: "Producto #${alerta.productoId!!.value}"}"
            }
            AgendaEventResponse(
                id = 200000 + alerta.id.value,
                title = if (alerta.tipo.equals("STOCK", ignoreCase = true)) "Alerta de stock" else "Alerta de vencimiento",
                date = fecha.date.toString(),
                rawDateTime = fecha.toString(),
                daysToExpire = diffDias,
                color = if (alerta.tipo.equals("STOCK", ignoreCase = true)) "amarillo" else "rojo",
                type = "ALERTA",
                referenceId = alerta.extintorId?.value ?: alerta.productoId?.value,
                estado = if (alerta.enviada) "ENVIADA" else "PENDIENTE",
                cliente = clienteNombre,
                sede = sedeNombre,
                descripcion = descripcion,
                codigo = codigoQr,
                alertaId = alerta.id.value
            )
        }

        (extEvents + ordenEvents + alertaEvents).sortedBy { it.rawDateTime ?: it.date }
    }

    private fun estadoDesdeDias(dias: Long?): EstadoExtintor = when {
        dias == null -> EstadoExtintor.VIGENTE
        dias <= 0 -> EstadoExtintor.VENCIDO
        dias <= 30 -> EstadoExtintor.POR_VENCER
        else -> EstadoExtintor.VIGENTE
    }

    private fun Extintor.refreshEstado(ahora: LocalDateTime? = null): Pair<ExtintorResponse, Boolean> {
        val (color, dias) = calcularColor(fechaProximoVencimiento)
        val estadoActual = estadoDesdeDias(dias)
        val changed = estado != estadoActual
        if (changed) {
            estado = estadoActual
            if (ahora != null) fechaActualizacion = ahora
        }
        return ExtintorResponse(
            id = id.value,
            codigoQr = codigoQr,
            clienteId = clienteId.value,
            sedeId = sedeId?.value,
            tipo = tipo,
            agente = agente,
            capacidad = capacidad,
            ubicacion = ubicacion,
            estadoLogistico = estadoLogistico,
            fechaProximoVencimiento = fechaProximoVencimiento?.toString(),
            diasParaVencer = dias,
            color = color,
            estado = estadoActual
        ) to changed
    }
}
