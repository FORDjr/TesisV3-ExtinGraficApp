package org.example.project

/** Lee una variable de entorno o devuelve un valor por defecto */
private fun env(key: String, default: String) =
    System.getenv(key) ?: default

private fun propOrEnv(key: String, default: String) = System.getenv(key) ?: System.getProperty(key) ?: default

/* ---------- Configuración base ---------- */

val SERVER_PORT: Int = env("SERVER_PORT", "8080").toInt()

val DB_HOST: String = env("DB_HOST", "localhost")
val DB_PORT: Int    = env("DB_PORT", "5432").toInt()
val DB_NAME: String = env("DB_NAME", "test_db")
val DB_USER: String = env("DB_USER", "postgres")
val DB_PASS: String = env("DB_PASSWORD", "postgres")

val ALLOWED_ORIGINS: String = env("ALLOWED_ORIGINS", "*")
val IS_PRODUCTION: Boolean = env("env", "development") == "production"
val API_VERSION: String = env("API_VERSION", "1.0.0")

val DISABLE_SCHEDULER: Boolean = env("DISABLE_SCHEDULER", "false") == "true"

/* Otros “tunables” */
const val CONNECTION_TIMEOUT = 30_000L       // 30 s

val USE_H2: Boolean = propOrEnv("USE_H2", "false") == "true"

// Periodos (meses) para próxima recarga según agente (placeholder configurable)
val RECARGA_MESES_POR_AGENTE: Map<String, Int> = mapOf(
    "ABC" to 12,
    "CO2" to 12,
    "K" to 12,
    "ESPUMA" to 12
)
