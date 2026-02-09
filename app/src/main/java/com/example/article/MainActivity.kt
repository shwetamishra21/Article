package com.example.article

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.article.Repository.ProfileViewModel
import com.example.article.ui.theme.ArticleTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Single, safe Cloudinary init using helper (internally uses BuildConfig)
        CloudinaryHelper.initIfNeeded(this)

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
    val profileViewModel: ProfileViewModel = viewModel()
    val scope = rememberCoroutineScope()

    var isLoggedIn by remember { mutableStateOf(auth.currentUser != null) }
    var userRole by remember { mutableStateOf(UserRole.MEMBER) }
    var userNeighborhood by remember { mutableStateOf("Your Neighborhood") }

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            scope.launch {
                userNeighborhood = profileViewModel.getUserNeighborhood()
            }
        }
    }

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
                composable("home") {
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
                            auth.signOut()
                            isLoggedIn = false
                            userRole = UserRole.MEMBER
                            userNeighborhood = "Your Neighborhood"
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
                composable("comments/{postId}") { backStack ->
                    val postId = backStack.arguments?.getString("postId") ?: return@composable
                    CommentScreen(
                        postId = postId,
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

            navController.addOnDestinationChangedListener { _, destination, _ ->
                if (destination.route == "home") {
                    scope.launch {
                        userNeighborhood = profileViewModel.getUserNeighborhood()
                    }
                }
            }
        }
    }
}
