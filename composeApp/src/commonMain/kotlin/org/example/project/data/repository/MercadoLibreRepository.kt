package org.example.project.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.example.project.data.api.MercadoLibreApiService
import org.example.project.data.models.MercadoLibreAssignment
import org.example.project.data.models.MercadoLibreOrder
import org.example.project.data.models.MercadoLibreOrderStatus

class MercadoLibreRepository(
    private val apiService: MercadoLibreApiService = MercadoLibreApiService()
) {

    private val _orders = MutableStateFlow<List<MercadoLibreOrder>>(emptyList())
    val orders: StateFlow<List<MercadoLibreOrder>> = _orders.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    suspend fun syncLatestOrders() {
        try {
            _isLoading.value = true
            _error.value = null
            val result = apiService.obtenerOrdenesRecientes()
            result.fold(
                onSuccess = { incoming ->
                    _orders.value = mergeOrders(_orders.value, incoming)
                },
                onFailure = { throwable ->
                    _error.value = throwable.message ?: "Error al obtener órdenes de Mercado Libre"
                }
            )
        } catch (e: Exception) {
            _error.value = e.message ?: "Error inesperado al sincronizar Mercado Libre"
        } finally {
            _isLoading.value = false
        }
    }

    fun registerAssignment(orderId: String, assignment: MercadoLibreAssignment) {
        _orders.value = _orders.value.map { order ->
            if (order.id == orderId) {
                order.copy(
                    status = MercadoLibreOrderStatus.ASSIGNED,
                    assignedProduct = assignment
                )
            } else order
        }
    }

    private fun mergeOrders(existing: List<MercadoLibreOrder>, incoming: List<MercadoLibreOrder>): List<MercadoLibreOrder> {
        val existingMap = existing.associateBy { it.id }
        val merged = incoming.map { order ->
            val previous = existingMap[order.id]
            when {
                previous == null -> order
                previous.status == MercadoLibreOrderStatus.ASSIGNED && previous.assignedProduct != null ->
                    previous.copy(
                        date = order.date,
                        buyerName = order.buyerName,
                        totalAmount = order.totalAmount,
                        items = order.items
                    )
                else -> order.copy(
                    status = previous.status,
                    assignedProduct = previous.assignedProduct
                )
            }
        }
        val notPresent = existing.filter { current -> merged.none { it.id == current.id } }
        return (merged + notPresent).sortedByDescending { it.date }
    }
}
