package org.example.project.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.example.project.models.Productos

object DatabaseConfig {

    fun init() {
        val config = HikariConfig().apply {
            // Cambiamos localhost por el servidor de la universidad
            jdbcUrl = "jdbc:postgresql://pgsqltrans.face.ubiobio.cl:5432/dpozas_bd"
            username = "dpozas"
            password = "diego2025"
            driverClassName = "org.postgresql.Driver"
            maximumPoolSize = 10
            minimumIdle = 2
            connectionTimeout = 30000
            idleTimeout = 600000
            maxLifetime = 1800000

            // Configuraciones adicionales para conexión remota
            addDataSourceProperty("ssl", "false")
            addDataSourceProperty("sslmode", "disable")
        }

        val dataSource = HikariDataSource(config)
        Database.connect(dataSource)

        // Crear las tablas si no existen
        transaction {
            SchemaUtils.create(Productos)
        }
    }
}
