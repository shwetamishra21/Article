package com.example.article.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

private val LightColors = lightColorScheme(
    primary = RoyalViolet,
    secondary = PeachGlow,
    background = BrightWhite,
    surface = LavenderMist,
    onPrimary = BrightWhite,
    onSecondary = DeepPlum,
    onBackground = SteelGray,
    onSurface = DeepPlum,
)

val AppFont = FontFamily.SansSerif

private val AppTypography = Typography(
    titleLarge = Typography().titleLarge.copy(fontFamily = AppFont, fontWeight = FontWeight.Bold),
    titleMedium = Typography().titleMedium.copy(fontFamily = AppFont, fontWeight = FontWeight.SemiBold),
    bodyMedium = Typography().bodyMedium.copy(fontFamily = AppFont),
    labelLarge = Typography().labelLarge.copy(fontFamily = AppFont, fontWeight = FontWeight.Medium)
)

@Composable
fun ForgeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = AppTypography,
        content = content
    )
}
