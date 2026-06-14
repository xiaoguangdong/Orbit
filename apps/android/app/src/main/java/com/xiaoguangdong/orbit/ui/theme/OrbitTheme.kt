package com.xiaoguangdong.orbit.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF2F6BFF),
    onPrimary = Color.White,
    secondary = Color(0xFF24C99A),
    tertiary = Color(0xFFF6B938),
    error = Color(0xFFFF6B6B),
    background = Color(0xFFF5F7FC),
    surface = Color.White,
    surfaceVariant = Color(0xFFEAF0FF),
    onSurface = Color(0xFF162038),
    onSurfaceVariant = Color(0xFF5B6478),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF84A7FF),
    secondary = Color(0xFF6EE6C2),
    tertiary = Color(0xFFFFD37A),
    background = Color(0xFF0D1425),
    surface = Color(0xFF111A2D),
    surfaceVariant = Color(0xFF162038),
    onSurface = Color(0xFFF4F7FF),
    onSurfaceVariant = Color(0xFFB8C3D9),
    error = Color(0xFFFF8A8A),
)

@Composable
fun OrbitTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = OrbitTypography,
        content = content,
    )
}
