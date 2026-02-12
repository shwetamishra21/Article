package com.example.article

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.article.ui.screens.LoginScreen
import com.example.article.ui.theme.ArticleTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d("MainActivity", "onCreate called")

        FirebaseApp.initializeApp(this)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Load profile if user is already logged in
        checkAndLoadProfile()

        setContent {
            ArticleTheme {
                ArticleApp()
            }
        }
    }

    private fun checkAndLoadProfile() {
        val auth = FirebaseAuth.getInstance()
        val firestore = FirebaseFirestore.getInstance()
        val currentUser = auth.currentUser

        Log.d("MainActivity", "checkAndLoadProfile - current user: ${currentUser?.email}")

        if (currentUser != null) {
            lifecycleScope.launch {
                try {
                    Log.d("MainActivity", "Loading profile for UID: ${currentUser.uid}")
                    val result = UserSessionManager.loadUserProfile(currentUser.uid, firestore)

                    if (result.isSuccess) {
                        Log.d("MainActivity", "Profile loaded successfully in checkAndLoadProfile")
                    } else {
                        Log.e("MainActivity", "Profile load failed: ${result.exceptionOrNull()?.message}")
                        auth.signOut()
                        UserSessionManager.clearSession()
                    }
                } catch (e: Exception) {
                    Log.e("MainActivity", "Exception loading profile", e)
                    auth.signOut()
                    UserSessionManager.clearSession()
                }
            }
        } else {
            Log.d("MainActivity", "No current user, clearing session")
            UserSessionManager.clearSession()
        }
    }
}

@Composable
fun ArticleApp() {
    val navController = rememberNavController()
    val auth = remember { FirebaseAuth.getInstance() }
    val firestore = remember { FirebaseFirestore.getInstance() }

    // Observe session state
    val currentUser by UserSessionManager.currentUser.collectAsState()
    val isLoading by UserSessionManager.isLoading.collectAsState()

    // Determine if user is logged in
    val isLoggedIn = currentUser != null
    val userRole = UserRole.from(currentUser?.role ?: "member")
    val userNeighborhood = currentUser?.neighbourhood ?: "Your Neighborhood"

    // Log state changes
    LaunchedEffect(currentUser, isLoading, isLoggedIn) {
        Log.d("ArticleApp", "=== STATE UPDATE ===")
        Log.d("ArticleApp", "isLoading: $isLoading")
        Log.d("ArticleApp", "currentUser: ${currentUser?.email}")
        Log.d("ArticleApp", "isLoggedIn: $isLoggedIn")
        Log.d("ArticleApp", "userRole: $userRole")
    }

    // Show loading while profile loads
    if (isLoading) {
        Log.d("ArticleApp", "Showing loading screen")
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    // Route based on login state
    if (!isLoggedIn) {
        Log.d("ArticleApp", "Showing LoginScreen (not logged in)")

        LoginScreen(
            auth = auth,
            firestore = firestore,
            onLoginSuccess = {
                Log.d("ArticleApp", "onLoginSuccess callback triggered!")
                // State update happens automatically through UserSessionManager
                // This will trigger recomposition
            }
        )
    } else {
        Log.d("ArticleApp", "Showing main app (logged in)")

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
                composable("home") {
                    Log.d("ArticleApp", "Rendering HomeScreen")
                    HomeScreen(
                        navController = navController,
                        userNeighborhood = userNeighborhood
                    )
                }

                composable("search") {
                    SearchScreen()
                }

                composable("inbox") {
                    InboxScreen(
                        navController = navController,
                        onCreateRequest = {
                            navController.navigate("request_form")
                        }
                    )
                }

                composable("profile") {
                    ProfileScreen(
                        role = userRole,
                        onLogout = {
                            Log.d("ArticleApp", "Logout triggered")
                            auth.signOut()
                            UserSessionManager.clearSession()
                        },
                        onCreatePost = {
                            if (userRole != UserRole.SERVICE_PROVIDER) {
                                navController.navigate("new_post")
                            }
                        }
                    )
                }

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

                composable("comments/{postId}/{postAuthorId}") { backStack ->
                    val postId = backStack.arguments?.getString("postId") ?: return@composable
                    val postAuthorId = backStack.arguments?.getString("postAuthorId") ?: return@composable

                    CommentScreen(
                        postId = postId,
                        postAuthorId = postAuthorId,
                        onBack = { navController.popBackStack() }
                    )
                }

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