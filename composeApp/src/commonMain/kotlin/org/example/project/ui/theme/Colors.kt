package org.example.project.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

object ExtintorColors {
    // Core palette
    val PrimaryRed = Color(0xFFEB4B4B)
    val PrimaryRedDark = Color(0xFFD63A3A)
    val PrimaryRedSoft = Color(0xFFFFE5E5)

    val Slate950 = Color(0xFF0F172A)
    val Slate900 = Color(0xFF111827)
    val Slate800 = Color(0xFF1F2937)
    val Slate700 = Color(0xFF374151)
    val Slate600 = Color(0xFF475569)
    val Slate500 = Color(0xFF64748B)
    val Slate400 = Color(0xFF94A3B8)
    val Slate300 = Color(0xFFCBD5E1)
    val Slate200 = Color(0xFFE2E8F0)
    val Slate100 = Color(0xFFF1F5F9)
    val Slate50 = Color(0xFFF8FAFC)

    val Success = Color(0xFF22C55E)
    val Warning = Color(0xFFF59E0B)
    val Error = Color(0xFFEF4444)
    val Info = Color(0xFF38BDF8)

    // Legacy aliases kept for compatibility while refactoring the UI codebase
    val ExtintorRed: Color get() = PrimaryRed
    val ExtintorRedLight = Color(0xFFFF6B6B)
    val ExtintorRedDark: Color get() = PrimaryRedDark

    val CharcoalBlack: Color get() = Slate900
    val DeepBlack: Color get() = Slate950
    val PureWhite: Color = Color.White
    val SoftWhite: Color get() = Slate50
    val SoftLavender: Color = Color(0xFFF0EAF7)

    val Gray50: Color get() = Slate50
    val Gray100: Color get() = Slate100
    val Gray200: Color get() = Slate200
    val Gray300: Color get() = Slate300
    val Gray400: Color get() = Slate400
    val Gray500: Color get() = Slate500
    val Gray600: Color get() = Slate600
    val Gray700: Color get() = Slate700
    val Gray800: Color get() = Slate800
    val Gray900: Color get() = Slate900
}

val LightExtintorColorScheme = lightColorScheme(
    primary = ExtintorColors.PrimaryRed,
    onPrimary = Color.White,
    primaryContainer = ExtintorColors.PrimaryRedSoft,
    onPrimaryContainer = ExtintorColors.PrimaryRedDark,

    secondary = ExtintorColors.Slate700,
    onSecondary = Color.White,
    secondaryContainer = ExtintorColors.Slate100,
    onSecondaryContainer = ExtintorColors.Slate900,

    tertiary = ExtintorColors.Slate600,
    onTertiary = Color.White,
    tertiaryContainer = ExtintorColors.Slate100,
    onTertiaryContainer = ExtintorColors.Slate800,

    error = ExtintorColors.Error,
    onError = Color.White,
    errorContainer = ExtintorColors.PrimaryRedSoft,
    onErrorContainer = ExtintorColors.PrimaryRedDark,

    background = ExtintorColors.Slate50,
    onBackground = ExtintorColors.Slate900,
    surface = Color.White,
    onSurface = ExtintorColors.Slate900,
    surfaceVariant = ExtintorColors.Slate100,
    onSurfaceVariant = ExtintorColors.Slate600,

    outline = ExtintorColors.Slate200,
    outlineVariant = ExtintorColors.Slate100,

    scrim = Color(0x66000000),
    inverseSurface = ExtintorColors.Slate900,
    inverseOnSurface = Color.White,
    inversePrimary = ExtintorColors.PrimaryRedSoft,
)

val DarkExtintorColorScheme = darkColorScheme(
    primary = ExtintorColors.PrimaryRedSoft,
    onPrimary = ExtintorColors.PrimaryRedDark,
    primaryContainer = ExtintorColors.PrimaryRed,
    onPrimaryContainer = Color.White,

    secondary = ExtintorColors.Slate300,
    onSecondary = ExtintorColors.Slate900,
    secondaryContainer = ExtintorColors.Slate700,
    onSecondaryContainer = ExtintorColors.Slate100,

    tertiary = ExtintorColors.Slate400,
    onTertiary = ExtintorColors.Slate950,
    tertiaryContainer = ExtintorColors.Slate800,
    onTertiaryContainer = ExtintorColors.Slate100,

    error = ExtintorColors.Error,
    onError = Color.White,
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color.White,

    background = ExtintorColors.Slate900,
    onBackground = ExtintorColors.Slate100,
    surface = ExtintorColors.Slate800,
    onSurface = ExtintorColors.Slate100,
    surfaceVariant = ExtintorColors.Slate700,
    onSurfaceVariant = ExtintorColors.Slate300,

    outline = ExtintorColors.Slate600,
    outlineVariant = ExtintorColors.Slate700,

    scrim = Color(0x99000000),
    inverseSurface = Color.White,
    inverseOnSurface = ExtintorColors.Slate900,
    inversePrimary = ExtintorColors.PrimaryRed,
)
