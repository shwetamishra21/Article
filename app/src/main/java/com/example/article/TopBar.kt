package com.example.article

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.example.article.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(username: String, onLogout: () -> Unit) {
    val gradientBrush = Brush.horizontalGradient(
        colors = listOf(RoyalViolet, DeepPlum, BrightWhite)
    )

    CenterAlignedTopAppBar(
        title = {
            Column {
                Text("Forge", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = BrightWhite)
                Text("Welcome, $username", fontSize = 13.sp, color = PeachGlow)
            }
        },
        actions = {
            IconButton(onClick = onLogout) {
                Icon(Icons.Filled.ExitToApp, contentDescription = "Logout", tint = BrightWhite)
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = BrightWhite.copy(alpha = 0f)),
        modifier = Modifier
            .background(gradientBrush)
            .height(60.dp)
    )
}
