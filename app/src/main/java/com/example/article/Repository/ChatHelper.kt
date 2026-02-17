package com.example.article.chat

import android.net.Uri
import android.util.Log
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Helper functions for chat functionality
 */
object ChatHelper {

    private const val TAG = "ChatHelper"

    /**
     * Start a chat with another user (member-to-member or member-to-provider)
     */
    fun startChatWith(
        navController: NavController,
        scope: CoroutineScope,
        otherUserId: String,
        otherUserName: String,
        otherUserPhoto: String = "",
        otherUserRole: String = "member"
    ) {
        val currentUser = FirebaseAuth.getInstance().currentUser

        Log.d(TAG, "Starting chat with: $otherUserName")

        if (currentUser == null) {
            Log.e(TAG, "Current user is null")
            return
        }

        val currentUserId = currentUser.uid

        scope.launch {
            try {
                // Get current user's details from Firestore
                val currentUserData = getUserData(currentUserId)

                val currentUserName = currentUserData["name"]
                    ?: currentUser.displayName
                    ?: currentUser.email
                    ?: "You"
                val currentUserPhoto = currentUserData["photoUrl"]
                    ?: currentUser.photoUrl?.toString()
                    ?: ""
                val currentUserRole = currentUserData["role"] ?: "member"

                val chatType = if (otherUserRole == "service_provider" || currentUserRole == "service_provider") {
                    ChatThread.TYPE_SERVICE
                } else {
                    ChatThread.TYPE_MEMBER
                }

                Log.d(TAG, "Creating chat...")

                val result = ChatRepository.getOrCreateChat(
                    userId1 = currentUserId,
                    userId2 = otherUserId,
                    user1Name = currentUserName,
                    user2Name = otherUserName,
                    user1Photo = currentUserPhoto,
                    user2Photo = otherUserPhoto,
                    user1Role = currentUserRole,
                    user2Role = otherUserRole,
                    type = chatType
                )

                result.fold(
                    onSuccess = { chatId ->
                        Log.d(TAG, "✅ Chat created: $chatId")

                        // URL encode and handle empty strings
                        val safePhoto = if (otherUserPhoto.isBlank()) "none" else Uri.encode(otherUserPhoto)
                        val safeName = Uri.encode(otherUserName)

                        val route = "chat/$chatId/$otherUserId/$safeName/$safePhoto"
                        Log.d(TAG, "Navigating to: $route")

                        navController.navigate(route)
                    },
                    onFailure = { error ->
                        Log.e(TAG, "❌ Failed to create chat", error)
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "❌ Exception in startChatWith", e)
            }
        }
    }

    /**
     * Start a service-related chat (when provider accepts a request)
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

        scope.launch {
            try {
                val currentUserData = getUserData(currentUserId)
                val currentUserName = currentUserData["name"]
                    ?: currentUser.displayName
                    ?: currentUser.email
                    ?: "You"
                val currentUserPhoto = currentUserData["photoUrl"]
                    ?: currentUser.photoUrl?.toString()
                    ?: ""
                val currentUserRole = currentUserData["role"] ?: "member"

                val result = ChatRepository.getOrCreateChat(
                    userId1 = currentUserId,
                    userId2 = providerId,
                    user1Name = currentUserName,
                    user2Name = providerName,
                    user1Photo = currentUserPhoto,
                    user2Photo = providerPhoto,
                    user1Role = currentUserRole,
                    user2Role = "service_provider",
                    type = ChatThread.TYPE_SERVICE,
                    serviceRequestId = serviceRequestId
                )

                result.fold(
                    onSuccess = { chatId ->
                        Log.d(TAG, "✅ Service chat created: $chatId")

                        // URL encode and handle empty strings
                        val safePhoto = if (providerPhoto.isBlank()) "none" else Uri.encode(providerPhoto)
                        val safeName = Uri.encode(providerName)

                        navController.navigate("chat/$chatId/$providerId/$safeName/$safePhoto")
                    },
                    onFailure = { error ->
                        Log.e(TAG, "❌ Failed to create service chat", error)
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "❌ Exception in startServiceChat", e)
            }
        }
    }

    /**
     * Navigate to existing chat from inbox
     */
    fun navigateToChat(
        navController: NavController,
        chat: ChatThread,
        currentUserId: String
    ) {
        val otherUserId = chat.getOtherUserId(currentUserId) ?: return
        val otherUserName = chat.getOtherUserName(currentUserId)
        val otherUserPhoto = chat.getOtherUserPhoto(currentUserId)

        Log.d(TAG, "Navigating to existing chat: ${chat.id}")

        // URL encode and handle empty strings
        val safePhoto = if (otherUserPhoto.isBlank()) "none" else Uri.encode(otherUserPhoto)
        val safeName = Uri.encode(otherUserName)

        navController.navigate("chat/${chat.id}/$otherUserId/$safeName/$safePhoto")
    }

    /**
     * Helper to get user data from Firestore
     */
    private suspend fun getUserData(userId: String): Map<String, String> {
        return try {
            val doc = FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .get()
                .await()

            mapOf(
                "name" to (doc.getString("name") ?: ""),
                "photoUrl" to (doc.getString("photoUrl") ?: ""),
                "role" to (doc.getString("role") ?: "member")
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user data", e)
            emptyMap()
        }
    }
}