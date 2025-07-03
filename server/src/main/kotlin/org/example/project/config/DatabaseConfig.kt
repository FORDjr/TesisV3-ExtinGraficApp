package org.example.project.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.example.project.models.Productos
import org.example.project.*

object DatabaseConfig {

    fun init() {
        val config = HikariConfig().apply {
            // Usar las constantes definidas
            jdbcUrl = "jdbc:postgresql://$DATABASE_HOST:$DATABASE_PORT/$DATABASE_NAME"
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
            SchemaUtils.create(Productos)
        }
    }
}
