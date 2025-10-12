package com.example.article.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = RoyalViolet,
    onPrimary = BrightWhite,
    background = Charcoal,
    onBackground = BrightWhite,
    surface = Color(0xFF1E1E1E),
    onSurface = BrightWhite,
    secondary = SoftLilac
)

private val LightColors = lightColorScheme(
    primary = RoyalViolet,
    onPrimary = BrightWhite,
    background = LavenderMist,
    onBackground = Color.Black,
    surface = BrightWhite,
    onSurface = Color.Black,
    secondary = DeepPlum
)

@Composable
fun ForgeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        shapes = MaterialTheme.shapes,
        content = content
    )
}
