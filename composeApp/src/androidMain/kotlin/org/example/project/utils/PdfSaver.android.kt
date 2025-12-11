package org.example.project.utils

import java.io.File
import java.io.FileOutputStream

actual suspend fun savePdfToFile(filename: String, bytes: ByteArray): String {
    val dirPath = System.getProperty("java.io.tmpdir") ?: "/data/data/org.example.project/cache"
    val dir = File(dirPath)
    val file = File(dir, filename)
    FileOutputStream(file).use { it.write(bytes) }
    return file.absolutePath
}
