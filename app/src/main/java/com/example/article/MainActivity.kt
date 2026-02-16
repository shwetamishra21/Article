package com.example.article

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.article.admin.AdminBottomBar
import com.example.article.admin.AdminDashboardScreen
import com.example.article.admin.AnnouncementManagementScreen
import com.example.article.admin.ContentModerationScreen
import com.example.article.admin.MemberManagementScreen
import com.example.article.admin.ProviderApprovalScreen
import com.example.article.provider.ProviderBottomBar
import com.example.article.provider.ProviderInboxScreen
import com.example.article.provider.ProviderProfileScreen
import com.example.article.provider.ProviderRequestsScreen
import com.example.article.chat.EnhancedChatScreen
import com.example.article.ui.screens.LoginScreen
import com.example.article.ui.theme.ArticleTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d("MainActivity", "onCreate called")

        FirebaseApp.initializeApp(this)
        WindowCompat.setDecorFitsSystemWindows(window, false)

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
                        Log.d("MainActivity", "Profile loaded successfully")
                    } else {
                        Log.e(
                            "MainActivity",
                            "Profile load failed: ${result.exceptionOrNull()?.message}"
                        )
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

    val currentUser by UserSessionManager.currentUser.collectAsState()
    val isLoading by UserSessionManager.isLoading.collectAsState()

    val isLoggedIn = currentUser != null
    val userRole = UserRole.from(currentUser?.role ?: "member")
    val userNeighborhood = currentUser?.neighbourhood ?: "Your Neighborhood"

    LaunchedEffect(currentUser, isLoading, isLoggedIn) {
        Log.d("ArticleApp", "=== STATE UPDATE ===")
        Log.d("ArticleApp", "isLoading: $isLoading")
        Log.d("ArticleApp", "currentUser: ${currentUser?.email}")
        Log.d("ArticleApp", "isLoggedIn: $isLoggedIn")
        Log.d("ArticleApp", "userRole: $userRole")
    }

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

    if (!isLoggedIn) {
        Log.d("ArticleApp", "Showing LoginScreen (not logged in)")
        LoginScreen(
            auth = auth,
            firestore = firestore,
            onLoginSuccess = {
                Log.d("ArticleApp", "onLoginSuccess callback triggered!")
            }
        )
    } else {
        when (userRole) {
            UserRole.SERVICE_PROVIDER -> {
                Log.d("ArticleApp", "Showing Service Provider UI")
                ProviderApp(navController, auth)
            }

            UserRole.MEMBER -> {
                Log.d("ArticleApp", "Showing Member UI")
                MemberApp(navController, userRole, userNeighborhood, auth)
            }

            UserRole.ADMIN -> {
                Log.d("ArticleApp", "Showing Admin UI")
                AdminApp(navController, userRole, userNeighborhood, auth)
            }
        }
    }
}

@Composable
private fun ProviderApp(
    navController: NavHostController,
    auth: FirebaseAuth
) {
    Scaffold(
        bottomBar = {
            ProviderBottomBar(navController = navController)
        }
    ) { innerPadding ->

        NavHost(
            navController = navController,
            startDestination = "provider_home",
            modifier = Modifier.padding(innerPadding)
        ) {

            // Provider Requests Screen (Wired with ViewModel)
            composable("provider_home") {
                ProviderRequestsScreen()
            }

            composable("provider_inbox") {
                ProviderInboxScreen(navController = navController)
            }

            // Provider Profile Screen (Wired with ViewModel)
            composable("provider_profile") {
                ProviderProfileScreen(
                    onLogout = {
                        Log.d("ProviderApp", "Logout triggered")
                        auth.signOut()
                        UserSessionManager.clearSession()
                    }
                )
            }

            // Enhanced Chat Screen
            composable(
                route = "chat/{chatId}/{otherUserId}/{otherUserName}/{otherUserPhoto}",
                arguments = listOf(
                    navArgument("chatId") { type = NavType.StringType },
                    navArgument("otherUserId") { type = NavType.StringType },
                    navArgument("otherUserName") { type = NavType.StringType },
                    navArgument("otherUserPhoto") {
                        type = NavType.StringType
                        defaultValue = ""
                    }
                )
            ) { backStackEntry ->
                EnhancedChatScreen(
                    navController = navController,
                    chatId = backStackEntry.arguments?.getString("chatId") ?: "",
                    otherUserId = backStackEntry.arguments?.getString("otherUserId") ?: "",
                    otherUserName = backStackEntry.arguments?.getString("otherUserName") ?: "",
                    otherUserPhoto = backStackEntry.arguments?.getString("otherUserPhoto") ?: ""
                )
            }

            composable("view_profile/{userId}/{role}") { backStack ->
                val userId = backStack.arguments?.getString("userId") ?: return@composable
                val role = backStack.arguments?.getString("role") ?: return@composable

                ViewProfileScreen(
                    navController = navController,
                    userId = userId,
                    userRole = role
                )
            }
        }
    }
}

@Composable
private fun MemberApp(
    navController: NavHostController,
    userRole: UserRole,
    userNeighborhood: String,
    auth: FirebaseAuth
) {
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
                SearchScreen(navController = navController)
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
                        Log.d("MemberApp", "Logout triggered")
                        auth.signOut()
                        UserSessionManager.clearSession()
                    },
                    onCreatePost = {
                        navController.navigate("new_post")
                    }
                )
            }

            // Service Requests Screen (Wired with ViewModel)
            composable("requests") {
                RequestsScreen(
                    onCreateNew = {
                        navController.navigate("request_form")
                    }
                )
            }

            // Request Form Screen (Wired with ViewModel)
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

            composable("comments/{postId}/{postAuthorId}") { backStack ->
                val postId = backStack.arguments?.getString("postId") ?: return@composable
                val postAuthorId =
                    backStack.arguments?.getString("postAuthorId") ?: return@composable

                CommentScreen(
                    postId = postId,
                    postAuthorId = postAuthorId,
                    onBack = { navController.popBackStack() }
                )
            }

            // Enhanced Chat Screen
            composable(
                route = "chat/{chatId}/{otherUserId}/{otherUserName}/{otherUserPhoto}",
                arguments = listOf(
                    navArgument("chatId") { type = NavType.StringType },
                    navArgument("otherUserId") { type = NavType.StringType },
                    navArgument("otherUserName") { type = NavType.StringType },
                    navArgument("otherUserPhoto") {
                        type = NavType.StringType
                        defaultValue = ""
                    }
                )
            ) { backStackEntry ->
                EnhancedChatScreen(
                    navController = navController,
                    chatId = backStackEntry.arguments?.getString("chatId") ?: "",
                    otherUserId = backStackEntry.arguments?.getString("otherUserId") ?: "",
                    otherUserName = backStackEntry.arguments?.getString("otherUserName") ?: "",
                    otherUserPhoto = backStackEntry.arguments?.getString("otherUserPhoto") ?: ""
                )
            }

            composable("view_profile/{userId}/{role}") { backStack ->
                val userId = backStack.arguments?.getString("userId") ?: return@composable
                val role = backStack.arguments?.getString("role") ?: return@composable

                ViewProfileScreen(
                    navController = navController,
                    userId = userId,
                    userRole = role
                )
            }
        }
    }
}

@Composable
private fun AdminApp(
    navController: NavHostController,
    userRole: UserRole,
    userNeighborhood: String,
    auth: FirebaseAuth
) {
    Scaffold(
        bottomBar = {
            AdminBottomBar(navController = navController)
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            // ============ MEMBER SCREENS (INHERITED) ============
            composable("home") {
                HomeScreen(
                    navController = navController,
                    userNeighborhood = userNeighborhood
                )
            }

            composable("search") {
                SearchScreen(navController = navController)
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
                        Log.d("AdminApp", "Logout triggered")
                        auth.signOut()
                        UserSessionManager.clearSession()
                    },
                    onCreatePost = {
                        navController.navigate("new_post")
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

            composable("comments/{postId}/{postAuthorId}") { backStack ->
                val postId = backStack.arguments?.getString("postId") ?: return@composable
                val postAuthorId =
                    backStack.arguments?.getString("postAuthorId") ?: return@composable

                CommentScreen(
                    postId = postId,
                    postAuthorId = postAuthorId,
                    onBack = { navController.popBackStack() }
                )
            }

            // Enhanced Chat Screen
            composable(
                route = "chat/{chatId}/{otherUserId}/{otherUserName}/{otherUserPhoto}",
                arguments = listOf(
                    navArgument("chatId") { type = NavType.StringType },
                    navArgument("otherUserId") { type = NavType.StringType },
                    navArgument("otherUserName") { type = NavType.StringType },
                    navArgument("otherUserPhoto") {
                        type = NavType.StringType
                        defaultValue = ""
                    }
                )
            ) { backStackEntry ->
                EnhancedChatScreen(
                    navController = navController,
                    chatId = backStackEntry.arguments?.getString("chatId") ?: "",
                    otherUserId = backStackEntry.arguments?.getString("otherUserId") ?: "",
                    otherUserName = backStackEntry.arguments?.getString("otherUserName") ?: "",
                    otherUserPhoto = backStackEntry.arguments?.getString("otherUserPhoto") ?: ""
                )
            }

            composable("view_profile/{userId}/{role}") { backStack ->
                val userId = backStack.arguments?.getString("userId") ?: return@composable
                val role = backStack.arguments?.getString("role") ?: return@composable

                ViewProfileScreen(
                    navController = navController,
                    userId = userId,
                    userRole = role
                )
            }

            // ============ ADMIN DASHBOARD (WIRED WITH VIEWMODELS) ============
            composable("admin_dashboard") {
                AdminDashboardScreen(
                    onNavigateToMembers = {
                        navController.navigate("member_management")
                    },
                    onNavigateToProviders = {
                        navController.navigate("provider_approval")
                    },
                    onNavigateToAnnouncements = {
                        navController.navigate("announcements")
                    },
                    onNavigateToModeration = {
                        navController.navigate("content_moderation")
                    }
                )
            }

            // Member Management Screen (Wired)
            composable("member_management") {
                MemberManagementScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Provider Approval Screen (Wired)
            composable("provider_approval") {
                ProviderApprovalScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Announcements Screen
            composable("announcements") {
                AnnouncementManagementScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Content Moderation Screen
            composable("content_moderation") {
                ContentModerationScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}