package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val TacticalDarkColorScheme = darkColorScheme(
    primary = TacticalCyan,
    onPrimary = Color(0xFF031B24),
    primaryContainer = Color(0xFF0E3A4C),
    onPrimaryContainer = Color(0xFFA5F3FC),
    secondary = TacticalAmber,
    onSecondary = Color(0xFF281802),
    secondaryContainer = Color(0xFF452B06),
    onSecondaryContainer = Color(0xFFFDE68A),
    tertiary = TacticalCoralRed,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF4C1111),
    onTertiaryContainer = Color(0xFFFECACA),
    background = TacticalNavyDark,
    onBackground = Color(0xFFF1F5F9),
    surface = TacticalSurfaceDark,
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = TacticalCardDark,
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = TacticalBorderDark,
    error = TacticalCoralRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = TacticalDarkColorScheme,
        typography = Typography,
        content = content
    )
}
