package com.example.article.admin

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.article.ui.theme.*

@Composable
fun AdminBottomBar(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Admin routes that should highlight the Admin tab
    val adminRoutes = listOf(
        "admin_dashboard",
        "member_management",
        "provider_approval",
        "announcements",
        "content_moderation"
    )

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
        // Home (Member Home - Admin inherits this)
        NavigationBarItem(
            selected = currentRoute == "home",
            onClick = {
                if (currentRoute != "home") {
                    navController.navigate("home") {
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
                    Icons.Default.Home,
                    contentDescription = "Home",
                    modifier = Modifier.height(24.dp)
                )
            },
            label = {
                Text(
                    "Home",
                    fontSize = 11.sp,
                    fontWeight = if (currentRoute == "home") FontWeight.SemiBold else FontWeight.Normal
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

        // Search (Member Search - Admin inherits this)
        NavigationBarItem(
            selected = currentRoute == "search",
            onClick = {
                if (currentRoute != "search") {
                    navController.navigate("search") {
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
                    Icons.Default.Search,
                    contentDescription = "Search",
                    modifier = Modifier.height(24.dp)
                )
            },
            label = {
                Text(
                    "Search",
                    fontSize = 11.sp,
                    fontWeight = if (currentRoute == "search") FontWeight.SemiBold else FontWeight.Normal
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

        // Inbox (Member Inbox - Admin inherits this)
        NavigationBarItem(
            selected = currentRoute == "inbox",
            onClick = {
                if (currentRoute != "inbox") {
                    navController.navigate("inbox") {
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
                    contentDescription = "Inbox",
                    modifier = Modifier.height(24.dp)
                )
            },
            label = {
                Text(
                    "Inbox",
                    fontSize = 11.sp,
                    fontWeight = if (currentRoute == "inbox") FontWeight.SemiBold else FontWeight.Normal
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

        // Admin Dashboard
        NavigationBarItem(
            selected = currentRoute in adminRoutes,
            onClick = {
                if (currentRoute !in adminRoutes) {
                    navController.navigate("admin_dashboard") {
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
                    Icons.Default.AdminPanelSettings,
                    contentDescription = "Admin",
                    modifier = Modifier.height(24.dp)
                )
            },
            label = {
                Text(
                    "Admin",
                    fontSize = 11.sp,
                    fontWeight = if (currentRoute in adminRoutes) FontWeight.SemiBold else FontWeight.Normal
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

        // Profile (Member Profile - Admin inherits this)
        NavigationBarItem(
            selected = currentRoute == "profile",
            onClick = {
                if (currentRoute != "profile") {
                    navController.navigate("profile") {
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
                    modifier = Modifier.height(24.dp)
                )
            },
            label = {
                Text(
                    "Profile",
                    fontSize = 11.sp,
                    fontWeight = if (currentRoute == "profile") FontWeight.SemiBold else FontWeight.Normal
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