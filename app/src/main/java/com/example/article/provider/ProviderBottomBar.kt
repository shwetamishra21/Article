package com.example.article.provider

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.article.ui.theme.*

@Composable
fun ProviderBottomBar(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(
        modifier = Modifier
            .height(70.dp)
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                spotColor = BluePrimary.copy(alpha = 0.3f)
            ),
        containerColor = SurfaceLight,
        tonalElevation = 0.dp
    ) {
        // Home (Requests)
        NavigationBarItem(
            selected = currentRoute == "provider_home",
            onClick = {
                navController.navigate("provider_home") {
                    popUpTo("provider_home") { inclusive = true }
                }
            },
            icon = {
                Icon(
                    Icons.Default.Assignment,
                    contentDescription = "Requests",
                    modifier = Modifier.height(24.dp)
                )
            },
            label = { Text("Requests", fontSize = 12.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = BluePrimary,
                selectedTextColor = BluePrimary,
                indicatorColor = BluePrimary.copy(alpha = 0.15f),
                unselectedIconColor = Color(0xFF999999),
                unselectedTextColor = Color(0xFF999999)
            )
        )

        // Inbox
        NavigationBarItem(
            selected = currentRoute == "provider_inbox",
            onClick = {
                navController.navigate("provider_inbox") {
                    launchSingleTop = true
                }
            },
            icon = {
                Icon(
                    Icons.Default.ChatBubble,
                    contentDescription = "Chats",
                    modifier = Modifier.height(24.dp)
                )
            },
            label = { Text("Chats", fontSize = 12.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = BluePrimary,
                selectedTextColor = BluePrimary,
                indicatorColor = BluePrimary.copy(alpha = 0.15f),
                unselectedIconColor = Color(0xFF999999),
                unselectedTextColor = Color(0xFF999999)
            )
        )

        // Profile
        NavigationBarItem(
            selected = currentRoute == "provider_profile",
            onClick = {
                navController.navigate("provider_profile") {
                    launchSingleTop = true
                }
            },
            icon = {
                Icon(
                    Icons.Default.Person,
                    contentDescription = "Profile",
                    modifier = Modifier.height(24.dp)
                )
            },
            label = { Text("Profile", fontSize = 12.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = BluePrimary,
                selectedTextColor = BluePrimary,
                indicatorColor = BluePrimary.copy(alpha = 0.15f),
                unselectedIconColor = Color(0xFF999999),
                unselectedTextColor = Color(0xFF999999)
            )
        )
    }
}