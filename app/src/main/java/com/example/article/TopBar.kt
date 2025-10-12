package com.example.article

import androidx.compose.animation.core.*
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.article.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(username: String, onLogout: () -> Unit) {
    val gradientBrush = Brush.horizontalGradient(
        colors = listOf(
            RoyalViolet,
            DeepPlum,
            RoyalViolet.copy(alpha = 0.9f)
        )
    )

    // Get current time
    val currentTime = remember {
        val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
        formatter.format(Date())
    }

    // Animation for notification icon
    val infiniteTransition = rememberInfiniteTransition(label = "notification_animation")
    val notificationScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "notification_scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(8.dp),
        colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
    ) {
        CenterAlignedTopAppBar(
            title = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Forge",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrightWhite
                    )
                    Text(
                        "Welcome back, ${username.split("@")[0].replaceFirstChar { it.uppercase() }}",
                        fontSize = 12.sp,
                        color = PeachGlow
                    )
                }
            },
            navigationIcon = {
                // Time display
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(BrightWhite.copy(alpha = 0.2f))
                        .padding(8.dp)
                ) {
                    Text(
                        text = currentTime,
                        color = BrightWhite,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            actions = {
                // Notification icon with animation
                IconButton(
                    onClick = { /* Handle notifications */ },
                    modifier = Modifier.scale(notificationScale)
                ) {
                    Box {
                        Icon(
                            Icons.Filled.Notifications,
                            contentDescription = "Notifications",
                            tint = BrightWhite
                        )
                        // Red dot indicator
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(androidx.compose.ui.graphics.Color.Red)
                                .align(Alignment.TopEnd)
                        )
                    }
                }

                // Logout button
                IconButton(onClick = onLogout) {
                    Icon(
                        Icons.Filled.ExitToApp,
                        contentDescription = "Logout",
                        tint = BrightWhite
                    )
                }
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = androidx.compose.ui.graphics.Color.Transparent
            ),
            modifier = Modifier
                .background(gradientBrush)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(20.dp))
        )
    }
}