package org.example.project.data.models

import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Representa una orden traída desde Mercado Libre que aún debe conciliarse con el inventario local.
 */
data class MercadoLibreOrder(
    val id: String,
    val date: String,
    val buyerName: String,
    val totalAmount: Double,
    val items: List<MercadoLibreOrderItem>,
    val status: MercadoLibreOrderStatus = MercadoLibreOrderStatus.PENDING_ASSIGNMENT,
    val assignedProduct: MercadoLibreAssignment? = null,
    val channel: String = "Mercado Libre"
) {
    val requiresAssignment: Boolean get() = status == MercadoLibreOrderStatus.PENDING_ASSIGNMENT
}

/**
 * Producto (título) tal como viene desde Mercado Libre.
 */
data class MercadoLibreOrderItem(
    val mlItemId: String,
    val title: String,
    val quantity: Int,
    val unitPrice: Double
) {
    val lineTotal: Double get() = quantity * unitPrice
}

data class MercadoLibreAssignment(
    val productId: Int,
    val productName: String,
    val quantity: Int,
    val assignedAtIso: String = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()
)

enum class MercadoLibreOrderStatus {
    PENDING_ASSIGNMENT,
    ASSIGNED
}
