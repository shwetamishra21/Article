package com.example.article

import androidx.compose.material3.*
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(username: String, onLogout: () -> Unit) {
    val isDark = isSystemInDarkTheme()
    val background = if (isDark) Color(0xFF1A1A1A) else Color(0xFFFDFBF9)
    val textColor = if (isDark) Color(0xFFEAEAEA) else Color(0xFF1E1E1E)

    CenterAlignedTopAppBar(
        title = {
            Text(
                text = "Forge",
                color = textColor,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        },
        navigationIcon = {
            IconButton(onClick = { /* Future menu or drawer */ }) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "Notifications",
                    tint = textColor
                )
            }
        },
        actions = {
            Text(
                text = username.split("@").firstOrNull()?.replaceFirstChar { it.uppercase() } ?: "Guest",
                color = textColor,
                fontSize = 14.sp,
                modifier = Modifier.padding(end = 8.dp)
            )
            IconButton(onClick = onLogout) {
                Icon(
                    imageVector = Icons.Default.Logout,
                    contentDescription = "Logout",
                    tint = textColor
                )
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = background
        )
    )
}
