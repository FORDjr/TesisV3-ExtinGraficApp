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

/* ---------- Seguridad / JWT ---------- */
val JWT_SECRET: String = env("JWT_SECRET", "super-secret-key-change-me")
val JWT_ISSUER: String = env("JWT_ISSUER", "extingrafic-api")
val JWT_AUDIENCE: String = env("JWT_AUDIENCE", "extingrafic-clients")
val JWT_REALM: String = env("JWT_REALM", "extingrafic")
val JWT_EXP_MINUTES: Long = env("JWT_EXP_MINUTES", "120").toLong() // 2h por defecto

/* Integraciones externas (API keys) */
val DEFAULT_INTEGRATION_TOKEN: String = env("INTEGRATION_TOKEN", "ext-api-demo-token")
val DEFAULT_INTEGRATION_SCOPES: String = env(
    "INTEGRATION_SCOPES",
    "INVENTARIO_READ,MOVIMIENTOS_READ,MOVIMIENTOS_WRITE,REPORTES_READ"
)

const val MAX_FAILED_ATTEMPTS = 5
const val LOCKOUT_MINUTES = 15

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
