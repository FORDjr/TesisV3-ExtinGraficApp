package org.example.project

/** Lee una variable de entorno o devuelve un valor por defecto */
private fun env(key: String, default: String) =
    System.getenv(key) ?: default

/* ---------- Configuración base ---------- */

val SERVER_PORT: Int = env("SERVER_PORT", "8080").toInt()

val DB_HOST: String = env("DB_HOST", "localhost")
val DB_PORT: Int    = env("DB_PORT", "5432").toInt()
val DB_NAME: String = env("DB_NAME", "test_db")
val DB_USER: String = env("DB_USER", "postgres")
val DB_PASS: String = env("DB_PASSWORD", "postgres")

/* Otros “tunables” */
const val CONNECTION_TIMEOUT = 30_000L       // 30 s
