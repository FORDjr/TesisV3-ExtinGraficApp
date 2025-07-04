package org.example.project.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.example.project.data.api.InventarioApiService
import org.example.project.data.model.Producto
import org.example.project.data.model.ProductoRequest
import org.example.project.data.model.toUI
import org.example.project.data.model.ProductoUI

class InventarioRepository {

    private val apiService = InventarioApiService()

    // Estado reactivo de la lista de productos
    private val _productos = MutableStateFlow<List<ProductoUI>>(emptyList())
    val productos: StateFlow<List<ProductoUI>> = _productos.asStateFlow()

    // Estado de carga
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Estado de error
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    /**
     * Verificar conexión con el servidor
     */
    suspend fun verificarConexion(): Boolean {
        return try {
            _isLoading.value = true
            _error.value = null
            apiService.verificarConexion()
        } catch (e: Exception) {
            _error.value = "Error de conexión: ${e.message}"
            false
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Cargar todos los productos desde la API con fallback a modo demo
     */
    suspend fun cargarProductos() {
        try {
            _isLoading.value = true
            _error.value = null

            println("🔄 Intentando cargar productos desde servidor...")

            // Intentar cargar desde API
            val productosApi = apiService.obtenerProductos()
            val productosUI = productosApi.map { it.toUI() }

            _productos.value = productosUI
            println("✅ Productos cargados desde servidor: ${productosUI.size} productos")

        } catch (e: Exception) {
            // Si falla la API, cargar datos demo INMEDIATAMENTE
            println("🔄 Error cargando desde servidor: ${e.message}")
            println("🔄 Activando datos demo...")
            cargarDatosDemo()
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Cargar datos demo para modo offline
     */
    private fun cargarDatosDemo() {
        val fechaHoy = "2025-01-03" // Fecha actual para los productos demo

        val productosDemo = listOf(
            ProductoUI(
                id = 1,
                nombre = "Extintor PQS 6kg",
                categoria = "Extintores",
                precio = 45000.0,
                stock = 15,
                stockMinimo = 5,
                descripcion = "Extintor de polvo químico seco para fuegos ABC",
                fechaIngreso = fechaHoy
            ),
            ProductoUI(
                id = 2,
                nombre = "Extintor CO2 5kg",
                categoria = "Extintores",
                precio = 65000.0,
                stock = 8,
                stockMinimo = 3,
                descripcion = "Extintor de dióxido de carbono para fuegos BC",
                fechaIngreso = fechaHoy
            ),
            ProductoUI(
                id = 3,
                nombre = "Detector de Humo",
                categoria = "Detectores",
                precio = 25000.0,
                stock = 25,
                stockMinimo = 10,
                descripcion = "Detector de humo fotoeléctrico",
                fechaIngreso = fechaHoy
            ),
            ProductoUI(
                id = 4,
                nombre = "Manguera Contraincendios",
                categoria = "Accesorios",
                precio = 120000.0,
                stock = 5,
                stockMinimo = 2,
                descripcion = "Manguera de 25m para sistemas contraincendios",
                fechaIngreso = fechaHoy
            ),
            ProductoUI(
                id = 5,
                nombre = "Señalética Salida Emergencia",
                categoria = "Señalización",
                precio = 8000.0,
                stock = 50,
                stockMinimo = 20,
                descripcion = "Señal luminosa de salida de emergencia",
                fechaIngreso = fechaHoy
            ),
            ProductoUI(
                id = 6,
                nombre = "Extintor H2O 9L",
                categoria = "Extintores",
                precio = 35000.0,
                stock = 2,
                stockMinimo = 5,
                descripcion = "Extintor de agua para fuegos tipo A",
                fechaIngreso = fechaHoy
            )
        )

        _productos.value = productosDemo
        _error.value = null
        println("✅ Datos demo cargados: ${productosDemo.size} productos")
    }

    /**
     * Crear un nuevo producto
     */
    suspend fun crearProducto(productoRequest: ProductoRequest): Boolean {
        return try {
            _isLoading.value = true
            _error.value = null

            val nuevoProducto = apiService.crearProducto(productoRequest)

            if (nuevoProducto != null) {
                // Recargar la lista para mostrar el nuevo producto
                cargarProductos()
                println("✅ Producto creado exitosamente: ${nuevoProducto.nombre}")
                true
            } else {
                _error.value = "Error al crear el producto"
                false
            }

        } catch (e: Exception) {
            _error.value = "Error al crear producto: ${e.message}"
            println("❌ Error al crear producto: ${e.message}")
            false
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Actualizar un producto existente
     */
    suspend fun actualizarProducto(id: Int, productoRequest: ProductoRequest): Boolean {
        return try {
            _isLoading.value = true
            _error.value = null

            val productoActualizado = apiService.actualizarProducto(id, productoRequest)

            if (productoActualizado != null) {
                // Recargar la lista para mostrar los cambios
                cargarProductos()
                println("✅ Producto actualizado exitosamente: ${productoActualizado.nombre}")
                true
            } else {
                _error.value = "Error al actualizar el producto"
                false
            }

        } catch (e: Exception) {
            _error.value = "Error al actualizar producto: ${e.message}"
            println("❌ Error al actualizar producto: ${e.message}")
            false
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Actualizar solo el stock de un producto
     */
    suspend fun actualizarStock(id: Int, cantidad: Int): Boolean {
        return try {
            _isLoading.value = true
            _error.value = null

            val productoActualizado = apiService.actualizarStock(id, cantidad)

            if (productoActualizado != null) {
                // Actualizar solo el producto específico en la lista local
                val productosActuales = _productos.value.toMutableList()
                val indice = productosActuales.indexOfFirst { it.id == id }

                if (indice != -1) {
                    productosActuales[indice] = productoActualizado.toUI()
                    _productos.value = productosActuales
                }

                println("✅ Stock actualizado exitosamente para producto ID: $id")
                true
            } else {
                _error.value = "Error al actualizar el stock"
                false
            }

        } catch (e: Exception) {
            _error.value = "Error al actualizar stock: ${e.message}"
            println("❌ Error al actualizar stock: ${e.message}")
            false
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Eliminar un producto
     */
    suspend fun eliminarProducto(id: Int): Boolean {
        return try {
            _isLoading.value = true
            _error.value = null

            val eliminado = apiService.eliminarProducto(id)

            if (eliminado) {
                // Eliminar el producto de la lista local
                val productosActuales = _productos.value.toMutableList()
                productosActuales.removeAll { it.id == id }
                _productos.value = productosActuales

                println("✅ Producto eliminado exitosamente ID: $id")
                true
            } else {
                _error.value = "Error al eliminar el producto"
                false
            }

        } catch (e: Exception) {
            _error.value = "Error al eliminar producto: ${e.message}"
            println("❌ Error al eliminar producto: ${e.message}")
            false
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Limpiar errores
     */
    fun limpiarError() {
        _error.value = null
    }

    /**
     * Liberar recursos
     */
    fun close() {
        apiService.close()
    }

    /**
     * Establecer productos demo directamente (para modo offline)
     */
    fun setProductosDemo(productosDemo: List<ProductoUI>) {
        println("🔄 Estableciendo ${productosDemo.size} productos demo en repositorio...")
        _productos.value = productosDemo
        _error.value = null
        _isLoading.value = false
        println("✅ Productos demo establecidos correctamente")
    }
}
