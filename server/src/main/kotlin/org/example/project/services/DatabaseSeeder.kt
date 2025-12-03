package org.example.project.services

import org.jetbrains.exposed.sql.transactions.transaction
import org.example.project.models.*
import kotlinx.datetime.*
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.and // Added import for 'and' operator
import java.security.SecureRandom
import javax.crypto.spec.PBEKeySpec
import javax.crypto.SecretKeyFactory

object DatabaseSeeder {

    private fun hashPassword(password: String, salt: ByteArray): String {
        val spec = PBEKeySpec(password.toCharArray(), salt, 100000, 256)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val hash = factory.generateSecret(spec).encoded
        return "${salt.joinToString("") { "%02x".format(it) }}:${hash.joinToString("") { "%02x".format(it) }}"
    }

    private fun generateSalt(): ByteArray {
        val salt = ByteArray(32)
        SecureRandom().nextBytes(salt)
        return salt
    }

    fun seedDatabase() {
        transaction {
            if (Usuario.all().count() > 0) {
                println("✅ Base de datos ya tiene usuarios, omitiendo seed de usuarios")
            } else {
                seedUsuarios()
            }

            if (Proveedor.all().count() > 0) {
                println("✅ Proveedores ya existen, omitiendo seed de proveedores")
            } else {
                seedProveedores()
            }

            if (Producto.all().count() > 0) {
                println("✅ Base de datos ya tiene productos, omitiendo seed de productos")
            } else {
                seedProductos()
            }

            if (Cliente.all().count() > 0) {
                println("✅ Base de datos ya tiene clientes, omitiendo seed de clientes")
            } else {
                println("🌱 Poblando base de datos con datos iniciales (si no existen)...")
                seedClientesSedes()
            }

            // Evitar crear extintores si ya existen
            if (Extintor.all().count() > 0) {
                println("✅ Base de datos ya tiene extintores, omitiendo seed de extintores")
            } else {
                seedExtintores()
            }

            // Órdenes de servicio y ventas también se crean solo si no existen
            if (OrdenServicio.all().count() > 0) {
                println("✅ Base de datos ya tiene órdenes de servicio, omitiendo seed de órdenes")
            } else {
                seedOrdenesServicio()
            }

            if (Venta.all().count() > 0) {
                println("✅ Base de datos ya tiene ventas, omitiendo seed de ventas")
            } else {
                seedVentas()
            }

            reemplazarClientesPlaceholderEnVentas()

            if (MovimientoInventario.all().count() > 0) {
                println("✅ Movimientos de inventario ya existen, omitiendo seed de movimientos")
            } else {
                seedMovimientosInventario()
            }

            println("✅ Base de datos poblada con éxito")
        }
    }

    private fun seedUsuarios() {
        val ahora = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        val salt = generateSalt()
        val hashedPassword = hashPassword("123456", salt)

        Usuario.new {
            this.nombre = "diego"
            this.apellido = "pozas"
            this.email = "prueba@gmail.com"
            this.password = hashedPassword
            this.rol = "user"
            this.activo = true
            this.fechaCreacion = ahora
            this.fechaUltimoAcceso = ahora
        }

        println("✅ Usuario 'diego pozas' creado")
    }

    private fun seedProveedores() {
        val ahora = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        val proveedores = listOf(
            Triple("Suministros ABC", "Juan Torres", "contacto@suministrosabc.cl"),
            Triple("Logística Pro", "Marcela Núñez", "ventas@logisticapro.cl")
        )

        proveedores.forEach { (nombre, contacto, email) ->
            Proveedor.new {
                this.nombre = nombre
                this.contacto = contacto
                this.email = email
                this.telefono = "+56 9 ${(SecureRandom().nextInt(90000000) + 10000000)}"
                this.activo = true
                this.fechaCreacion = ahora
                this.fechaActualizacion = ahora
            }
        }
        println("✅ Proveedores de ejemplo creados")
    }

    private fun seedProductos() {
        println("DEBUG: seedProductos() is being executed!") // Added debug log
        val ahora = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        val proveedores = Proveedor.all().toList()

        val productosEjemplo = listOf(
            Triple("Extintor PQS 4kg", "Extintor de Polvo Químico Seco ABC de 4kg", 25000L),
            Triple("Extintor PQS 6kg", "Extintor de Polvo Químico Seco ABC de 6kg", 35000L),
            Triple("Extintor CO2 5kg", "Extintor de Dióxido de Carbono de 5kg", 75000L),
            Triple("Gabinete para extintor", "Gabinete metálico para extintor de hasta 10kg", 40000L),
            Triple("Señalética de extintor", "Señalética fotoluminiscente para extintor", 5000L)
        )

        productosEjemplo.forEachIndexed { index, (nombre, descripcion, precio) ->
            Producto.new {
                this.nombre = nombre
                this.codigo = "PRD-${(index + 1).toString().padStart(3, '0')}"
                this.descripcion = descripcion
                this.precio = precio
                this.precioCompra = (precio * 0.7).toLong()
                this.cantidad = (20..100).random()
                this.categoria = "Seguridad contra incendios"
                this.estado = EstadoProducto.ACTIVO
                this.stockMinimo = 5
                this.proveedor = if (proveedores.isNotEmpty()) proveedores.random() else null
                this.fechaCreacion = ahora
                this.fechaActualizacion = ahora
            }
        }

        println("✅ Productos de ejemplo de extintores creados")
    }

    private fun seedClientesSedes() {
        val ahora = Clock.System.now().toLocalDateTime(TimeZone.UTC) // Defined 'ahora' here
        transaction {
            // Primero, verifica si el cliente ya existe
            val existingCliente = Cliente.find { Clientes.rut eq "76.123.456-7" }.firstOrNull()

            // Usa el cliente existente o crea uno nuevo si no existe
            val cliente = existingCliente ?: Cliente.new {
                nombre = "Empresa XYZ"
                rut = "76.123.456-7"
                activo = true
                fechaCreacion = ahora
                fechaActualizacion = ahora
            }

            // Solo crea la sede si no existe
            if (Sede.find { Sedes.clienteId eq cliente.id }.empty()) {
                Sede.new {
                    this.clienteId = cliente.id
                    nombre = "Casa Matriz"
                    direccion = "Av Principal 123"
                    comuna = "Santiago"
                    fechaCreacion = ahora
                    fechaActualizacion = ahora
                }
            }
        }

        transaction {
            // Cliente 2
            val rut2 = "78.987.654-3"
            val cliente2Existente = Cliente.find { Clientes.rut eq rut2 }.count()

            val cliente2 = if (cliente2Existente == 0L) {
                Cliente.new {
                    nombre = "Comercializadora ABC"
                    rut = rut2
                    activo = true
                    fechaCreacion = ahora
                    fechaActualizacion = ahora
                }
            } else {
                println("✅ Cliente con RUT $rut2 ya existe, omitiendo creación.")
                Cliente.findByRut(rut2)
            }
            checkNotNull(cliente2) { "Cliente 'Comercializadora ABC' no encontrado después de la verificación de existencia." }

            // Crear sede si no existe
            val bodegaPrincipal = Sede.find { (Sedes.clienteId eq cliente2.id) and (Sedes.nombre eq "Bodega Principal") }.firstOrNull()
            if (bodegaPrincipal == null) {
                Sede.new {
                    clienteId = cliente2.id
                    nombre = "Bodega Principal"
                    direccion = "Calle Falsa 456"
                    comuna = "Shelbyville"
                    fechaCreacion = ahora
                    fechaActualizacion = ahora
                }
            } else {
                println("✅ Sede 'Bodega Principal' para cliente ${cliente2.nombre} ya existe, omitiendo creación.")
            }

            println("✅ Clientes y sedes de ejemplo creados (o ya existentes)")
        }
    }

    private fun seedExtintores() {
        // Evitar creación si ya existen (comprobación ya hecha en seedDatabase pero se deja extra protección)
        if (Extintor.all().count() > 0) {
            println("✅ Extintores ya existen, omitiendo creación")
            return
        }

        val ahora = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        val cliente1 = Cliente.findByRut("76.123.456-7") ?: run {
            println("❌ No se encontró cliente para crear extintores, omitiendo")
            return
        }
        val sede1 = Sede.find { Sedes.nombre eq "Oficina Central" }.firstOrNull() ?: run {
            println("❌ No se encontró sede 'Oficina Central', omitiendo extintores")
            return
        }

        // Sólo crear si no hay extintores con ese código
        if (Extintor.findByCodigoQr("EXT-001") == null) {
            Extintor.new {
                codigoQr = "EXT-001"
                clienteId = cliente1.id
                sedeId = sede1.id
                tipo = "PQS"
                agente = "ABC"
                capacidad = "4kg"
                fechaFabricacion = ahora
                fechaUltimaRecarga = ahora
                fechaProximoVencimiento = ahora.date.plus(DatePeriod(years = 1)).atStartOfDayIn(TimeZone.UTC).toLocalDateTime(TimeZone.UTC)
                estado = EstadoExtintor.VIGENTE
                fechaCreacion = ahora
                fechaActualizacion = ahora
            }
        }
        if (Extintor.findByCodigoQr("EXT-002") == null) {
            Extintor.new {
                codigoQr = "EXT-002"
                clienteId = cliente1.id
                sedeId = sede1.id
                tipo = "CO2"
                agente = "BC"
                capacidad = "5kg"
                fechaFabricacion = ahora
                fechaUltimaRecarga = ahora
                fechaProximoVencimiento = ahora.date.plus(DatePeriod(months = 6)).atStartOfDayIn(TimeZone.UTC).toLocalDateTime(TimeZone.UTC)
                estado = EstadoExtintor.VIGENTE
                fechaCreacion = ahora
                fechaActualizacion = ahora
            }
        }
        println("✅ Extintores de ejemplo creados (o ya existentes)")
    }

    private fun seedOrdenesServicio() {
        if (OrdenServicio.all().count() > 0) {
            println("✅ Órdenes ya existen, omitiendo creación")
            return
        }

        val ahora = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        val user = Usuario.findByEmail("prueba@gmail.com") ?: run {
            println("❌ No se encontró usuario técnico, omitiendo ordenes de servicio")
            return
        }
        val cliente1 = Cliente.findByRut("76.123.456-7") ?: run {
            println("❌ No se encontró cliente para crear ordenes de servicio, omitiendo")
            return
        }
        val sede1 = Sede.find { Sedes.nombre eq "Oficina Central" }.firstOrNull() ?: run {
            println("❌ No se encontró sede 'Oficina Central', omitiendo ordenes de servicio")
            return
        }
        val extintor1 = Extintor.findByCodigoQr("EXT-001") ?: run {
            println("❌ No se encontró extintor EXT-001, omitiendo ordenes")
            return
        }

        val orden = OrdenServicio.new {
            fechaProgramada = ahora.date.plus(DatePeriod(days = 10)).atStartOfDayIn(TimeZone.UTC).toLocalDateTime(TimeZone.UTC)
            estado = EstadoOrdenServicio.PLANIFICADA
            tecnicoId = user.id
            clienteId = cliente1.id
            sedeId = sede1.id
            creadoPor = user.id
            fechaCreacion = ahora
            fechaActualizacion = ahora
        }

        OrdenServicioExtintores.insert {
            it[ordenId] = orden.id
            it[extintorId] = extintor1.id
        }
        println("✅ Órdenes de servicio de ejemplo creadas (o ya existentes)")
    }

    private fun seedMovimientosInventario() {
        val ahora = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        val usuario = Usuario.findByEmail("prueba@gmail.com")
        Producto.all().forEach { producto ->
            MovimientoInventario.new {
                this.producto = producto
                this.tipo = TipoMovimientoInventario.ENTRADA
                this.cantidad = 10
                this.motivo = "Carga inicial"
                this.documento = "FAC-${producto.codigo}"
                this.proveedor = producto.proveedor
                this.usuario = usuario
                this.observaciones = "Seed automático"
                this.fechaRegistro = ahora
                this.estadoAprobacion = EstadoAprobacionMovimiento.APROBADO
                this.requiereAprobacion = false
            }
        }
        println("✅ Movimientos de inventario iniciales creados")
    }

    private fun seedVentas() {
        val ahora = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        val productos = Producto.all().toList()
        val usuario = Usuario.findByEmail("prueba@gmail.com")

        if (productos.isEmpty()) {
            println("❌ No hay productos disponibles para crear ventas")
            return
        }

        val clientesEjemplo = listOf(
            "Camila Reyes",
            "Javier Muñoz",
            "Sofía Torres",
            "Diego Silva",
            "Valentina Pérez",
            "Mateo Rojas",
            "Antonia Campos",
            "Lucas Herrera"
        )
        val metodosPago = MetodoPago.values()
        val estados = listOf(EstadoVenta.COMPLETADA, EstadoVenta.PENDIENTE)

        var correlativo = Venta.all().count().toInt()

        repeat(5) { i ->
            val cliente = clientesEjemplo.random()
            val metodoPago = metodosPago.random()
            val estado = estados.random()
            val fechaVenta = ahora.date.minus(DatePeriod(days = i)).atStartOfDayIn(TimeZone.UTC).toLocalDateTime(TimeZone.UTC)
            val productosVenta = productos.shuffled().take((1..2).random())
            var totalVenta = 0L
            correlativo += 1
            val numero = "V${correlativo.toString().padStart(5, '0')}"

            val venta = Venta.new {
                this.numero = numero
                this.cliente = cliente
                this.fecha = fechaVenta
                this.total = 0L // Se calculará después
                this.estado = estado
                this.metodoPago = metodoPago
                this.vendedor = usuario
                this.fechaCreacion = fechaVenta
                this.fechaActualizacion = fechaVenta
            }

            productosVenta.forEach { producto ->
                val cantidad = (1..3).random()
                val precioUnitario = producto.precio
                val subtotal = precioUnitario * cantidad

                VentaProducto.new {
                    this.ventaId = venta.id
                    this.productoId = producto.id
                    this.cantidad = cantidad
                    this.precio = precioUnitario
                    this.subtotal = subtotal
                }
                totalVenta += subtotal
                if (estado == EstadoVenta.COMPLETADA) {
                    producto.cantidad = maxOf(0, producto.cantidad - cantidad)
                    producto.fechaActualizacion = ahora
                }
            }
            venta.total = totalVenta
        }
        println("✅ 5 ventas de ejemplo creadas")
    }

    /**
     * Reemplaza nombres de cliente de ventas de demo ("Cliente Final ...") por nombres reales
     */
    private fun reemplazarClientesPlaceholderEnVentas() {
        val placeholders = Venta.find { Ventas.cliente like "Cliente Final%" }.toList()
        if (placeholders.isEmpty()) return

        val nombres = listOf(
            "Camila Reyes",
            "Javier Muñoz",
            "Sofía Torres",
            "Diego Silva",
            "Valentina Pérez",
            "Mateo Rojas",
            "Antonia Campos",
            "Lucas Herrera",
            "Isidora Fuentes",
            "Felipe Carrasco"
        )

        val ahora = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        placeholders.forEachIndexed { idx, venta ->
            val nombreNuevo = nombres[idx % nombres.size]
            venta.cliente = nombreNuevo
            venta.fechaActualizacion = ahora
        }
        println("🔄 Ventas de demo actualizadas con nombres reales (${placeholders.size} registros)")
    }

    // Helpers para encontrar por campos únicos
    private fun Cliente.Companion.findByRut(rut: String) = find { Clientes.rut eq rut }.firstOrNull()
    private fun Usuario.Companion.findByEmail(email: String) = find { Usuarios.email eq email }.firstOrNull()
    private fun Extintor.Companion.findByCodigoQr(codigoQr: String) = find { Extintores.codigoQr eq codigoQr }.firstOrNull()
}
