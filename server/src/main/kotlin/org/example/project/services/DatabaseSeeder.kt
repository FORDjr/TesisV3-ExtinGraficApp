package org.example.project.services

import org.jetbrains.exposed.sql.transactions.transaction
import org.example.project.models.*
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.minus
import java.math.BigDecimal

object DatabaseSeeder {

    fun seedDatabase() {
        transaction {
            // Verificar si ya hay datos
            if (Venta.all().count() > 0) {
                println("✅ Base de datos ya tiene ventas, omitiendo seed")
                return@transaction
            }

            println("🌱 Poblando base de datos con datos iniciales...")

            // Verificar que hay productos en inventario
            val productos = Producto.all().toList()
            if (productos.isEmpty()) {
                println("⚠️ No hay productos en inventario, creando algunos productos de ejemplo...")
                seedProductos()
            }

            // Crear ventas de ejemplo
            seedVentas()

            println("✅ Base de datos poblada con éxito")
        }
    }

    private fun seedProductos() {
        val ahora = Clock.System.now().toLocalDateTime(TimeZone.UTC)

        val productosEjemplo = listOf(
            Triple("Laptop HP", "Laptop HP Pavilion 15.6\"", 899.99),
            Triple("Mouse Logitech", "Mouse inalámbrico Logitech MX Master", 79.99),
            Triple("Teclado Mecánico", "Teclado mecánico RGB", 129.99),
            Triple("Monitor Samsung", "Monitor Samsung 24\" Full HD", 199.99),
            Triple("Auriculares Sony", "Auriculares Sony WH-1000XM4", 299.99)
        )

        productosEjemplo.forEach { (nombre, descripcion, precio) ->
            Producto.new {
                this.nombre = nombre
                this.descripcion = descripcion
                this.precio = BigDecimal.valueOf(precio)
                this.cantidad = (10..50).random() // Stock aleatorio entre 10 y 50
                this.categoria = "Electrónicos"
                this.fechaCreacion = ahora
                this.fechaActualizacion = ahora
            }
        }

        println("✅ Productos de ejemplo creados")
    }

    private fun seedVentas() {
        val ahora = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        val productos = Producto.all().toList()

        if (productos.isEmpty()) {
            println("❌ No hay productos disponibles para crear ventas")
            return
        }

        val clientesEjemplo = listOf(
            "María García López",
            "Carlos Rodríguez Martín",
            "Ana Fernández Silva",
            "Luis González Pérez",
            "Elena Martínez Ruiz",
            "Miguel Sánchez Torres",
            "Carmen López Díaz",
            "José Antonio Vega"
        )

        val metodosPago = MetodoPago.values()
        val estados = listOf(EstadoVenta.COMPLETADA, EstadoVenta.PENDIENTE)

        // Crear 10 ventas de ejemplo
        repeat(10) { i ->
            val cliente = clientesEjemplo.random()
            val metodoPago = metodosPago.random()
            val estado = estados.random()

            // Calcular fecha (días anteriores)
            val fechaVenta = if (i == 0) {
                ahora
            } else {
                val fechaAnterior = ahora.date.minus(DatePeriod(days = i))
                kotlinx.datetime.LocalDateTime(fechaAnterior, ahora.time)
            }

            // Seleccionar 1-3 productos aleatorios para la venta
            val productosVenta = productos.shuffled().take((1..3).random())
            var totalVenta = BigDecimal.ZERO

            val venta = Venta.new {
                this.cliente = cliente
                this.fecha = fechaVenta
                this.total = BigDecimal.ZERO // Se calculará después
                this.estado = estado
                this.metodoPago = metodoPago
                this.observaciones = if (i % 3 == 0) "Entrega urgente" else null
                this.fechaCreacion = fechaVenta
                this.fechaActualizacion = fechaVenta
            }

            // Crear los productos de la venta
            productosVenta.forEach { producto ->
                val cantidad = (1..3).random()
                val precioUnitario = producto.precio
                val subtotal = precioUnitario * BigDecimal.valueOf(cantidad.toDouble())

                VentaProducto.new {
                    this.ventaId = venta.id
                    this.productoId = producto.id
                    this.cantidad = cantidad
                    this.precio = precioUnitario
                    this.subtotal = subtotal
                }

                totalVenta = totalVenta.add(subtotal)

                // Solo actualizar stock si la venta está completada
                if (estado == EstadoVenta.COMPLETADA) {
                    producto.cantidad = maxOf(0, producto.cantidad - cantidad)
                    producto.fechaActualizacion = ahora
                }
            }

            // Actualizar el total de la venta
            venta.total = totalVenta
        }

        println("✅ 10 ventas de ejemplo creadas")
    }
}
