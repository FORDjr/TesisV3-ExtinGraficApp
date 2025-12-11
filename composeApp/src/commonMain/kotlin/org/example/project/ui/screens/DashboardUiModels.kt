package org.example.project.ui.screens

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class DashboardMetric(
    val title: String,
    val value: String,
    val trend: String?,
    val positiveTrend: Boolean
)

@Immutable
data class QuickAction(
    val title: String,
    val subtitle: String?,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val iconTint: Color? = null,
    val highlight: Boolean = false,
    val onClick: () -> Unit
)
