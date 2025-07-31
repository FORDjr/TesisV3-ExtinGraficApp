package org.example.project.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.example.project.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.example.project.models.*          // tus tablas
import org.example.project.services.DatabaseSeeder

object DatabaseConfig {

    fun init() {
        /* ---------- Hikari ---------- */
        val hConfig = HikariConfig().apply {
            jdbcUrl = "jdbc:postgresql://$DB_HOST:$DB_PORT/$DB_NAME"
            username = DB_USER
            password = DB_PASS
            driverClassName = "org.postgresql.Driver"

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
            SchemaUtils.create(Productos, Usuarios, Ventas, VentaProductos)
            println("🗄️  Inicializando datos de ejemplo…")
            DatabaseSeeder.seedDatabase()
        }
    }
}
