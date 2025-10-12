package com.example.article.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val LightColors = lightColorScheme(
    primary = RoyalViolet,
    onPrimary = BrightWhite,
    primaryContainer = SoftLilac,
    onPrimaryContainer = DeepPlum,
    secondary = PeachGlow,
    onSecondary = DeepPlum,
    secondaryContainer = PeachGlow.copy(alpha = 0.3f),
    onSecondaryContainer = DeepPlum,
    tertiary = SoftLilac,
    onTertiary = DeepPlum,
    error = Color(0xFFE57373),
    onError = BrightWhite,
    errorContainer = Color(0xFFFFEBEE),
    onErrorContainer = Color(0xFFD32F2F),
    background = LavenderMist,
    onBackground = SteelGray,
    surface = BrightWhite,
    onSurface = DeepPlum,
    surfaceVariant = SoftLilac.copy(alpha = 0.5f),
    onSurfaceVariant = SteelGray,
    outline = SteelGray.copy(alpha = 0.5f),
    outlineVariant = SoftLilac,
    scrim = Color.Black.copy(alpha = 0.3f),
    inverseSurface = DeepPlum,
    inverseOnSurface = BrightWhite,
    inversePrimary = SoftLilac,
    surfaceDim = LavenderMist.copy(alpha = 0.8f),
    surfaceBright = BrightWhite,
    surfaceContainerLowest = BrightWhite,
    surfaceContainerLow = LavenderMist.copy(alpha = 0.5f),
    surfaceContainer = SoftLilac.copy(alpha = 0.3f),
    surfaceContainerHigh = SoftLilac.copy(alpha = 0.6f),
    surfaceContainerHighest = SoftLilac.copy(alpha = 0.8f)
)

val AppFont = FontFamily.SansSerif

private val AppTypography = Typography(
    // Display styles
    displayLarge = Typography().displayLarge.copy(
        fontFamily = AppFont,
        fontWeight = FontWeight.Bold,
        color = DeepPlum
    ),
    displayMedium = Typography().displayMedium.copy(
        fontFamily = AppFont,
        fontWeight = FontWeight.Bold,
        color = DeepPlum
    ),
    displaySmall = Typography().displaySmall.copy(
        fontFamily = AppFont,
        fontWeight = FontWeight.Bold,
        color = DeepPlum
    ),

    // Headline styles
    headlineLarge = Typography().headlineLarge.copy(
        fontFamily = AppFont,
        fontWeight = FontWeight.Bold,
        color = DeepPlum
    ),
    headlineMedium = Typography().headlineMedium.copy(
        fontFamily = AppFont,
        fontWeight = FontWeight.SemiBold,
        color = DeepPlum
    ),
    headlineSmall = Typography().headlineSmall.copy(
        fontFamily = AppFont,
        fontWeight = FontWeight.SemiBold,
        color = DeepPlum
    ),

    // Title styles
    titleLarge = Typography().titleLarge.copy(
        fontFamily = AppFont,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        color = DeepPlum
    ),
    titleMedium = Typography().titleMedium.copy(
        fontFamily = AppFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        color = DeepPlum
    ),
    titleSmall = Typography().titleSmall.copy(
        fontFamily = AppFont,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        color = DeepPlum
    ),

    // Body styles
    bodyLarge = Typography().bodyLarge.copy(
        fontFamily = AppFont,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        color = SteelGray
    ),
    bodyMedium = Typography().bodyMedium.copy(
        fontFamily = AppFont,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        color = SteelGray
    ),
    bodySmall = Typography().bodySmall.copy(
        fontFamily = AppFont,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        color = SteelGray
    ),

    // Label styles
    labelLarge = Typography().labelLarge.copy(
        fontFamily = AppFont,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        color = SteelGray
    ),
    labelMedium = Typography().labelMedium.copy(
        fontFamily = AppFont,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        color = SteelGray
    ),
    labelSmall = Typography().labelSmall.copy(
        fontFamily = AppFont,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        color = SteelGray
    )
)

@Composable
fun ForgeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = AppTypography,
        content = content
    )
}