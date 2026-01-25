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

    // UI-only auth state (as per your current setup)
    var isLoggedIn by remember { mutableStateOf(false) }

    if (!isLoggedIn) {

        /* ---------- LOGIN ---------- */
        LoginScreen(
            onLoginSuccess = {
                isLoggedIn = true
            }
        )

    } else {

        /* ---------- MAIN APP ---------- */
        Scaffold(
            topBar = { TopBar() },
            bottomBar = { BottomBar(navController) }
        ) { innerPadding ->

            NavHost(
                navController = navController,
                startDestination = "home",
                modifier = Modifier.padding(innerPadding)
            ) {

                /* ---------- HOME ---------- */
                composable("home") {
                    HomeScreen()
                }

                /* ---------- SEARCH ---------- */
                composable("search") {
                    SearchScreen()
                }

                /* ---------- INBOX ---------- */
                composable("inbox") {
                    InboxScreen()
                }

                /* ---------- PROFILE ---------- */
                composable("profile") {
                    ProfileScreen(
                        onLogout = {
                            isLoggedIn = false
                        }
                    )
                }

                /* ---------- REQUESTS ---------- */
                composable("requests") {
                    RequestsScreen(
                        onCreateNew = {
                            navController.navigate("request_form")
                        }
                    )
                }

                /* ---------- REQUEST FORM ---------- */
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

                /* ---------- NEW POST ---------- */
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
