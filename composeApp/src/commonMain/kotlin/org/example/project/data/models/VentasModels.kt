package org.example.project.data.models

import kotlinx.serialization.Serializable
import org.example.project.data.model.Producto
import org.example.project.data.model.ProductoUI

@Serializable
data class Venta(
    val id: String = "",
    val cliente: String,
    val fecha: String,
    val total: Double,
    val estado: EstadoVenta,
    val metodoPago: MetodoPago,
    val observaciones: String? = null,
    val productos: List<ProductoVenta> = emptyList()
)

@Serializable
data class ProductoVenta(
    val id: Int,
    val nombre: String,
    val cantidad: Int,
    val precio: Double,
    val subtotal: Double
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
    val ventasHoy: Double = 0.0,
    val ordenesHoy: Int = 0,
    val ticketPromedio: Double = 0.0,
    val ventasMes: Double = 0.0,
    val crecimientoVentasHoy: Double = 0.0,
    val crecimientoOrdenes: Double = 0.0,
    val crecimientoTicket: Double = 0.0,
    val crecimientoMes: Double = 0.0
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
    val observaciones: String? = null
)

@Serializable
data class ProductoVentaRequest(
    val id: Int,
    val cantidad: Int,
    val precio: Double
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
    val searchQuery: String = "",
    val filtroEstado: EstadoVenta? = null,
    val filtroFecha: FiltroFecha = FiltroFecha.TODOS
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
    val error: String? = null
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
    val categorias: List<String> = listOf("Extintores", "Test", "Electrónicos", "Otros"),
    val categoriasPersonalizadas: List<String> = emptyList()
) {
    val categoriasCompletas: List<String> get() = categorias + categoriasPersonalizadas
    val esValido: Boolean get() = nombre.isNotBlank() &&
                                  precio.toDoubleOrNull() != null &&
                                  cantidad.toIntOrNull() != null &&
                                  categoriaSeleccionada.isNotBlank()
}
