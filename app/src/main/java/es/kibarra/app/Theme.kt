package es.kibarra.app

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

// Colores del logo de KiBarra (azul + magenta) y el fondo oscuro de la GUI de escritorio.
val KibarraAzul = Color(0xFF00AEEF)
val KibarraMagenta = Color(0xFFFF0099)
val KibarraFondo = Color(0xFF10172A)
val KibarraSuperficie = Color(0xFF1B2340)

val KibarraColorScheme = darkColorScheme(
    primary = KibarraAzul,
    onPrimary = Color.White,
    secondary = KibarraMagenta,
    onSecondary = Color.White,
    tertiary = KibarraMagenta,
    background = KibarraFondo,
    onBackground = Color(0xFFE6E9F5),
    surface = KibarraSuperficie,
    onSurface = Color(0xFFE6E9F5),
    surfaceVariant = KibarraSuperficie,
    onSurfaceVariant = Color(0xFFB8C0E0),
)
