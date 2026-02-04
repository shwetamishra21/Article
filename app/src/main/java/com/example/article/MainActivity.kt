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
import com.example.article.provider.ProviderRequestsScreen
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
    var userRole by remember { mutableStateOf(UserRole.MEMBER) }

    /* ---------- AUTH GATE ---------- */

    if (!isLoggedIn) {
        LoginScreen(
            onLoginSuccess = { roleString ->
                userRole = UserRole.from(roleString)
                isLoggedIn = true
            }
        )
    } else {
        Scaffold(
            bottomBar = {
                BottomBar(
                    navController = navController,
                    role = userRole
                )
            }
        ) { innerPadding ->

            NavHost(
                navController = navController,
                startDestination = "home",
                modifier = Modifier.padding(innerPadding)
            ) {

                /* ---------- HOME ---------- */
                composable("home") {
                    HomeScreen(navController = navController)
                }

                /* ---------- SEARCH ---------- */
                composable("search") {
                    SearchScreen()
                }

                /* ---------- INBOX ---------- */
                composable("inbox") {
                    InboxScreen(
                        navController = navController,
                        onCreateRequest = {
                            navController.navigate("request_form")
                        }
                    )
                }

                /* ---------- PROFILE (with Create Post navigation) ---------- */
                composable("profile") {
                    ProfileScreen(
                        role = userRole,
                        onLogout = {
                            auth.signOut()
                            isLoggedIn = false
                            userRole = UserRole.MEMBER
                        },
                        onCreatePost = {
                            // ✨ Navigate to new post screen from profile
                            if (userRole != UserRole.SERVICE_PROVIDER) {
                                navController.navigate("new_post")
                            }
                        }
                    )
                }

                /* ---------- MEMBER / ADMIN REQUESTS ---------- */
                composable("requests") {
                    if (userRole == UserRole.MEMBER || userRole == UserRole.ADMIN) {
                        RequestsScreen(
                            onCreateNew = {
                                navController.navigate("request_form")
                            }
                        )
                    }
                }

                composable("request_form") {
                    RequestFormScreen(
                        onCancel = { navController.popBackStack() },
                        onSubmit = { navController.popBackStack() }
                    )
                }

                /* ---------- PROVIDER ONLY ---------- */
                composable("provider_requests") {
                    if (userRole == UserRole.SERVICE_PROVIDER) {
                        ProviderRequestsScreen()
                    }
                }

                /* ---------- NEW POST (from Profile or direct) ---------- */
                composable("new_post") {
                    if (userRole != UserRole.SERVICE_PROVIDER) {
                        NewPostScreen(
                            onPostUploaded = {
                                navController.navigate("home") {
                                    popUpTo("home") { inclusive = true }
                                }
                            }
                        )
                    }
                }

                /* ---------- COMMENTS ---------- */
                composable("comments/{postId}") { backStack ->
                    val postId = backStack.arguments?.getString("postId") ?: return@composable

                    CommentScreen(
                        postId = postId,
                        onBack = { navController.popBackStack() }
                    )
                }

                /* ---------- CHAT ---------- */
                composable("chat/{chatId}/{title}") { backStack ->
                    val chatId = backStack.arguments?.getString("chatId") ?: return@composable
                    val title = backStack.arguments?.getString("title") ?: "Chat"

                    ChatScreen(
                        navController = navController,
                        chatId = chatId,
                        title = title
                    )
                }
            }
        }
    }
}