package org.example.project.utils

expect object Notifier {
    fun init(context: Any? = null)
    fun notify(title: String, message: String)
}
