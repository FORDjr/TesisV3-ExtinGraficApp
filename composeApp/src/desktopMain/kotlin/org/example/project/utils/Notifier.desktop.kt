package org.example.project.utils

actual object Notifier {
    actual fun init(context: Any?) {}
    actual fun notify(title: String, message: String) {
        println("Notification: $title - $message")
    }
}
