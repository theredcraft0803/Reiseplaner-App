package de.reiseplaner.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val DoneGreen = Color(0xFF12A150)

private val LightColors = lightColorScheme(
    primary = Color(0xFF1F5FD6),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE7FF),
    onPrimaryContainer = Color(0xFF001A41),
    secondary = Color(0xFF00796B),
    secondaryContainer = Color(0xFFD3F0EA),
    onSecondaryContainer = Color(0xFF00201B),
    background = Color(0xFFF6F8FB),
    onBackground = Color(0xFF1A2230),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A2230),
    surfaceVariant = Color(0xFFE6EBF2),
    onSurfaceVariant = Color(0xFF5A6677),
    outlineVariant = Color(0xFFD9DFE8),
    error = Color(0xFFD93A3A)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8AB4FF),
    onPrimary = Color(0xFF002F6C),
    primaryContainer = Color(0xFF1B3A73),
    onPrimaryContainer = Color(0xFFDCE7FF),
    secondary = Color(0xFF63D2C0),
    secondaryContainer = Color(0xFF14403A),
    onSecondaryContainer = Color(0xFFD3F0EA),
    background = Color(0xFF0F1318),
    onBackground = Color(0xFFE8ECF1),
    surface = Color(0xFF171C23),
    onSurface = Color(0xFFE8ECF1),
    surfaceVariant = Color(0xFF232B36),
    onSurfaceVariant = Color(0xFF9AA6B5),
    outlineVariant = Color(0xFF2B3440),
    error = Color(0xFFFF6B6B)
)

@Composable
fun ReiseTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content
    )
}
