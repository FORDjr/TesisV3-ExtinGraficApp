package org.example.project.data.sync

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.example.project.data.api.MovimientosApiService
import org.example.project.data.model.CrearMovimientoRequest
import kotlin.math.min
import kotlin.random.Random

@Serializable
enum class PendingType { MOVIMIENTO }

@Serializable
data class PendingOperation(
    val id: String,
    val type: PendingType,
    val movimiento: CrearMovimientoRequest? = null,
    val retries: Int = 0,
    val nextRetryAt: Long = 0L,
    val lastError: String? = null
)

interface SyncStorage {
    suspend fun read(): String?
    suspend fun write(content: String)
}

expect fun provideSyncStorage(): SyncStorage

object PendingSyncManager {
    private val mutex = Mutex()
    private val storage: SyncStorage = provideSyncStorage()
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val _pending = MutableStateFlow<List<PendingOperation>>(emptyList())
    val pending: StateFlow<List<PendingOperation>> = _pending.asStateFlow()
    private var loaded = false

    // Cliente dedicado para la cola (reutilizable)
    private val movimientosApi = MovimientosApiService()

    suspend fun enqueueMovimiento(request: CrearMovimientoRequest): String {
        ensureLoaded()
        return mutex.withLock {
            val key = request.idempotenciaKey ?: generateKey()
            val op = PendingOperation(
                id = key,
                type = PendingType.MOVIMIENTO,
                movimiento = request.copy(idempotenciaKey = key),
                retries = 0,
                nextRetryAt = System.currentTimeMillis(),
                lastError = null
            )
            _pending.value = _pending.value.filterNot { it.id == key } + op
            persistLocked()
            key
        }
    }

    /**
     * Procesa la cola respetando backoff exponencial.
     * @return cantidad de operaciones despachadas con éxito
     */
    suspend fun processQueue(): Int {
        ensureLoaded()
        var processed = 0
        mutex.withLock {
            val now = System.currentTimeMillis()
            val current = _pending.value.toMutableList()
            val ready = current.filter { it.nextRetryAt <= now }
            ready.forEach { op ->
                try {
                    when (op.type) {
                        PendingType.MOVIMIENTO -> {
                            val payload = op.movimiento ?: error("Payload de movimiento vacío")
                            movimientosApi.crearMovimiento(payload)
                        }
                    }
                    current.removeAll { it.id == op.id }
                    processed += 1
                } catch (e: Exception) {
                    val retries = op.retries + 1
                    val delayMs = backoffMs(retries)
                    val updated = op.copy(
                        retries = retries,
                        nextRetryAt = now + delayMs,
                        lastError = e.message?.take(140)
                    )
                    val idx = current.indexOfFirst { it.id == op.id }
                    if (idx >= 0) current[idx] = updated
                }
            }
            _pending.value = current
            persistLocked()
        }
        return processed
    }

    fun pendingCount(): Int = _pending.value.size

    fun generateKey(prefix: String = "mov"): String {
        val rand = Random.nextInt(0, Int.MAX_VALUE).toString(16)
        val timestamp = System.currentTimeMillis().toString(16)
        return "$prefix-$timestamp-$rand"
    }

    fun close() {
        movimientosApi.close()
    }

    private suspend fun ensureLoaded() {
        if (loaded) return
        mutex.withLock {
            if (loaded) return@withLock
            val raw = storage.read()
            if (!raw.isNullOrBlank()) {
                runCatching {
                    val serializer = ListSerializer(PendingOperation.serializer())
                    _pending.value = json.decodeFromString(serializer, raw)
                }
            }
            loaded = true
        }
    }

    private suspend fun persistLocked() {
        val serializer = ListSerializer(PendingOperation.serializer())
        storage.write(json.encodeToString(serializer, _pending.value))
    }

    private fun backoffMs(retries: Int): Long {
        val capped = retries.coerceAtMost(6)
        val factor = 1L shl capped
        return min(60_000L, 1_000L * factor)
    }
}
