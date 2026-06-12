package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = UrbanPrimary,
    secondary = UrbanSecondary,
    tertiary = UrbanTertiary,
    background = DarkBack,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onBackground = Color(0xFFECEFF1),
    onSurface = Color(0xFFECEFF1),
    error = StatusReportado
)

private val LightColorScheme = lightColorScheme(
    primary = UrbanPrimary,
    secondary = UrbanSecondary,
    tertiary = UrbanTertiary,
    background = LightBack,
    surface = LightSurface,
    surfaceVariant = Color(0xFFECEFF1),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF263238),
    onSurface = Color(0xFF263238),
    error = StatusReportado
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Enforce our curated corporate aesthetic for consistent visual balance
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
