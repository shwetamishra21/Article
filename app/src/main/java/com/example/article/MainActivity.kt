package com.example.article

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.article.ui.theme.ForgeTheme
import com.example.article.ui.theme.LavenderMist

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            ForgeApp()
        }
    }
}

@Composable
fun ForgeApp() {
    ForgeTheme {
        val navController = rememberNavController()
        var isLoggedIn by remember { mutableStateOf(false) }
        var username by remember { mutableStateOf("") }

        if (!isLoggedIn) {
            LoginScreen { email ->
                isLoggedIn = true
                username = email
            }
        } else {
            Scaffold(
                topBar = {
                    TopBar(
                        username = username,
                        onLogout = {
                            isLoggedIn = false
                            username = ""
                        }
                    )
                },
                bottomBar = { BottomBar(navController) },
                containerColor = LavenderMist,
                contentWindowInsets = WindowInsets(0)
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = "dashboard",
                    modifier = Modifier.padding(innerPadding)
                ) {
                    composable("dashboard") {
                        HomeScreen()
                    }
                    composable("search") {
                        SearchScreen()
                    }
                    composable("inbox") {
                        InboxScreen()
                    }
                    composable("profile") {
                        ProfileScreen(
                            username = username,
                            onUsernameChange = { username = it },
                            onLogout = {
                                isLoggedIn = false
                                username = ""
                            }
                        )
                    }
                }
            }
        }
    }
}