package org.example.project.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.example.project.data.api.VentasApiService
import org.example.project.data.models.*
import org.example.project.data.model.Producto

class VentasRepository(private val apiService: VentasApiService) {

    fun obtenerVentasResumen(): Flow<Result<VentasListResponse>> = flow {
        emit(apiService.obtenerVentas())
    }

    fun obtenerMetricas(): Flow<Result<MetricasVentas>> = flow {
        emit(apiService.obtenerMetricas())
    }

    suspend fun crearVenta(nuevaVenta: NuevaVentaRequest): Result<Venta> {
        return apiService.crearVenta(nuevaVenta)
    }

    suspend fun obtenerVentaPorId(id: String): Result<Venta> {
        return apiService.obtenerVentaPorId(id)
    }

    suspend fun actualizarEstadoVenta(id: String, nuevoEstado: EstadoVenta): Result<Venta> {
        return apiService.actualizarEstadoVenta(id, nuevoEstado)
    }

    fun obtenerProductosParaVenta(): Flow<Result<List<Producto>>> = flow {
        emit(apiService.obtenerProductosParaVenta())
    }

    fun obtenerMetricasEjemplo(): MetricasVentas {
        return MetricasVentas(
            ventasHoy = 235000L,
            ordenesHoy = 23,
            ticketPromedio = 102000L,
            ventasMes = 4523100L,
            crecimientoVentasHoy = 12,
            crecimientoOrdenes = 5,
            crecimientoTicket = 8,
            crecimientoMes = 20
        )
    }
}
