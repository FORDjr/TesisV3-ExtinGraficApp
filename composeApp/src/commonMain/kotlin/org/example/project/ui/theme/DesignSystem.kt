package org.example.project.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class ExtintorSpacing(
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 12.dp,
    val lg: Dp = 16.dp,
    val xl: Dp = 24.dp,
    val xxl: Dp = 32.dp
)

val LocalExtintorSpacing = staticCompositionLocalOf { ExtintorSpacing() }

object ExtintorElevation {
    val Level0 = 0.dp
    val Level1 = 2.dp
    val Level2 = 4.dp
    val Level3 = 8.dp
}

object ExtintorDefaults {
    val CardShape = RoundedCornerShape(18.dp)

    @Composable
    fun cardColors(highlighted: Boolean = false) = CardDefaults.cardColors(
        containerColor = if (highlighted) {
            ExtintorColors.PrimaryRedSoft
        } else {
            MaterialTheme.colorScheme.surface
        },
        contentColor = MaterialTheme.colorScheme.onSurface
    )

    @Composable
    fun cardElevation(level: Dp = ExtintorElevation.Level1) = CardDefaults.cardElevation(
        defaultElevation = level,
        pressedElevation = level + 2.dp
    )
}
