package org.example.project

import androidx.compose.runtime.*
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.example.project.ui.screens.MainScreen
import org.example.project.ui.screens.LoginScreen
import org.example.project.ui.theme.ExtintorTheme
import org.example.project.data.auth.AuthManager

@Composable
@Preview
fun App() {
    ExtintorTheme {
        val authState by AuthManager.authState.collectAsState()

        if (authState.isAuthenticated) {
            MainScreen(
                onLogout = {
                    AuthManager.logout()
                }
            )
        } else {
            LoginScreen(
                onLoginSuccess = {
                }
            )
        }
    }
}
