package org.example.project.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun platformContext(): Any? = LocalContext.current
