package com.dcfrederique.pizzabread.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Light = lightColorScheme(
    primary = Color(0xFF9A4E1C),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDCC2),
    onPrimaryContainer = Color(0xFF331200),
    secondary = Color(0xFF6E5A47),
    secondaryContainer = Color(0xFFF7DEC4),
    tertiary = Color(0xFFB5452B),
    tertiaryContainer = Color(0xFFFFDAD2),
    background = Color(0xFFFBF4EA),
    surface = Color(0xFFFBF4EA),
    surfaceVariant = Color(0xFFF1E3D3),
    surfaceContainer = Color(0xFFF5EADC),
    surfaceContainerHigh = Color(0xFFF0E2D1),
)

private val Dark = darkColorScheme(
    primary = Color(0xFFFFB68A),
    onPrimary = Color(0xFF532200),
    primaryContainer = Color(0xFF763408),
    onPrimaryContainer = Color(0xFFFFDCC2),
    secondary = Color(0xFFDFC1A5),
    secondaryContainer = Color(0xFF56432F),
    tertiary = Color(0xFFFFB4A4),
    tertiaryContainer = Color(0xFF8F2F17),
    background = Color(0xFF1C1510),
    surface = Color(0xFF1C1510),
    surfaceVariant = Color(0xFF3A2E25),
    surfaceContainer = Color(0xFF261E18),
    surfaceContainerHigh = Color(0xFF31281F),
)

@Composable
fun BroodTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Dark else Light, content = content)
}
