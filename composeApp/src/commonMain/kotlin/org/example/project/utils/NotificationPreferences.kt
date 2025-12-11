package org.example.project.utils

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class NotificationSettings(
    val notifySales: Boolean = true,
    val notifyStock: Boolean = true,
    val notifyReminders: Boolean = true
)

object NotificationPreferences {
    private val _settings = MutableStateFlow(NotificationSettings())
    val settings: StateFlow<NotificationSettings> = _settings.asStateFlow()

    fun setNotifySales(enabled: Boolean) {
        _settings.value = _settings.value.copy(notifySales = enabled)
    }

    fun setNotifyStock(enabled: Boolean) {
        _settings.value = _settings.value.copy(notifyStock = enabled)
    }

    fun setNotifyReminders(enabled: Boolean) {
        _settings.value = _settings.value.copy(notifyReminders = enabled)
    }
}
