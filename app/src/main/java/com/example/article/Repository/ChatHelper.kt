package com.example.article.chat

import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Helper functions for chat functionality
 * Use these from anywhere in your app to start chats
 */
object ChatHelper {

    /**
     * Start a chat with another user (member-to-member)
     *
     * Usage:
     * ```
     * ChatHelper.startChatWith(
     *     navController = navController,
     *     scope = coroutineScope,
     *     otherUserId = "user123",
     *     otherUserName = "John Doe",
     *     otherUserPhoto = "https://..."
     * )
     * ```
     */
    fun startChatWith(
        navController: NavController,
        scope: CoroutineScope,
        otherUserId: String,
        otherUserName: String,
        otherUserPhoto: String = ""
    ) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val currentUserId = currentUser.uid
        val currentUserName = currentUser.displayName ?: currentUser.email ?: "You"
        val currentUserPhoto = currentUser.photoUrl?.toString() ?: ""

        scope.launch {
            val result = ChatRepository.getOrCreateChat(
                userId1 = currentUserId,
                userId2 = otherUserId,
                user1Name = currentUserName,
                user2Name = otherUserName,
                user1Photo = currentUserPhoto,
                user2Photo = otherUserPhoto,
                type = ChatThread.TYPE_MEMBER
            )

            result.fold(
                onSuccess = { chatId ->
                    // Simple navigation - chat screen will load user data
                    navController.navigate(
                        "chat/$chatId/$otherUserId/loading/loading"
                    )
                },
                onFailure = { error ->
                    // Handle error (e.g., show a toast)
                    android.util.Log.e("ChatHelper", "Failed to create chat", error)
                }
            )
        }
    }

    /**
     * Start a service-related chat (when provider accepts a request)
     *
     * Usage:
     * ```
     * ChatHelper.startServiceChat(
     *     navController = navController,
     *     scope = coroutineScope,
     *     providerId = "provider123",
     *     providerName = "Jane Smith",
     *     providerPhoto = "https://...",
     *     serviceRequestId = "request456"
     * )
     * ```
     */
    fun startServiceChat(
        navController: NavController,
        scope: CoroutineScope,
        providerId: String,
        providerName: String,
        providerPhoto: String = "",
        serviceRequestId: String
    ) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val currentUserId = currentUser.uid
        val currentUserName = currentUser.displayName ?: currentUser.email ?: "You"
        val currentUserPhoto = currentUser.photoUrl?.toString() ?: ""

        scope.launch {
            val result = ChatRepository.getOrCreateChat(
                userId1 = currentUserId,
                userId2 = providerId,
                user1Name = currentUserName,
                user2Name = providerName,
                user1Photo = currentUserPhoto,
                user2Photo = providerPhoto,
                type = ChatThread.TYPE_SERVICE,
                serviceRequestId = serviceRequestId
            )

            result.fold(
                onSuccess = { chatId ->
                    // Simple navigation - chat screen will load user data
                    navController.navigate(
                        "chat/$chatId/$providerId/loading/loading"
                    )
                },
                onFailure = { error ->
                    android.util.Log.e("ChatHelper", "Failed to create service chat", error)
                }
            )
        }
    }
}