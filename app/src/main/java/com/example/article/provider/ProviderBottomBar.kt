package com.example.article.provider

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.article.ui.theme.*

@Composable
fun ProviderBottomBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(
        modifier = Modifier.height(70.dp),
        containerColor = SurfaceLight,
        tonalElevation = 3.dp
    ) {
        // Requests (Home for providers)
        NavigationBarItem(
            selected = currentRoute == "provider_home",
            onClick = {
                if (currentRoute != "provider_home") {
                    navController.navigate("provider_home") {
                        launchSingleTop = true
                        restoreState = true
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                    }
                }
            },
            icon = {
                Icon(
                    Icons.Default.Assignment,
                    contentDescription = "Requests",
                    modifier = Modifier.size(24.dp)
                )
            },
            label = {
                Text(
                    "Requests",
                    fontSize = 12.sp,
                    fontWeight = if (currentRoute == "provider_home") FontWeight.SemiBold else FontWeight.Normal
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = BluePrimary,
                selectedTextColor = BluePrimary,
                indicatorColor = BluePrimary.copy(alpha = 0.15f),
                unselectedIconColor = Color(0xFF999999),
                unselectedTextColor = Color(0xFF999999)
            )
        )

        // Chats/Inbox
        NavigationBarItem(
            selected = currentRoute == "provider_inbox",
            onClick = {
                if (currentRoute != "provider_inbox") {
                    navController.navigate("provider_inbox") {
                        launchSingleTop = true
                        restoreState = true
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                    }
                }
            },
            icon = {
                Icon(
                    Icons.Default.Chat,
                    contentDescription = "Chats",
                    modifier = Modifier.size(24.dp)
                )
            },
            label = {
                Text(
                    "Chats",
                    fontSize = 12.sp,
                    fontWeight = if (currentRoute == "provider_inbox") FontWeight.SemiBold else FontWeight.Normal
                )
            },
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
                if (currentRoute != "provider_profile") {
                    navController.navigate("provider_profile") {
                        launchSingleTop = true
                        restoreState = true
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                    }
                }
            },
            icon = {
                Icon(
                    Icons.Default.Person,
                    contentDescription = "Profile",
                    modifier = Modifier.size(24.dp)
                )
            },
            label = {
                Text(
                    "Profile",
                    fontSize = 12.sp,
                    fontWeight = if (currentRoute == "provider_profile") FontWeight.SemiBold else FontWeight.Normal
                )
            },
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