package com.example.article.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape

// ---------------------------
// Shapes
// ---------------------------
val ForgeShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp)
)

// ---------------------------
// Dark & Light Color Schemes
// ---------------------------
private val LightColors = lightColorScheme(
    primary = RoyalViolet,
    onPrimary = BrightWhite,
    secondary = DeepPlum,
    onSecondary = BrightWhite,
    background = LavenderMist,
    onBackground = Color.Black,
    surface = BrightWhite,
    onSurface = Color.Black
)

private val DarkColors = darkColorScheme(
    primary = RoyalViolet,
    onPrimary = BrightWhite,
    secondary = SoftLilac,
    onSecondary = BrightWhite,
    background = Charcoal,
    onBackground = BrightWhite,
    surface = Color(0xFF1E1E1E),
    onSurface = BrightWhite
)

// ---------------------------
// Forge Theme Composable
// ---------------------------
@Composable
fun ForgeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        shapes = ForgeShapes,
        content = content
    )
}

// ---------------------------
// Optional Helpers for Consistency
// ---------------------------
@Composable
fun ForgeCard(
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(6.dp),
        content = content
    )
}

@Composable
fun ForgeButton(
    onClick: () -> Unit,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
    text: String
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
