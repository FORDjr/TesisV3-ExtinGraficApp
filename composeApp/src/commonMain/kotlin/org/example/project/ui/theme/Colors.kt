package org.example.project.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Paleta de Colores Elegante: Extintor + Negro + Blanco
object ExtintorColors {
    // Colores principales
    val ExtintorRed = Color(0xFFDC2626)          // Rojo extintor intenso
    val ExtintorRedLight = Color(0xFFEF4444)     // Rojo extintor claro
    val ExtintorRedDark = Color(0xFFB91C1C)      // Rojo extintor oscuro

    // Escala de grises elegantes
    val CharcoalBlack = Color(0xFF1F1F23)        // Negro carbón
    val DeepBlack = Color(0xFF0F0F11)            // Negro profundo
    val PureWhite = Color(0xFFFFFFFE)            // Blanco puro
    val SoftWhite = Color(0xFFFAFAFA)            // Blanco suave

    // Grises sofisticados
    val Gray50 = Color(0xFFF9FAFB)
    val Gray100 = Color(0xFFF3F4F6)
    val Gray200 = Color(0xFFE5E7EB)
    val Gray300 = Color(0xFFD1D5DB)
    val Gray400 = Color(0xFF9CA3AF)
    val Gray500 = Color(0xFF6B7280)
    val Gray600 = Color(0xFF4B5563)
    val Gray700 = Color(0xFF374151)
    val Gray800 = Color(0xFF1F2937)
    val Gray900 = Color(0xFF111827)

    // Colores de estado con toques rojos
    val Success = Color(0xFF10B981)
    val Warning = Color(0xFFF59E0B)
    val Error = ExtintorRed
    val Info = Color(0xFF3B82F6)
}

// Esquema de colores claro elegante
val LightExtintorColorScheme = lightColorScheme(
    // Colores principales
    primary = ExtintorColors.ExtintorRed,
    onPrimary = ExtintorColors.PureWhite,
    primaryContainer = ExtintorColors.ExtintorRedLight,
    onPrimaryContainer = ExtintorColors.CharcoalBlack,

    // Colores secundarios
    secondary = ExtintorColors.Gray600,
    onSecondary = ExtintorColors.PureWhite,
    secondaryContainer = ExtintorColors.Gray100,
    onSecondaryContainer = ExtintorColors.CharcoalBlack,

    // Colores terciarios
    tertiary = ExtintorColors.ExtintorRedDark,
    onTertiary = ExtintorColors.PureWhite,
    tertiaryContainer = ExtintorColors.Gray50,
    onTertiaryContainer = ExtintorColors.CharcoalBlack,

    // Colores de error
    error = ExtintorColors.Error,
    onError = ExtintorColors.PureWhite,
    errorContainer = Color(0xFFFFEDED),
    onErrorContainer = ExtintorColors.ExtintorRedDark,

    // Colores de fondo
    background = ExtintorColors.SoftWhite,
    onBackground = ExtintorColors.CharcoalBlack,
    surface = ExtintorColors.PureWhite,
    onSurface = ExtintorColors.CharcoalBlack,
    surfaceVariant = ExtintorColors.Gray50,
    onSurfaceVariant = ExtintorColors.Gray600,

    // Colores de contorno
    outline = ExtintorColors.Gray300,
    outlineVariant = ExtintorColors.Gray200,

    // Colores diversos
    scrim = Color(0x80000000),
    inverseSurface = ExtintorColors.CharcoalBlack,
    inverseOnSurface = ExtintorColors.SoftWhite,
    inversePrimary = ExtintorColors.ExtintorRedLight,
)

// Esquema de colores oscuro elegante
val DarkExtintorColorScheme = darkColorScheme(
    // Colores principales
    primary = ExtintorColors.ExtintorRedLight,
    onPrimary = ExtintorColors.DeepBlack,
    primaryContainer = ExtintorColors.ExtintorRedDark,
    onPrimaryContainer = ExtintorColors.PureWhite,

    // Colores secundarios
    secondary = ExtintorColors.Gray400,
    onSecondary = ExtintorColors.Gray900,
    secondaryContainer = ExtintorColors.Gray800,
    onSecondaryContainer = ExtintorColors.Gray100,

    // Colores terciarios
    tertiary = ExtintorColors.ExtintorRed,
    onTertiary = ExtintorColors.PureWhite,
    tertiaryContainer = ExtintorColors.Gray900,
    onTertiaryContainer = ExtintorColors.Gray100,

    // Colores de error
    error = ExtintorColors.ExtintorRedLight,
    onError = ExtintorColors.DeepBlack,
    errorContainer = ExtintorColors.ExtintorRedDark,
    onErrorContainer = ExtintorColors.Gray100,

    // Colores de fondo
    background = ExtintorColors.DeepBlack,
    onBackground = ExtintorColors.SoftWhite,
    surface = ExtintorColors.CharcoalBlack,
    onSurface = ExtintorColors.SoftWhite,
    surfaceVariant = ExtintorColors.Gray800,
    onSurfaceVariant = ExtintorColors.Gray300,

    // Colores de contorno
    outline = ExtintorColors.Gray600,
    outlineVariant = ExtintorColors.Gray700,

    // Colores diversos
    scrim = Color(0x80000000),
    inverseSurface = ExtintorColors.Gray100,
    inverseOnSurface = ExtintorColors.CharcoalBlack,
    inversePrimary = ExtintorColors.ExtintorRed,
)
