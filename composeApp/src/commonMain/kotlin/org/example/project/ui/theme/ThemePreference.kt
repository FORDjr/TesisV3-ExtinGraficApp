package org.example.project.ui.theme

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// Preferencias de tema disponibles
enum class ThemePreference { SYSTEM, LIGHT, DARK }

// Gestor simple en memoria (se puede extender a persistencia multiplataforma luego)
object ThemeManager {
    private val _themePreference = MutableStateFlow(ThemePreference.SYSTEM)
    val themePreference: StateFlow<ThemePreference> = _themePreference

    fun setThemePreference(pref: ThemePreference) {
        _themePreference.value = pref
    }
}

