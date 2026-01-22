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
import com.example.article.ui.theme.ArticleTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            ArticleTheme {
                ArticleApp()
            }
        }
    }
}

@Composable
fun ArticleApp() {
    val navController = rememberNavController()

    // UI-only login state
    var isLoggedIn by remember { mutableStateOf(false) }

    if (!isLoggedIn) {
        LoginScreen(
            onLoginSuccess = {
                isLoggedIn = true
            }
        )
    } else {
        Scaffold(
            topBar = { TopBar() },
            bottomBar = { BottomBar(navController) }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = "home",
                modifier = Modifier.padding(innerPadding)
            ) {
                composable("home") { HomeScreen() }
                composable("search") { SearchScreen() }
                composable("inbox") { InboxScreen() }
                composable("profile") {
                    ProfileScreen(
                        onLogout = { isLoggedIn = false }
                    )
                }
                composable("requests") {
                    RequestsScreen(
                        onCreateNew = {
                            // next screen later: request_form
                        }
                    )
                }
                composable("request_form") {
                    RequestFormScreen(
                        onCancel = {
                            navController.popBackStack()
                        },
                        onSubmit = {
                            navController.popBackStack()
                        }
                    )
                }



                // ✅ THIS WAS MISSING — ADD IT
                composable("new_post") {
                    NewPostScreen(
                        onPostUploaded = {
                            navController.navigate("home") {
                                popUpTo("home") { inclusive = true }
                            }
                        }
                    )
                }
            }

        }
    }
}
