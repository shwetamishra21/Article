package com.example.article

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import com.example.article.ui.theme.*

sealed class BottomNavItem(val route: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Dashboard : BottomNavItem("dashboard", Icons.Filled.Home)
    object Search : BottomNavItem("search", Icons.Filled.Search)
    object Inbox : BottomNavItem("inbox", Icons.Filled.Email)
    object Profile : BottomNavItem("profile", Icons.Filled.Person)
}

@Composable
fun BottomBar(navController: NavController) {
    val items = listOf(
        BottomNavItem.Dashboard,
        BottomNavItem.Search,
        BottomNavItem.Inbox,
        BottomNavItem.Profile
    )

    val gradientBrush = Brush.horizontalGradient(
        colors = listOf(DeepPlum, RoyalViolet, SoftLilac)
    )

    NavigationBar(
        containerColor = Color.Transparent,
        tonalElevation = 10.dp,
        modifier = Modifier
            .background(gradientBrush)
            .fillMaxWidth()
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.route) },
                selected = currentRoute == item.route,
                onClick = {
                    if (currentRoute != item.route) {
                        navController.navigate(item.route) {
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = BrightWhite,
                    unselectedIconColor = PeachGlow,
                    indicatorColor = Color.Transparent
                ),
                alwaysShowLabel = false
            )
        }
    }
}
