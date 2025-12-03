package org.example.project.data.models

import kotlinx.serialization.Serializable
import org.example.project.data.model.Producto
import org.example.project.data.model.ProductoUI

@Serializable
data class Venta(
    val id: String = "",
    val numero: String = "",
    val cliente: String,
    val fecha: String,
    val total: Long,
    val descuento: Long = 0L,
    val estado: EstadoVenta,
    val metodoPago: MetodoPago,
    val vendedorId: Int? = null,
    val observaciones: String? = null,
    val productos: List<ProductoVenta> = emptyList(),
    val subtotal: Long = productos.sumOf { it.subtotal },
    val correlativo: String = numero.ifBlank { id }
)

@Serializable
data class ProductoVenta(
    val id: Int,
    val nombre: String,
    val cantidad: Int,
    val precio: Long,
    val subtotal: Long
)

// Modelo para items en el carrito de venta (UI)
data class ItemVenta(
    val producto: Producto,
    val cantidad: Int,
    val subtotal: Double = producto.precio * cantidad
)

@Serializable
enum class EstadoVenta {
    PENDIENTE,
    COMPLETADA,
    CANCELADA
}

@Serializable
enum class MetodoPago {
    EFECTIVO,
    TARJETA,
    TRANSFERENCIA,
    CREDITO
}

@Serializable
data class MetricasVentas(
    val ventasHoy: Long = 0L,
    val ordenesHoy: Int = 0,
    val ticketPromedio: Long = 0L,
    val ventasMes: Long = 0L,
    val crecimientoVentasHoy: Int = 0,
    val crecimientoOrdenes: Int = 0,
    val crecimientoTicket: Int = 0,
    val crecimientoMes: Int = 0
)

@Serializable
data class VentasListResponse(
    val ventas: List<Venta> = emptyList(),
    val metricas: MetricasVentas = MetricasVentas()
)

@Serializable
data class ProductoCarrito(
    val id: Int,
    val nombre: String,
    val precio: Double,
    val cantidad: Int,
    val stock: Int,
    val descripcion: String? = null
) {
    val subtotal: Double
        get() = precio * cantidad
}

@Serializable
data class NuevaVentaRequest(
    val cliente: String,
    val productos: List<ProductoVentaRequest>,
    val metodoPago: MetodoPago,
    val vendedorId: Int? = null,
    val descuento: Long? = null,
    val observaciones: String? = null
)

@Serializable
data class ProductoVentaRequest(
    val id: Int,
    val cantidad: Int
)

@Serializable
data class VentaRequest(
    val cliente: String,
    val productos: List<ProductoVentaRequest>,
    val metodoPago: MetodoPago,
    val vendedorId: Int? = null,
    val descuento: Long? = null,
    val observaciones: String? = null
)

@Serializable
data class Categoria(
    val id: Int = 0,
    val nombre: String,
    val descripcion: String? = null,
    val fechaCreacion: String = ""
)

@Serializable
data class NuevoProductoRequest(
    val nombre: String,
    val descripcion: String? = null,
    val precio: Double,
    val cantidad: Int,
    val categoria: String
)

// Estados de UI
data class VentasUiState(
    val ventas: List<Venta> = emptyList(),
    val metricas: MetricasVentas = MetricasVentas(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val searchQuery: String = "",
    val filtroEstado: EstadoVenta? = null,
    val filtroFecha: FiltroFecha = FiltroFecha.TODOS,
    val mercadoLibreOrders: List<MercadoLibreOrder> = emptyList(),
    val mercadoLibreIsLoading: Boolean = false,
    val mercadoLibreError: String? = null
)

enum class FiltroFecha {
    TODOS, HOY, SEMANA, MES
}

data class NuevaVentaUiState(
    val cliente: String = "",
    val metodoPago: MetodoPago? = null,
    val observaciones: String = "",
    val productos: List<ProductoCarrito> = emptyList(),
    val productosDisponibles: List<ProductoUI> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val ventaCreada: Venta? = null
) {
    val total: Double
        get() = productos.sumOf { it.subtotal }

    val isValid: Boolean
        get() = cliente.isNotBlank() && metodoPago != null && productos.isNotEmpty()
}

// Estados para UI de nuevo producto
data class NuevoProductoUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val nombre: String = "",
    val descripcion: String = "",
    val precio: String = "",
    val cantidad: String = "",
    val categoriaSeleccionada: String = "",
    val nuevaCategoria: String = "",
    val mostrarInputCategoria: Boolean = false,
    val categorias: List<String> = listOf("Extintores", "Test", "Electronicos", "Otros"),
    val categoriasPersonalizadas: List<String> = emptyList()
) {
    val categoriasCompletas: List<String> get() = categorias + categoriasPersonalizadas
    val esValido: Boolean get() = nombre.isNotBlank() &&
        precio.toDoubleOrNull() != null &&
        cantidad.toIntOrNull() != null &&
        categoriaSeleccionada.isNotBlank()
}

data class MercadoLibreAssignmentUiState(
    val isDialogOpen: Boolean = false,
    val selectedOrder: MercadoLibreOrder? = null,
    val availableProducts: List<ProductoUI> = emptyList(),
    val selectedProduct: ProductoUI? = null,
    val quantity: Int = 1,
    val isProcessing: Boolean = false,
    val error: String? = null
) {
    val canConfirm: Boolean get() =
        isDialogOpen && selectedOrder != null && selectedProduct != null && quantity > 0
}
