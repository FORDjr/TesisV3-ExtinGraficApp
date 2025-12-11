package org.example.project.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.example.project.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.SQLException
import org.example.project.models.*          // tus tablas
import org.example.project.services.DatabaseSeeder
import org.example.project.models.Clientes
import org.example.project.models.Sedes
import org.example.project.models.Extintores
import org.example.project.models.OrdenesServicio
import org.example.project.models.OrdenServicioExtintores
import org.jetbrains.exposed.sql.transactions.TransactionManager

object DatabaseConfig {

    fun init() {
        /* ---------- Hikari ---------- */
        val hConfig = HikariConfig().apply {
            if (USE_H2) {
                jdbcUrl = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL"
                driverClassName = "org.h2.Driver"
                username = "sa"
                password = ""
            } else {
                jdbcUrl = "jdbc:postgresql://$DB_HOST:$DB_PORT/$DB_NAME"
                username = DB_USER
                password = DB_PASS
                driverClassName = "org.postgresql.Driver"
            }

            maximumPoolSize = 10
            minimumIdle = 2
            connectionTimeout = CONNECTION_TIMEOUT

            /* extras */
            addDataSourceProperty("sslmode", "disable")
            addDataSourceProperty("tcpKeepAlive", "true")
        }

        val dataSource = HikariDataSource(hConfig)
        Database.connect(dataSource)

        /* ---------- DDL + datos de ejemplo ---------- */
        transaction {
            SchemaUtils.createMissingTablesAndColumns(
                Proveedores,
                Productos,
                Usuarios,
                Ventas,
                VentaProductos,
                Clientes,
                Sedes,
                Extintores,
                OrdenesServicio,
                OrdenServicioExtintores,
                Certificados,
                ServiceRegistros,
                ServiceRegistroProductos,
                Alertas,
                MovimientosInventario,
                Integraciones
            )
            // Intentar agregar columna stock_minimo si no existe (PostgreSQL)
            try {
                TransactionManager.current().exec(
                    "ALTER TABLE productos ADD COLUMN IF NOT EXISTS stock_minimo INTEGER DEFAULT 0"
                )
            } catch (e: Exception) {
                println("[DB][WARN] No se pudo agregar stock_minimo: ${e.message}")
            }
            try {
                TransactionManager.current().exec("ALTER TABLE productos ADD COLUMN IF NOT EXISTS codigo VARCHAR(80)")
                TransactionManager.current().exec("CREATE UNIQUE INDEX IF NOT EXISTS productos_codigo_key ON productos(codigo)")
                TransactionManager.current().exec("ALTER TABLE productos ADD COLUMN IF NOT EXISTS precio_compra BIGINT DEFAULT 0")
                TransactionManager.current().exec("ALTER TABLE productos ADD COLUMN IF NOT EXISTS estado VARCHAR(30) DEFAULT 'ACTIVO'")
                TransactionManager.current().exec("ALTER TABLE productos ADD COLUMN IF NOT EXISTS proveedor_id INTEGER REFERENCES proveedores(id)")
                TransactionManager.current().exec(
                    """
                    UPDATE productos
                    SET codigo = CONCAT('PRD-', LPAD(id::text, 4, '0'))
                    WHERE codigo IS NULL OR codigo = ''
                    """.trimIndent()
                )
            } catch (e: Exception) {
                println("[DB][WARN] No se pudieron agregar columnas de productos: ${e.message}")
            }
            try {
                TransactionManager.current().exec("ALTER TABLE ventas ADD COLUMN IF NOT EXISTS numero VARCHAR(30)")
                TransactionManager.current().exec("CREATE UNIQUE INDEX IF NOT EXISTS ventas_numero_key ON ventas(numero)")
                TransactionManager.current().exec("ALTER TABLE ventas ADD COLUMN IF NOT EXISTS descuento BIGINT DEFAULT 0")
                TransactionManager.current().exec("ALTER TABLE ventas ADD COLUMN IF NOT EXISTS vendedor_id INTEGER REFERENCES usuarios(id)")
                TransactionManager.current().exec(
                    """
                    UPDATE ventas
                    SET numero = CONCAT('V', LPAD(id::text, 5, '0'))
                    WHERE numero IS NULL OR numero = ''
                    """.trimIndent()
                )
            } catch (e: Exception) {
                println("[DB][WARN] No se pudieron agregar columnas de ventas: ${e.message}")
            }
            try {
                TransactionManager.current().exec("ALTER TABLE ventas ADD COLUMN IF NOT EXISTS cliente_rut VARCHAR(50)")
                TransactionManager.current().exec("ALTER TABLE ventas ADD COLUMN IF NOT EXISTS cliente_direccion VARCHAR(255)")
                TransactionManager.current().exec("ALTER TABLE ventas ADD COLUMN IF NOT EXISTS cliente_telefono VARCHAR(80)")
                TransactionManager.current().exec("ALTER TABLE ventas ADD COLUMN IF NOT EXISTS cliente_email VARCHAR(120)")
                TransactionManager.current().exec("ALTER TABLE ventas ADD COLUMN IF NOT EXISTS subtotal BIGINT DEFAULT 0")
                TransactionManager.current().exec("ALTER TABLE ventas ADD COLUMN IF NOT EXISTS impuestos BIGINT DEFAULT 0")
            } catch (e: Exception) {
                println("[DB][WARN] No se pudieron agregar columnas de cliente/impuestos en ventas: ${e.message}")
            }
            try {
                TransactionManager.current().exec("ALTER TABLE venta_productos ADD COLUMN IF NOT EXISTS descuento BIGINT DEFAULT 0")
                TransactionManager.current().exec("ALTER TABLE venta_productos ADD COLUMN IF NOT EXISTS iva DOUBLE PRECISION DEFAULT 0")
                TransactionManager.current().exec("ALTER TABLE venta_productos ADD COLUMN IF NOT EXISTS devuelto INTEGER DEFAULT 0")
            } catch (e: Exception) {
                println("[DB][WARN] No se pudieron agregar columnas de descuento/iva/devuelto en venta_productos: ${e.message}")
            }
            try {
                TransactionManager.current().exec("ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS intentos_fallidos INTEGER DEFAULT 0")
                TransactionManager.current().exec("ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS bloqueado_hasta TIMESTAMP")
                TransactionManager.current().exec("ALTER TABLE usuarios ALTER COLUMN rol SET DEFAULT 'USER'")
                TransactionManager.current().exec("UPDATE usuarios SET rol = UPPER(rol)")
            } catch (e: Exception) {
                println("[DB][WARN] No se pudieron agregar columnas de usuarios: ${e.message}")
            }
            try {
                TransactionManager.current().exec("ALTER TABLE extintores ADD COLUMN IF NOT EXISTS estado VARCHAR(20) DEFAULT 'VIGENTE'")
            } catch (e: Exception) {
                println("[DB][WARN] No se pudo agregar columna estado en extintores: ${e.message}")
            }
            try {
                TransactionManager.current().exec("ALTER TABLE extintores ADD COLUMN IF NOT EXISTS ubicacion VARCHAR(255)")
                TransactionManager.current().exec("ALTER TABLE extintores ADD COLUMN IF NOT EXISTS estado_logistico VARCHAR(30) DEFAULT 'DISPONIBLE'")
            } catch (e: Exception) {
                println("[DB][WARN] No se pudieron agregar columnas logisticas en extintores: ${e.message}")
            }
            try {
                TransactionManager.current().exec("ALTER TABLE movimientos_inventario ADD COLUMN IF NOT EXISTS estado_aprobacion VARCHAR(20) DEFAULT 'APROBADO'")
                TransactionManager.current().exec("ALTER TABLE movimientos_inventario ADD COLUMN IF NOT EXISTS requiere_aprobacion BOOLEAN DEFAULT FALSE")
                TransactionManager.current().exec("ALTER TABLE movimientos_inventario ADD COLUMN IF NOT EXISTS aprobado_por INTEGER REFERENCES usuarios(id)")
                TransactionManager.current().exec("ALTER TABLE movimientos_inventario ADD COLUMN IF NOT EXISTS fecha_aprobacion TIMESTAMP")
                TransactionManager.current().exec("ALTER TABLE movimientos_inventario ADD COLUMN IF NOT EXISTS idempotencia_key VARCHAR(80)")
                TransactionManager.current().exec("CREATE UNIQUE INDEX IF NOT EXISTS idx_movimientos_idempotencia ON movimientos_inventario(idempotencia_key)")
            } catch (e: Exception) {
                println("[DB][WARN] No se pudieron agregar columnas de aprobación en movimientos: ${e.message}")
            }
            try {
                TransactionManager.current().exec("CREATE INDEX IF NOT EXISTS idx_extintores_vencimiento ON extintores(fecha_proximo_vencimiento)")
            } catch (e: Exception) {
                println("[DB][WARN] Índice no creado: ${e.message}")
            }
            try {
                TransactionManager.current().exec("ALTER TABLE alertas ALTER COLUMN extintor_id DROP NOT NULL")
            } catch (_: Exception) {}
            try {
                TransactionManager.current().exec("ALTER TABLE alertas ADD COLUMN IF NOT EXISTS producto_id INTEGER REFERENCES productos(id)")
            } catch (_: Exception) {}
            try {
                TransactionManager.current().exec("CREATE INDEX IF NOT EXISTS idx_alertas_tipo_enviada ON alertas(tipo,enviada)")
            } catch (_: Exception) {}
            println("🗄️  Inicializando datos de ejemplo…")
            DatabaseSeeder.seedDatabase()
        }
    }
}
