package org.example.project.services

import org.jetbrains.exposed.sql.transactions.transaction
import org.example.project.models.*
import kotlinx.datetime.*
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.and // Added import for 'and' operator
import java.security.SecureRandom
import kotlin.math.roundToLong
import org.example.project.security.PasswordUtils
import org.example.project.security.UserRole
import org.example.project.DEFAULT_INTEGRATION_TOKEN
import org.example.project.DEFAULT_INTEGRATION_SCOPES

object DatabaseSeeder {

    fun seedDatabase() {
        transaction {
            seedUsuarios() // ahora garantiza admin/user base aunque ya existan

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

        seedAlertas()

        seedIntegraciones()

        println("✅ Base de datos poblada con éxito")
        }
    }

    private fun seedUsuarios() {
        val ahora = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        ensureUsuario(
            email = "admin@extingrafic.com",
            password = "Admin123!",
            nombre = "Admin",
            apellido = "ExtinGrafic",
            rol = UserRole.ADMIN,
            ahora = ahora
        )
        ensureUsuario(
            email = "prueba@gmail.com",
            password = "123456",
            nombre = "diego",
            apellido = "pozas",
            rol = UserRole.USER,
            ahora = ahora
        )
        println("✅ Usuarios base garantizados (admin y usuario estándar)")
    }

    private fun ensureUsuario(
        email: String,
        password: String,
        nombre: String,
        apellido: String,
        rol: UserRole,
        ahora: LocalDateTime
    ) {
        val salt = PasswordUtils.generateSalt()
        val hash = PasswordUtils.hashPassword(password, salt)
        val existente = Usuario.find { Usuarios.email eq email.lowercase() }.firstOrNull()
        if (existente == null) {
            Usuario.new {
                this.nombre = nombre
                this.apellido = apellido
                this.email = email.lowercase()
                this.password = hash
                this.rol = rol.name
                this.activo = true
                this.fechaCreacion = ahora
                this.fechaUltimoAcceso = ahora
                this.intentosFallidos = 0
                this.bloqueadoHasta = null
            }
        } else {
            existente.password = hash
            existente.rol = rol.name
            existente.activo = true
            existente.intentosFallidos = 0
            existente.bloqueadoHasta = null
            existente.fechaUltimoAcceso = ahora
        }
    }

    private fun seedProveedores() {
        val ahora = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        val proveedores = listOf(
            Triple("SeguriPro Ltda", "Carolina Soto", "contacto@seguripro.cl"),
            Triple("Andes Equipamientos", "Ricardo Muñoz", "ventas@andes-equip.cl"),
            Triple("Distribuidora Norte", "María Ignacia Peña", "mpeña@distnorte.cl"),
            Triple("Repuestos Austral", "Hernán Valdés", "hvaldes@austral.cl")
        )

        proveedores.forEach { (nombre, contacto, email) ->
            if (Proveedor.find { Proveedores.nombre eq nombre }.empty()) {
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
        }
        println("✅ Proveedores realistas creados/validados")
    }

    private fun seedProductos() {
        val ahora = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        val proveedores = Proveedor.all().toList()

        data class Prod(
            val codigo: String,
            val nombre: String,
            val descripcion: String,
            val precio: Long,
            val stock: Int,
            val stockMin: Int,
            val categoria: String
        )
        val productosEjemplo = listOf(
            Prod("PRD-001", "Extintor PQS 4kg", "Extintor de polvo químico seco 4kg certificado", 24500L, 34, 6, "Extintores"),
            Prod("PRD-002", "Extintor PQS 6kg", "Extintor PQS ABC 6kg con manómetro metálico", 32500L, 22, 6, "Extintores"),
            Prod("PRD-003", "Extintor CO2 5kg", "Extintor de dióxido de carbono 5kg para tableros", 76500L, 12, 3, "Extintores"),
            Prod("PRD-004", "Extintor Agua Presurizada 9L", "Para riesgos clase A, incluye sello de seguridad", 28500L, 18, 4, "Extintores"),
            Prod("PRD-005", "Kit Señalética Fotoluminiscente", "Set de 10 señales estándar NCh", 18500L, 40, 10, "Señalética"),
            Prod("PRD-006", "Gabinete Metálico Rojo", "Gabinete para extintor 6-10kg con visor", 41500L, 9, 2, "Gabinetes"),
            Prod("PRD-007", "Carro porta extintor", "Carro reforzado para extintores 10-15kg", 58900L, 5, 1, "Accesorios"),
            Prod("PRD-008", "Carga PQS a Granel", "Bolsa de 25kg de polvo ABC", 52000L, 7, 2, "Recargas"),
            Prod("PRD-009", "Válvula de latón 3/4\"", "Válvula de repuesto para extintores PQS", 8900L, 25, 5, "Repuestos"),
            Prod("PRD-010", "Manómetro 250 psi", "Manómetro rosca fina para recargas", 6900L, 30, 6, "Repuestos"),
            Prod("PRD-011", "Manguera alta presión 1.5m", "Manguera con abrazaderas para extintor 6kg", 12900L, 18, 4, "Repuestos")
        )

        productosEjemplo.forEach { p ->
            val existente = Producto.find { Productos.codigo eq p.codigo }.firstOrNull()
            if (existente == null) {
                Producto.new {
                    this.nombre = p.nombre
                    this.codigo = p.codigo
                    this.descripcion = p.descripcion
                    this.precio = p.precio
                    this.precioCompra = (p.precio * 0.7).toLong()
                    this.cantidad = p.stock
                    this.categoria = p.categoria
                    this.estado = EstadoProducto.ACTIVO
                    this.stockMinimo = p.stockMin
                    this.proveedor = proveedores.randomOrNull()
                    this.fechaCreacion = ahora
                    this.fechaActualizacion = ahora
                }
            }
        }

        println("✅ Catálogo inicial poblado con 8 productos realistas")
    }

    private fun seedClientesSedes() {
        val ahora = Clock.System.now().toLocalDateTime(TimeZone.UTC)

        data class SedeSeed(val nombre: String, val direccion: String, val comuna: String)
        data class ClienteSeed(val nombre: String, val rut: String, val sedes: List<SedeSeed>)

        val clientes = listOf(
            ClienteSeed(
                nombre = "Constructora Andina",
                rut = "76.123.456-7",
                sedes = listOf(
                    SedeSeed("Oficina Central", "Av. Apoquindo 4500", "Las Condes"),
                    SedeSeed("Obra Quilicura", "Camino Lo Echevers 1020", "Quilicura")
                )
            ),
            ClienteSeed(
                nombre = "Clínica Santa Laura",
                rut = "78.987.654-3",
                sedes = listOf(
                    SedeSeed("Casa Matriz", "Av. Grecia 1231", "Ñuñoa"),
                    SedeSeed("Sucursal Norte", "Av. Recoleta 5321", "Recoleta")
                )
            ),
            ClienteSeed(
                nombre = "Alimentos Patagonia",
                rut = "65.432.198-1",
                sedes = listOf(
                    SedeSeed("Planta Principal", "Ruta 5 Sur Km 510", "Los Ángeles"),
                    SedeSeed("Centro Distribución Maipú", "Camino a Melipilla 9000", "Maipú")
                )
            )
        )

        clientes.forEach { c ->
            val cliente = Cliente.findByRut(c.rut) ?: Cliente.new {
                nombre = c.nombre
                rut = c.rut
                activo = true
                fechaCreacion = ahora
                fechaActualizacion = ahora
            }
            c.sedes.forEach { s ->
                val existe = Sede.find { (Sedes.clienteId eq cliente.id) and (Sedes.nombre eq s.nombre) }.firstOrNull()
                if (existe == null) {
                    Sede.new {
                        clienteId = cliente.id
                        nombre = s.nombre
                        direccion = s.direccion
                        comuna = s.comuna
                        fechaCreacion = ahora
                        fechaActualizacion = ahora
                    }
                }
            }
        }
        println("✅ Clientes y sedes poblados con datos más realistas")
    }

    private fun seedExtintores() {
        if (Extintor.all().count() > 0) {
            println("✅ Extintores ya existen, omitiendo creación")
            return
        }
        val ahora = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        data class ExtSeed(
            val codigo: String,
            val rutCliente: String,
            val sede: String,
            val tipo: String,
            val agente: String,
            val capacidad: String,
            val mesesVenc: Int,
            val mesesUltRecarga: Int
        )
        val seeds = listOf(
            ExtSeed("EXT-001", "76.123.456-7", "Oficina Central", "PQS", "ABC", "4kg", 11, -2),
            ExtSeed("EXT-002", "76.123.456-7", "Obra Quilicura", "CO2", "BC", "5kg", 5, -1),
            ExtSeed("EXT-003", "78.987.654-3", "Casa Matriz", "PQS", "ABC", "6kg", 9, -3),
            ExtSeed("EXT-004", "78.987.654-3", "Sucursal Norte", "Agua", "A", "9L", 3, -6),
            ExtSeed("EXT-005", "65.432.198-1", "Planta Principal", "PQS", "ABC", "10kg", 12, 0),
            ExtSeed("EXT-006", "65.432.198-1", "Centro Distribución Maipú", "PQS", "ABC", "6kg", 4, -2),
            ExtSeed("EXT-007", "76.123.456-7", "Obra Quilicura", "CO2", "BC", "10kg", 2, -5)
        )

        seeds.forEach { s ->
            val cliente = Cliente.findByRut(s.rutCliente) ?: return@forEach
            val sede = Sede.find { (Sedes.clienteId eq cliente.id) and (Sedes.nombre eq s.sede) }.firstOrNull() ?: return@forEach
            if (Extintor.findByCodigoQr(s.codigo) == null) {
                val fechaBase = ahora.date
                Extintor.new {
                    codigoQr = s.codigo
                    clienteId = cliente.id
                    sedeId = sede.id
                    tipo = s.tipo
                    agente = s.agente
                    capacidad = s.capacidad
                    fechaFabricacion = fechaBase.minus(DatePeriod(years = 1)).atStartOfDayIn(TimeZone.UTC).toLocalDateTime(TimeZone.UTC)
                    fechaUltimaRecarga = fechaBase.plus(DatePeriod(months = s.mesesUltRecarga)).atStartOfDayIn(TimeZone.UTC).toLocalDateTime(TimeZone.UTC)
                    fechaProximoVencimiento = fechaBase.plus(DatePeriod(months = s.mesesVenc)).atStartOfDayIn(TimeZone.UTC).toLocalDateTime(TimeZone.UTC)
                    estado = EstadoExtintor.VIGENTE
                    fechaCreacion = ahora
                    fechaActualizacion = ahora
                }
            }
        }
        println("✅ Extintores creados con ubicaciones reales")
    }

    private fun seedOrdenesServicio() {
        if (OrdenServicio.all().count() > 0) {
            println("✅ Órdenes ya existen, omitiendo creación")
            return
        }

        val ahora = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        val user = Usuario.findByEmail("prueba@gmail.com") ?: return

        val ordenesSeed = listOf(
            Triple("EXT-001", 5, EstadoOrdenServicio.PLANIFICADA),
            Triple("EXT-003", 12, EstadoOrdenServicio.EN_PROGRESO),
            Triple("EXT-004", 20, EstadoOrdenServicio.PLANIFICADA),
            Triple("EXT-006", 7, EstadoOrdenServicio.PLANIFICADA)
        )

        ordenesSeed.forEach { (codigoExt, dias, estado) ->
            val ext = Extintor.findByCodigoQr(codigoExt) ?: return@forEach
            val cliente = Cliente.findById(ext.clienteId) ?: return@forEach
            val sede = Sede.findById(ext.sedeId!!) ?: return@forEach
            val orden = OrdenServicio.new {
                fechaProgramada = ahora.date.plus(DatePeriod(days = dias)).atStartOfDayIn(TimeZone.UTC).toLocalDateTime(TimeZone.UTC)
                this.estado = estado
                tecnicoId = user.id
                clienteId = cliente.id
                sedeId = sede.id
                creadoPor = user.id
                fechaCreacion = ahora
                fechaActualizacion = ahora
            }
            OrdenServicioExtintores.insert {
                it[ordenId] = orden.id
                it[extintorId] = ext.id
            }
        }
        println("✅ Órdenes de servicio planificadas con extintores asignados")
    }

    private fun seedMovimientosInventario() {
        val ahora = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        val usuario = Usuario.findByEmail("prueba@gmail.com")
        Producto.all().forEach { producto ->
            // Entrada inicial
            MovimientoInventario.new {
                this.producto = producto
                this.tipo = TipoMovimientoInventario.ENTRADA
                this.cantidad = 10
                this.motivo = "Carga inicial"
                this.documento = "FAC-${producto.codigo}"
                this.proveedor = producto.proveedor
                this.usuario = usuario
                this.observaciones = "Ingreso bodega central"
                this.fechaRegistro = ahora.date.minus(DatePeriod(days = 7)).atStartOfDayIn(TimeZone.UTC).toLocalDateTime(TimeZone.UTC)
                this.estadoAprobacion = EstadoAprobacionMovimiento.APROBADO
                this.requiereAprobacion = false
            }
            // Salida reciente
            MovimientoInventario.new {
                this.producto = producto
                this.tipo = TipoMovimientoInventario.SALIDA
                this.cantidad = 2
                this.motivo = "Venta mostrador"
                this.documento = "SAL-${producto.codigo}"
                this.proveedor = null
                this.usuario = usuario
                this.observaciones = "Salida para cliente recurrente"
                this.fechaRegistro = ahora.date.minus(DatePeriod(days = 2)).atStartOfDayIn(TimeZone.UTC).toLocalDateTime(TimeZone.UTC)
                this.estadoAprobacion = EstadoAprobacionMovimiento.APROBADO
                this.requiereAprobacion = false
            }
            // Ajuste por inventario crítico
            MovimientoInventario.new {
                this.producto = producto
                this.tipo = TipoMovimientoInventario.ENTRADA
                this.cantidad = 1
                this.motivo = "Ajuste inventario"
                this.documento = "AJU-${producto.codigo}"
                this.proveedor = producto.proveedor
                this.usuario = usuario
                this.observaciones = "Reposición mínima para stock crítico"
                this.fechaRegistro = ahora.date.minus(DatePeriod(days = 1)).atStartOfDayIn(TimeZone.UTC).toLocalDateTime(TimeZone.UTC)
                this.estadoAprobacion = EstadoAprobacionMovimiento.APROBADO
                this.requiereAprobacion = false
            }
        }
        println("✅ Movimientos de inventario con entradas y salidas recientes")
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
            "Camila Reyes", "Javier Muñoz", "Sofía Torres", "Diego Silva", "Valentina Pérez",
            "Mateo Rojas", "Antonia Campos", "Lucas Herrera", "Isidora Fuentes", "Felipe Carrasco",
            "Alejandro Bravo", "Paula Vega"
        )
        val metodosPago = MetodoPago.values()
        val estados = listOf(
            EstadoVenta.COMPLETADA,
            EstadoVenta.COMPLETADA,
            EstadoVenta.PENDIENTE
        )

        var correlativo = 0
        repeat(15) { i ->
            val cliente = clientesEjemplo[i % clientesEjemplo.size]
            val metodoPago = metodosPago.random()
            val estado = estados.random()
            val fechaVenta = ahora.date.minus(DatePeriod(days = i)).atStartOfDayIn(TimeZone.UTC).toLocalDateTime(TimeZone.UTC)
            val productosVenta = productos.shuffled().take((1..3).random())
            var subtotalVenta = 0L
            var impuestosVenta = 0L
            var totalVenta = 0L
            correlativo += 1
            val numero = "V${correlativo.toString().padStart(5, '0')}"

            val venta = Venta.new {
                this.numero = numero
                this.cliente = cliente
                this.clienteRut = "76.${(10..99).random()}${(100..999).random()}-${(0..9).random()}"
                this.clienteTelefono = "+56 9 ${(SecureRandom().nextInt(90000000) + 10000000)}"
                this.clienteEmail = "${cliente.lowercase().replace(" ", ".")}@mail.com"
                this.fecha = fechaVenta
                this.subtotal = 0L
                this.impuestos = 0L
                this.total = 0L
                this.descuento = listOf(0L, 0L, 1500L).random()
                this.estado = estado
                this.metodoPago = metodoPago
                this.vendedor = usuario
                this.fechaCreacion = fechaVenta
                this.fechaActualizacion = fechaVenta
            }

            productosVenta.forEach { producto ->
                val cantidad = (1..3).random()
                val precioUnitario = producto.precio
                val base = precioUnitario * cantidad - venta.descuento
                val ivaMonto = (base * 0.19).roundToLong()
                val subtotal = base + ivaMonto

                VentaProducto.new {
                    this.ventaId = venta.id
                    this.productoId = producto.id
                    this.cantidad = cantidad
                    this.precio = precioUnitario
                    this.subtotal = subtotal
                    this.descuento = venta.descuento
                    this.iva = 19.0
                    this.devuelto = 0
                }
                subtotalVenta += (precioUnitario * cantidad - venta.descuento)
                impuestosVenta += ivaMonto
                totalVenta = subtotalVenta + impuestosVenta
                if (estado == EstadoVenta.COMPLETADA) {
                    producto.cantidad = maxOf(0, producto.cantidad - cantidad)
                    producto.fechaActualizacion = ahora
                }
            }
            venta.subtotal = subtotalVenta
            venta.impuestos = impuestosVenta
            venta.total = totalVenta
        }
        println("✅ 12 ventas con mezcla de estados y días anteriores creadas")
    }

    private fun seedAlertas() {
        if (Alerta.all().count() > 0) return
        val ahora = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        val extList = Extintor.all().limit(3).toList()
        extList.forEachIndexed { idx, ext ->
            Alerta.new {
                extintorId = ext.id
                productoId = null
                tipo = if (idx % 2 == 0) "VENCIMIENTO_EXTINTOR" else "SERVICIO_PENDIENTE"
                fechaGenerada = ahora.date.minus(DatePeriod(days = idx + 1)).atStartOfDayIn(TimeZone.UTC).toLocalDateTime(TimeZone.UTC)
                enviada = false
                fechaEnvio = null
                reintentos = 0
            }
        }
        println("✅ Alertas creadas para vencimientos/servicios")
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

    private fun seedIntegraciones() {
        val ahora = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        val existente = Integracion.find { Integraciones.token eq DEFAULT_INTEGRATION_TOKEN }.firstOrNull()
        if (existente == null) {
            Integracion.new {
                nombre = "Integración demo (API key)"
                token = DEFAULT_INTEGRATION_TOKEN
                scopes = DEFAULT_INTEGRATION_SCOPES
                activo = true
                creadoEn = ahora
                creadoPor = "seed"
                ultimoUso = null
                hits = 0
            }
            println("✅ Token de integración creado: $DEFAULT_INTEGRATION_TOKEN (scopes=$DEFAULT_INTEGRATION_SCOPES)")
        } else {
            if (!existente.activo) existente.activo = true
            if (existente.scopes != DEFAULT_INTEGRATION_SCOPES) existente.scopes = DEFAULT_INTEGRATION_SCOPES
            println("✅ Token de integración existente habilitado: ${existente.token}")
        }
    }
}
