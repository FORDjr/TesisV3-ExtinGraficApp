package org.example.project

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.*
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.example.project.ui.screens.MainScreen
import org.example.project.ui.screens.LoginScreen
import org.example.project.ui.theme.ExtintorTheme
import org.example.project.data.auth.AuthManager
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import androidx.compose.ui.graphics.Color

@Composable
@Preview
fun App() {
    ExtintorTheme {
        val systemUiController = rememberSystemUiController()
        SideEffect {
            systemUiController.setStatusBarColor(
                color = Color.Transparent,
                darkIcons = true // Cambia a false si usas fondo oscuro
            )
        }
        Box(
            modifier = androidx.compose.ui.Modifier.statusBarsPadding()
        ) {
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
}
