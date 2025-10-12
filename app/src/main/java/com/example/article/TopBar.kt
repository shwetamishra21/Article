package com.example.article

import androidx.compose.foundation.background
import androidx.compose.material3.*
import androidx.compose.foundation.layout.height
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.article.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    username: String,
    onLogout: () -> Unit
) {
    val gradientBrush = Brush.horizontalGradient(
        colors = listOf(RoyalViolet, SoftLilac)
    )

    CenterAlignedTopAppBar(
        title = {
            Column {
                Text(
                    text = "Forge",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppWhite
                )
                Text(
                    text = "Welcome, $username",
                    fontSize = 12.sp,
                    color = LightIvory
                )
            }
        },
        actions = {
            IconButton(onClick = onLogout) {
                Icon(
                    imageVector = Icons.Filled.ExitToApp,
                    contentDescription = "Logout",
                    tint = AppWhite
                )
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = Color.Transparent
        ),
        modifier = Modifier
            .background(gradientBrush)
            .height(56.dp)
    )
}
