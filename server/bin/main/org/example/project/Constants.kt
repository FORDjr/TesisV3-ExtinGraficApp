package org.example.project

// Configuración dinámica basada en el entorno
private val isProduction = System.getenv("env") == "production"

// Puertos disponibles
const val LOCAL_PORT = 8080      // Puerto local para desarrollo
const val PRODUCTION_PORT = 8080 // Puerto interno para Java (Apache hace proxy desde 80)

// Configuración de base de datos
const val PRODUCTION_DB_HOST = "pgsqltrans.face.ubiobio.cl"
const val LOCAL_DB_HOST = "localhost"
const val DATABASE_PORT = 5432
const val PRODUCTION_DB_NAME = "dpozas_bd"
const val LOCAL_DB_NAME = "test_db"
const val CONNECTION_TIMEOUT = 30000L

// Funciones para obtener configuración dinámica
fun getServerPort(): Int = if (isProduction) PRODUCTION_PORT else LOCAL_PORT
fun getDatabaseHost(): String = if (isProduction) PRODUCTION_DB_HOST else LOCAL_DB_HOST
fun getDatabaseName(): String = if (isProduction) PRODUCTION_DB_NAME else LOCAL_DB_NAME
