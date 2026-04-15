package com.kinetik.arsurvey.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF1B5E20),      // Dark green (construction)
    onPrimary = Color.White,
    primaryContainer = Color(0xFF2E7D32),
    onPrimaryContainer = Color.White,
    secondary = Color(0xFFFF8F00),     // Orange accent (safety vest)
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFFFFB300),
    onSecondaryContainer = Color.Black,
    tertiary = Color(0xFFD32F2F),      // Red (danger/violation)
    tertiaryContainer = Color(0xFFB71C1C),
    background = Color(0xFF121212),
    onBackground = Color(0xFFE0E0E0),
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFE0E0E0),
    surfaceVariant = Color(0xFF2C2C2C),
    onSurfaceVariant = Color(0xFFBDBDBD),
    error = Color(0xFFCF6679),
    onError = Color.Black
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF2E7D32),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFA5D6A7),
    onPrimaryContainer = Color(0xFF1B5E20),
    secondary = Color(0xFFFF8F00),
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFFFFE082),
    onSecondaryContainer = Color(0xFF6D4C00),
    tertiary = Color(0xFFD32F2F),
    tertiaryContainer = Color(0xFFFFCDD2),
    background = Color(0xFFFAFAFA),
    onBackground = Color(0xFF212121),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF212121),
    surfaceVariant = Color(0xFFF5F5F5),
    onSurfaceVariant = Color(0xFF424242),
    error = Color(0xFFB00020),
    onError = Color.White
)

@Composable
fun ARSurveyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
