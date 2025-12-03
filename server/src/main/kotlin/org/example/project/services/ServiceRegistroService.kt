package org.example.project.services

import kotlinx.datetime.*
import org.example.project.RECARGA_MESES_POR_AGENTE
import org.example.project.models.*
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

class ServiceRegistroService(
    private val extService: ExtintoresService = ExtintoresService()
) {
    private val movimientosService = MovimientosInventarioService()

    private fun proximaRecarga(ext: Extintor): LocalDateTime {
        val meses = RECARGA_MESES_POR_AGENTE[ext.agente.uppercase()] ?: 12
        val base = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        val proxDate = base.date.plus(DatePeriod(months = meses))
        return LocalDateTime(proxDate, base.time)
    }

    fun crear(req: CrearServiceRegistroRequest): ServiceRegistroResponse = transaction {
        val ext = Extintor.findById(req.extintorId) ?: error("Extintor no existe")
        val ahora = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        val prox = proximaRecarga(ext)
        ext.fechaUltimaRecarga = ahora
        ext.fechaProximoVencimiento = prox
        ext.fechaActualizacion = ahora

        val registro = ServiceRegistro.new {
            extintorId = EntityID(ext.id.value, Extintores)
            ordenId = req.ordenId?.let { EntityID(it, OrdenesServicio) }
            tecnicoId = req.tecnicoId?.let { EntityID(it, Usuarios) }
            pesoInicial = req.pesoInicial
            observaciones = req.observaciones
            fechaRegistro = ahora
            fechaProximoVencimiento = prox
            numeroCertificado = null // se asignará luego
        }

        // Descontar inventario productos usados
        val items = mutableListOf<ItemUsoProducto>()
        req.productos.forEach { p ->
            val prod = Producto.findById(p.productoId) ?: error("Producto ${p.productoId} no existe")
            if (prod.cantidad < p.cantidad) error("Stock insuficiente para producto ${prod.id.value}")
            ServiceRegistroProducto.new {
                serviceId = registro.id
                productoId = prod.id
                cantidad = p.cantidad
            }
            items += ItemUsoProducto(prod.id.value, p.cantidad)
            movimientosService.crearMovimiento(
                CrearMovimientoInventarioRequest(
                    productoId = prod.id.value,
                    tipo = TipoMovimientoInventario.SALIDA,
                    cantidad = p.cantidad,
                    motivo = "Servicio extintor ${ext.codigoQr}",
                    documento = req.ordenId?.let { "OS-${req.ordenId}" } ?: "SERV-${registro.id.value}",
                    usuarioId = req.tecnicoId,
                    observaciones = req.observaciones,
                    fechaRegistro = ahora.toString()
                )
            )
        }

        // Emitir certificado
        val cert = extService.emitirCertificado(ext.id.value)
        if (cert != null) {
            registro.numeroCertificado = cert.numero
            registro.fechaProximoVencimiento = ext.fechaProximoVencimiento
        }

        // Cerrar orden si todos extintores tienen registro
        req.ordenId?.let { oid ->
            val orden = OrdenServicio.findById(oid)
            if (orden != null) {
                val extIdsOrden = OrdenServicioExtintor.find { OrdenServicioExtintores.ordenId eq orden.id }.map { it.extintorId.value }
                val registrados = ServiceRegistro.find { ServiceRegistros.ordenId eq orden.id }.map { it.extintorId.value }
                if (extIdsOrden.all { registrados.contains(it) }) {
                    orden.estado = EstadoOrdenServicio.CERRADA
                    orden.fechaActualizacion = ahora
                }
            }
        }

        ServiceRegistroResponse(
            id = registro.id.value,
            extintorId = ext.id.value,
            ordenId = req.ordenId,
            tecnicoId = req.tecnicoId,
            fechaRegistro = registro.fechaRegistro.toString(),
            fechaProximoVencimiento = registro.fechaProximoVencimiento?.toString(),
            numeroCertificado = registro.numeroCertificado,
            productos = items
        )
    }

    fun listar(extintorId: Int?, ordenId: Int?): List<ServiceRegistroResponse> = transaction {
        var all = ServiceRegistro.all().toList()
        if (extintorId != null) all = all.filter { it.extintorId.value == extintorId }
        if (ordenId != null) all = all.filter { it.ordenId?.value == ordenId }
        all.map { reg ->
            val productos = ServiceRegistroProducto.find { ServiceRegistroProductos.serviceId eq reg.id }
                .map { ItemUsoProducto(it.productoId.value, it.cantidad) }
            ServiceRegistroResponse(
                id = reg.id.value,
                extintorId = reg.extintorId.value,
                ordenId = reg.ordenId?.value,
                tecnicoId = reg.tecnicoId?.value,
                fechaRegistro = reg.fechaRegistro.toString(),
                fechaProximoVencimiento = reg.fechaProximoVencimiento?.toString(),
                numeroCertificado = reg.numeroCertificado,
                productos = productos
            )
        }
    }
}
