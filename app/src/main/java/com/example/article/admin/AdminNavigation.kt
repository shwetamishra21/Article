package com.example.article.admin

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

sealed class AdminRoute(val route: String) {
    object Dashboard : AdminRoute("admin_dashboard")
    object MemberManagement : AdminRoute("member_management")
    object ProviderApproval : AdminRoute("provider_approval")
    object Announcements : AdminRoute("announcements")
    object ContentModeration : AdminRoute("content_moderation")
}

@Composable
fun AdminNavigation(
    navController: NavHostController = rememberNavController(),
    onNavigateBack: () -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = AdminRoute.Dashboard.route
    ) {
        composable(AdminRoute.Dashboard.route) {
            AdminDashboardScreen(
                onNavigateToMembers = {
                    navController.navigate(AdminRoute.MemberManagement.route)
                },
                onNavigateToProviders = {
                    navController.navigate(AdminRoute.ProviderApproval.route)
                },
                onNavigateToAnnouncements = {
                    navController.navigate(AdminRoute.Announcements.route)
                },
                onNavigateToModeration = {
                    navController.navigate(AdminRoute.ContentModeration.route)
                }
            )
        }

        composable(AdminRoute.MemberManagement.route) {
            MemberManagementScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(AdminRoute.ProviderApproval.route) {
            ProviderApprovalScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(AdminRoute.Announcements.route) {
            AnnouncementManagementScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(AdminRoute.ContentModeration.route) {
            ContentModerationScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}