package org.example.project.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.example.project.data.api.VentasApiService
import org.example.project.data.models.*
import org.example.project.data.model.Producto

class VentasRepository(private val apiService: VentasApiService) {

    fun obtenerVentas(): Flow<Result<List<Venta>>> = flow {
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

    // Datos de ejemplo para cuando falle la API


    fun obtenerMetricasEjemplo(): MetricasVentas {
        return MetricasVentas(
            ventasHoy = 2350.0,
            ordenesHoy = 23,
            ticketPromedio = 102.17,
            ventasMes = 45231.0,
            crecimientoVentasHoy = 12.0,
            crecimientoOrdenes = 5.0,
            crecimientoTicket = 8.0,
            crecimientoMes = 20.0
        )
    }
}
