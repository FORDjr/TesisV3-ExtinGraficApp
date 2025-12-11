package org.example.project.data.sync

import platform.Foundation.NSString
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.stringWithContentsOfFile
import platform.Foundation.writeToFile

private val queuePath = (NSTemporaryDirectory() ?: "") + "extingrafic-sync.json"

actual fun provideSyncStorage(): SyncStorage = object : SyncStorage {
    override suspend fun read(): String? =
        NSString.stringWithContentsOfFile(queuePath, encoding = NSUTF8StringEncoding, error = null)?.toString()

    override suspend fun write(content: String) {
        val nsString = NSString.create(string = content)
        nsString.writeToFile(queuePath, atomically = true, encoding = NSUTF8StringEncoding, error = null)
    }
}
