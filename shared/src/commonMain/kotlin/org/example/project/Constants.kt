package org.example.project

const val SERVER_PORT = 8081  // Cambiado de 8080 a 8081
// Configuración para diferentes entornos
const val DATABASE_HOST = "pgsqltrans.face.ubiobio.cl"
const val DATABASE_PORT = 5432
const val DATABASE_NAME = "dpozas_bd"

// URLs base para diferentes configuraciones
const val LOCAL_SERVER_URL = "http://localhost:8081"
const val UNIVERSITY_SERVER_URL = "http://pgsqltrans.face.ubiobio.cl:8081"

// Configuración de timeouts para conexiones móviles
const val CONNECTION_TIMEOUT = 30000L
const val READ_TIMEOUT = 60000L
