package com.example.article

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.article.UserRole

data class BottomNavItem(
    val label: String,
    val route: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@Composable
fun BottomBar(
    navController: NavController,
    role: UserRole
) {

    /* ---------- ALL POSSIBLE ITEMS ---------- */

    val allItems = listOf(
        BottomNavItem("Home", "home", Icons.Default.Home),
        BottomNavItem("Search", "search", Icons.Default.Search),
        BottomNavItem("Requests", "requests", Icons.Default.Build),
        BottomNavItem("Post", "new_post", Icons.Default.AddCircle),
        BottomNavItem("Inbox", "inbox", Icons.Default.Chat),
        BottomNavItem("Profile", "profile", Icons.Default.Person)
    )

    /* ---------- ROLE FILTERING ---------- */

    val items = when (role) {
        UserRole.MEMBER -> allItems

        UserRole.SERVICE_PROVIDER -> allItems.filter {
            it.route in listOf("home", "search", "inbox", "profile")
        }

        UserRole.ADMIN -> allItems.filter {
            it.route in listOf("home", "search", "new_post", "inbox", "profile")
        }
    }

    /* ---------- STATE ---------- */

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val isDark = isSystemInDarkTheme()

    val containerColor = if (isDark)
        Color(0xFF0D1B2A)
    else
        Color(0xFFE3F2FD)

    val activeColor = if (isDark)
        Color(0xFF90CAF9)
    else
        Color(0xFF1565C0)

    val inactiveColor = if (isDark)
        Color(0xFFB0BEC5)
    else
        Color(0xFF607D8B)

    /* ---------- UI ---------- */

    NavigationBar(
        containerColor = containerColor,
        tonalElevation = 8.dp
    ) {
        items.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.route,
                onClick = {
                    if (currentRoute != item.route) {
                        navController.navigate(item.route) {
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
                        imageVector = item.icon,
                        contentDescription = item.label
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = activeColor,
                    selectedTextColor = activeColor,
                    indicatorColor = activeColor.copy(alpha = 0.15f),
                    unselectedIconColor = inactiveColor,
                    unselectedTextColor = inactiveColor
                )
            )
        }
    }
}
