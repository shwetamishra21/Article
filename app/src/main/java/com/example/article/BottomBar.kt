package com.example.article

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState

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
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // ← FIXED: 5 tabs instead of 6
    val items = when (role) {
        UserRole.MEMBER -> listOf(
            BottomNavItem("Home", "home", Icons.Default.Home),
            BottomNavItem("Search", "search", Icons.Default.Search),
            BottomNavItem("Services", "requests", Icons.Default.Build), // ← Services combines requests
            BottomNavItem("Inbox", "inbox", Icons.Default.Chat),
            BottomNavItem("Profile", "profile", Icons.Default.Person)
        )

        UserRole.SERVICE_PROVIDER -> listOf(
            BottomNavItem("Home", "home", Icons.Default.Home),
            BottomNavItem("Search", "search", Icons.Default.Search),
            BottomNavItem("Inbox", "inbox", Icons.Default.Chat),
            BottomNavItem("Profile", "profile", Icons.Default.Person)
        )

        UserRole.ADMIN -> listOf(
            BottomNavItem("Home", "home", Icons.Default.Home),
            BottomNavItem("Search", "search", Icons.Default.Search),
            BottomNavItem("Post", "new_post", Icons.Default.AddCircle),
            BottomNavItem("Inbox", "inbox", Icons.Default.Chat),
            BottomNavItem("Profile", "profile", Icons.Default.Person)
        )
    }

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp
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
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (currentRoute == item.route)
                            FontWeight.SemiBold
                        else
                            FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
