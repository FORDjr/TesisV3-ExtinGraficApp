package org.example.project.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.example.project.models.Productos
import org.example.project.models.Usuarios
import org.example.project.models.Ventas
import org.example.project.models.VentaProductos
import org.example.project.services.DatabaseSeeder
import org.example.project.*

object DatabaseConfig {

    fun init() {
        val config = HikariConfig().apply {
            // Usar las funciones definidas en Constants.kt
            jdbcUrl = "jdbc:postgresql://${getDatabaseHost()}:$DATABASE_PORT/${getDatabaseName()}"
            username = "dpozas"
            password = "diego2025"
            driverClassName = "org.postgresql.Driver"
            maximumPoolSize = 10
            minimumIdle = 2
            connectionTimeout = CONNECTION_TIMEOUT
            idleTimeout = 600000
            maxLifetime = 1800000

            // Configuraciones adicionales para conexión remota y VPN
            addDataSourceProperty("ssl", "false")
            addDataSourceProperty("sslmode", "disable")
            addDataSourceProperty("tcpKeepAlive", "true")
            addDataSourceProperty("socketTimeout", "60")
            addDataSourceProperty("loginTimeout", "30")
            addDataSourceProperty("connectTimeout", "30")

            // Configuraciones adicionales para estabilidad de red
            addDataSourceProperty("prepareThreshold", "0")
            addDataSourceProperty("preparedStatementCacheQueries", "0")
            addDataSourceProperty("defaultRowFetchSize", "0")
        }

        val dataSource = HikariDataSource(config)
        Database.connect(dataSource)

        // Crear las tablas si no existen
        transaction {
            SchemaUtils.create(Productos, Usuarios, Ventas, VentaProductos)

            // Poblar la base de datos con datos iniciales
            println("🗄️ Inicializando datos de ejemplo...")
            DatabaseSeeder.seedDatabase()
        }
    }
}
