package org.example.project.utils

import androidx.compose.runtime.Composable

/**
 * Retorna un contexto de plataforma cuando exista (Android), o null en las demás.
 */
@Composable
expect fun platformContext(): Any?
