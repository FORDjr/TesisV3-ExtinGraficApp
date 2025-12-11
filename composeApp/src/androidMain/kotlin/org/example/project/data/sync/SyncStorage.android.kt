package org.example.project.data.sync

import java.io.File

private val queueFile = File(System.getProperty("java.io.tmpdir"), "extingrafic-sync.json")

actual fun provideSyncStorage(): SyncStorage = object : SyncStorage {
    override suspend fun read(): String? = if (queueFile.exists()) queueFile.readText() else null
    override suspend fun write(content: String) {
        queueFile.writeText(content)
    }
}
