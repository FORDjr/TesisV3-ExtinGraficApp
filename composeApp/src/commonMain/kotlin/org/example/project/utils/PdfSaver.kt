package org.example.project.utils

expect suspend fun savePdfToFile(filename: String, bytes: ByteArray): String
