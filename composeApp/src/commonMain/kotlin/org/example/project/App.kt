package org.example.project

import androidx.compose.runtime.*
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.example.project.ui.screens.MainScreen
import org.example.project.ui.theme.ExtintorTheme

@Composable
@Preview
fun App() {
    ExtintorTheme {
        MainScreen()
    }
}