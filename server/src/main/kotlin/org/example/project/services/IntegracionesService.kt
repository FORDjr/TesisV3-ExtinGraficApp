package org.example.project.services

import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.example.project.models.*
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.count
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import java.util.UUID

class IntegracionesService(
    private val inventarioService: InventarioService = InventarioService(),
    private val movimientosService: MovimientosInventarioService = MovimientosInventarioService()
 ) {

    fun validarToken(token: String, required: Set<IntegrationScope> = emptySet()): Integracion = transaction {
        val integ = Integracion.find { Integraciones.token eq token }.firstOrNull()
            ?: error("Token de integración inválido")
        if (!integ.activo) error("Integración deshabilitada")
        if (required.isNotEmpty()) {
            val granted = integ.scopeSet()
            val falta = required.any { it !in granted }
            if (falta) error("El token no tiene permisos suficientes")
        }
        integ.touch()
        integ
    }

    fun estado(integ: Integracion): IntegrationStatusResponse = IntegrationStatusResponse(
        nombre = integ.nombre,
        scopes = integ.scopeSet(),
        activo = integ.activo,
        creadoEn = integ.creadoEn.toString(),
        ultimoUso = integ.ultimoUso?.toString(),
        hits = integ.hits
    )

    fun resumenInventario(): IntegrationInventarioResumen = transaction {
        val total = Productos.selectAll().count().toInt()
        val activos = Producto.find { Productos.estado eq EstadoProducto.ACTIVO }.count().toInt()
        val inactivos = total - activos
        val stockCritico = Producto.find {
            (Productos.estado eq EstadoProducto.ACTIVO) and (Productos.cantidad lessEq Productos.stockMinimo)
        }.count().toInt()
        val pendientes = MovimientoInventario.find {
            MovimientosInventario.estadoAprobacion eq EstadoAprobacionMovimiento.PENDIENTE
        }.count().toInt()
        IntegrationInventarioResumen(
            totalProductos = total,
            activos = activos,
            inactivos = inactivos,
            stockCritico = stockCritico,
            pendientesAprobacion = pendientes
        )
    }

    fun listarMovimientos(
        filtros: MovimientosInventarioService.Filtros,
        limit: Int,
        offset: Int
    ) = movimientosService.listarMovimientos(filtros, limit, offset)

    fun crearMovimiento(request: CrearMovimientoInventarioRequest): MovimientoInventarioResponse {
        val withKey = request.copy(
            idempotenciaKey = request.idempotenciaKey ?: "ext-int-${UUID.randomUUID()}"
        )
        return movimientosService.crearMovimiento(withKey)
    }

    fun exportarCsv(filtros: MovimientosInventarioService.Filtros): String =
        movimientosService.exportarCsv(filtros)
}
