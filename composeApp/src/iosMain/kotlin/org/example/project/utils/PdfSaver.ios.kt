package org.example.project.utils

import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.create
import platform.Foundation.writeToURL
import platform.Foundation.NSData

actual suspend fun savePdfToFile(filename: String, bytes: ByteArray): String {
    val dir = NSTemporaryDirectory()
    val path = dir + filename
    val url = NSURL.fileURLWithPath(path)
    val data = NSData.create(bytes = bytes, length = bytes.size.toULong())
    data?.writeToURL(url, true)
    return path
}
