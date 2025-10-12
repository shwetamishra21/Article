package com.example.article

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.article.ui.theme.ForgeTheme
import com.example.article.ui.theme.LavenderMist
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        FirebaseApp.initializeApp(this)

        // Edge-to-edge
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
        var isLoggedIn by remember { mutableStateOf(FirebaseAuth.getInstance().currentUser != null) }
        var username by remember { mutableStateOf(FirebaseAuth.getInstance().currentUser?.email ?: "") }

        if (!isLoggedIn) {
            LoginScreen(
                onLoginSuccess = { email ->
                    isLoggedIn = true
                    username = email
                }
            )
        } else {
            Scaffold(
                topBar = {
                    TopBar(
                        username = username,
                        onLogout = {
                            FirebaseAuth.getInstance().signOut()
                            isLoggedIn = false
                            username = ""
                        }
                    )
                },
                bottomBar = { BottomBar(navController) },
                containerColor = LavenderMist
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = "dashboard",
                    modifier = Modifier.padding(innerPadding)
                ) {
                    composable("dashboard") { HomeScreen() }
                    composable("search") { SearchScreen() }
                    composable("inbox") { InboxScreen() }
                    composable("profile") {
                        ProfileScreen(
                            username = username,
                            onUsernameChange = { username = it },
                            onLogout = {
                                FirebaseAuth.getInstance().signOut()
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
