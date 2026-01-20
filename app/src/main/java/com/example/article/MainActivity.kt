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

// ✅ Import NewPostScreen
import com.example.article.NewPostScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FirebaseApp.initializeApp(this)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent { ForgeApp() }
    }
}

@Composable
fun ForgeApp() {
    ForgeTheme {
        val navController = rememberNavController()
        val firebaseAuth = remember { FirebaseAuth.getInstance() }

        var isLoggedIn by remember { mutableStateOf(firebaseAuth.currentUser != null) }
        var name by remember { mutableStateOf(firebaseAuth.currentUser?.displayName ?: "") }

        if (!isLoggedIn) {
            LoginScreen(
                onLoginSuccess = { email ->
                    isLoggedIn = true
                    name = firebaseAuth.currentUser?.displayName ?: email
                }
            )
        } else {
            Scaffold(
                topBar = {
                    TopBar(
                        username = name,
                        onLogout = {
                            firebaseAuth.signOut()
                            isLoggedIn = false
                            name = ""
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
                            name = name,
                            onNameChange = { name = it },
                            onLogout = {
                                firebaseAuth.signOut()
                                isLoggedIn = false
                                name = ""
                            }
                        )
                    }
                    composable("new_post") {
                        // ✅ NewPostScreen reference works now
                        NewPostScreen(
                            onPostUploaded = {
                                navController.navigate("dashboard") {
                                    popUpTo("dashboard") { inclusive = true }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
