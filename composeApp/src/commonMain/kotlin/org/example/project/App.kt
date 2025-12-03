package org.example.project

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.*
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.example.project.ui.screens.MainScreen
import org.example.project.ui.screens.LoginScreen
import org.example.project.ui.theme.ExtintorTheme
import org.example.project.data.auth.AuthManager
import androidx.compose.foundation.isSystemInDarkTheme
import org.example.project.ui.theme.ThemeManager
import org.example.project.ui.theme.ThemePreference

@Composable
@Preview
fun App() {
    ExtintorTheme {
        val themePref by ThemeManager.themePreference.collectAsState()
        // isDark se mantiene por si en el futuro se aplican ajustes condicionales
        val isDark = when (themePref) {
            ThemePreference.SYSTEM -> isSystemInDarkTheme()
            ThemePreference.LIGHT -> false
            ThemePreference.DARK -> true
        }
        Box(
            modifier = androidx.compose.ui.Modifier.statusBarsPadding()
        ) {
            val authState by AuthManager.authState.collectAsState()
            if (authState.isAuthenticated) {
                MainScreen(
                    onLogout = { AuthManager.logout() }
                )
            } else {
                LoginScreen(onLoginSuccess = { })
            }
        }
    }
}
