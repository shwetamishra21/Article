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
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FirebaseApp.initializeApp(this)
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
    val auth = remember { FirebaseAuth.getInstance() }

    var isLoggedIn by remember { mutableStateOf(auth.currentUser != null) }
    var userRole by remember { mutableStateOf("member") } // fail-safe default

    if (!isLoggedIn) {

        LoginScreen(
            onLoginSuccess = { role ->
                userRole = role
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

                composable("home") {
                    HomeScreen(role = userRole)
                }

                composable("search") {
                    SearchScreen()
                }

                composable("inbox") {
                    InboxScreen()
                }

                composable("profile") {
                    ProfileScreen(
                        role = userRole,
                        onLogout = {
                            auth.signOut()
                            isLoggedIn = false
                        }
                    )

                }

                composable("requests") {
                    RequestsScreen(
                        onCreateNew = {
                            navController.navigate("request_form")
                        }
                    )
                }

                composable("request_form") {
                    RequestFormScreen(
                        onCancel = { navController.popBackStack() },
                        onSubmit = { navController.popBackStack() }
                    )
                }

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
