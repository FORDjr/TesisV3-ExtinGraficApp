package org.example.project.utils

import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files

actual suspend fun savePdfToFile(filename: String, bytes: ByteArray): String {
    val dir = Files.createTempDirectory("ventas-pdf").toFile()
    val file = File(dir, filename)
    FileOutputStream(file).use { it.write(bytes) }
    return file.absolutePath
}
