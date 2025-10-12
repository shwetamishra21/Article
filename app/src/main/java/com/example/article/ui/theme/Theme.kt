package com.example.article.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color


// Linear gradient helper
fun professionalGradient(colors: List<Color>) = Brush.linearGradient(
    colors = colors,
    start = Offset(0f, 0f),
    end = Offset(800f, 800f)
)

// Gradient Button Composable
@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val gradient = Brush.horizontalGradient(listOf(RoyalViolet, SoftLilac))
    Button(
        onClick = onClick,
        enabled = enabled,
        contentPadding = PaddingValues(),
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .background(gradient, RoundedCornerShape(10.dp))
                .fillMaxWidth()
                .height(48.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text, color = AppWhite, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

// Typography for Material3
val AppTypography = Typography(
    bodyLarge = androidx.compose.ui.text.TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        letterSpacing = 0.5.sp
    ),
    titleMedium = androidx.compose.ui.text.TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        letterSpacing = 0.sp
    )
)

// Material3 App Theme
@Composable
fun ForgeTheme(content: @Composable () -> Unit) {
    val colorScheme = lightColorScheme(
        primary = RoyalViolet,
        onPrimary = AppWhite,
        secondary = SoftLilac,
        onSecondary = AppWhite,
        background = LightIvory,
        onBackground = DeepIndigo,
        surface = AppWhite,
        onSurface = DeepIndigo
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
