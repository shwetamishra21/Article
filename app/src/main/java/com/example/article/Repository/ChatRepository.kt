package com.example.article.chat

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

object ChatRepository {

    private const val TAG = "ChatRepository"
    private val firestore = FirebaseFirestore.getInstance()

    // ==================== CHAT CREATION ====================

    /**
     * Create or get existing chat between two users
     */
    suspend fun getOrCreateChat(
        userId1: String,
        userId2: String,
        user1Name: String,
        user2Name: String,
        user1Photo: String = "",
        user2Photo: String = "",
        type: String = ChatThread.TYPE_MEMBER,
        serviceRequestId: String? = null
    ): Result<String> {
        return try {
            // Create consistent chat ID (sorted UIDs)
            val chatId = if (userId1 < userId2) {
                "${userId1}_${userId2}"
            } else {
                "${userId2}_${userId1}"
            }

            val chatRef = firestore.collection("chats").document(chatId)
            val existingChat = chatRef.get().await()

            if (existingChat.exists()) {
                Log.d(TAG, "Chat already exists: $chatId")
                return Result.success(chatId)
            }

            // Create new chat
            val chatData = hashMapOf(
                "id" to chatId,
                "participants" to listOf(userId1, userId2),
                "participantNames" to mapOf(
                    userId1 to user1Name,
                    userId2 to user2Name
                ),
                "participantPhotos" to mapOf(
                    userId1 to user1Photo,
                    userId2 to user2Photo
                ),
                "type" to type,
                "title" to if (type == ChatThread.TYPE_SERVICE) "Service Chat" else "",
                "lastMessage" to "",
                "lastMessageSenderId" to "",
                "lastMessageAt" to Timestamp.now(),
                "unreadCounts" to mapOf(
                    userId1 to 0,
                    userId2 to 0
                ),
                "typingUsers" to emptyList<String>(),
                "createdAt" to Timestamp.now(),
                "serviceRequestId" to serviceRequestId
            )

            chatRef.set(chatData).await()
            Log.d(TAG, "Created new chat: $chatId")

            Result.success(chatId)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating chat", e)
            Result.failure(e)
        }
    }

    // ==================== REAL-TIME INBOX ====================

    /**
     * Observe user's inbox in real-time
     */
    fun observeInbox(userId: String): Flow<List<ChatThread>> = callbackFlow {
        val listener = firestore.collection("chats")
            .whereArrayContains("participants", userId)
            .orderBy("lastMessageAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error observing inbox", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val chats = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        ChatThread(
                            id = doc.id,
                            participants = (doc.get("participants") as? List<*>)
                                ?.mapNotNull { it as? String } ?: emptyList(),
                            participantNames = (doc.get("participantNames") as? Map<*, *>)
                                ?.mapKeys { it.key.toString() }
                                ?.mapValues { it.value.toString() } ?: emptyMap(),
                            participantPhotos = (doc.get("participantPhotos") as? Map<*, *>)
                                ?.mapKeys { it.key.toString() }
                                ?.mapValues { it.value.toString() } ?: emptyMap(),
                            type = doc.getString("type") ?: ChatThread.TYPE_MEMBER,
                            title = doc.getString("title") ?: "",
                            lastMessage = doc.getString("lastMessage") ?: "",
                            lastMessageSenderId = doc.getString("lastMessageSenderId") ?: "",
                            lastMessageAt = doc.getTimestamp("lastMessageAt") ?: Timestamp.now(),
                            unreadCounts = (doc.get("unreadCounts") as? Map<*, *>)
                                ?.mapKeys { it.key.toString() }
                                ?.mapValues { (it.value as? Long)?.toInt() ?: 0 } ?: emptyMap(),
                            typingUsers = (doc.get("typingUsers") as? List<*>)
                                ?.mapNotNull { it as? String } ?: emptyList(),
                            createdAt = doc.getTimestamp("createdAt") ?: Timestamp.now(),
                            serviceRequestId = doc.getString("serviceRequestId"),
                            serviceType = doc.getString("serviceType"),
                            serviceStatus = doc.getString("serviceStatus")
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing chat ${doc.id}", e)
                        null
                    }
                } ?: emptyList()

                trySend(chats)
            }

        awaitClose { listener.remove() }
    }

    // ==================== REAL-TIME MESSAGES ====================

    /**
     * Observe messages in a chat
     */
    fun observeMessages(chatId: String): Flow<List<ChatMessage>> = callbackFlow {
        val listener = firestore.collection("chats")
            .document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error observing messages", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val messages = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        ChatMessage(
                            id = doc.id,
                            chatId = chatId,
                            senderId = doc.getString("senderId") ?: "",
                            senderName = doc.getString("senderName") ?: "",
                            senderPhotoUrl = doc.getString("senderPhotoUrl") ?: "",
                            text = doc.getString("text") ?: "",
                            timestamp = doc.getTimestamp("timestamp") ?: Timestamp.now(),
                            readBy = (doc.get("readBy") as? List<*>)
                                ?.mapNotNull { it as? String } ?: emptyList(),
                            deliveredTo = (doc.get("deliveredTo") as? List<*>)
                                ?.mapNotNull { it as? String } ?: emptyList(),
                            imageUrl = doc.getString("imageUrl"),
                            isSystemMessage = doc.getBoolean("isSystemMessage") ?: false,
                            systemMessageType = doc.getString("systemMessageType")
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing message ${doc.id}", e)
                        null
                    }
                } ?: emptyList()

                trySend(messages)
            }

        awaitClose { listener.remove() }
    }

    // ==================== SEND MESSAGE ====================

    /**
     * Send a message in a chat
     */
    suspend fun sendMessage(
        chatId: String,
        senderId: String,
        senderName: String,
        senderPhotoUrl: String,
        text: String,
        recipientId: String
    ): Result<String> {
        return try {
            val messageId = firestore.collection("chats")
                .document(chatId)
                .collection("messages")
                .document().id

            val message = hashMapOf(
                "id" to messageId,
                "chatId" to chatId,
                "senderId" to senderId,
                "senderName" to senderName,
                "senderPhotoUrl" to senderPhotoUrl,
                "text" to text.trim(),
                "timestamp" to Timestamp.now(),
                "readBy" to listOf(senderId), // Sender has read their own message
                "deliveredTo" to emptyList<String>(),
                "isSystemMessage" to false
            )

            // Batch write: message + chat update
            firestore.runBatch { batch ->
                // Add message
                batch.set(
                    firestore.collection("chats")
                        .document(chatId)
                        .collection("messages")
                        .document(messageId),
                    message
                )

                // Update chat metadata
                batch.update(
                    firestore.collection("chats").document(chatId),
                    mapOf(
                        "lastMessage" to text.trim(),
                        "lastMessageSenderId" to senderId,
                        "lastMessageAt" to Timestamp.now(),
                        "unreadCounts.$recipientId" to FieldValue.increment(1)
                    )
                )
            }.await()

            Log.d(TAG, "Message sent successfully: $messageId")
            Result.success(messageId)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending message", e)
            Result.failure(e)
        }
    }

    // ==================== MARK AS READ ====================

    /**
     * Mark all unread messages in a chat as read (convenience function)
     * Use this when user opens a chat from the inbox
     */
    suspend fun markChatAsRead(
        chatId: String,
        userId: String
    ): Result<Unit> {
        return try {
            // Get all unread messages for this user
            val messages = firestore.collection("chats")
                .document(chatId)
                .collection("messages")
                .get()
                .await()

            val unreadMessageIds = messages.documents
                .mapNotNull { doc ->
                    val readBy = (doc.get("readBy") as? List<*>)
                        ?.mapNotNull { it as? String } ?: emptyList()
                    if (!readBy.contains(userId)) doc.id else null
                }

            if (unreadMessageIds.isEmpty()) {
                // No unread messages, just reset the count
                firestore.collection("chats")
                    .document(chatId)
                    .update(mapOf("unreadCounts.$userId" to 0))
                    .await()
                return Result.success(Unit)
            }

            // Mark messages as read
            return markMessagesAsRead(chatId, userId, unreadMessageIds)
        } catch (e: Exception) {
            Log.e(TAG, "Error marking chat as read", e)
            Result.failure(e)
        }
    }

    /**
     * Mark specific messages as read
     */
    suspend fun markMessagesAsRead(
        chatId: String,
        userId: String,
        messageIds: List<String>
    ): Result<Unit> {
        return try {
            if (messageIds.isEmpty()) return Result.success(Unit)

            firestore.runBatch { batch ->
                // Update all unread messages
                messageIds.forEach { messageId ->
                    batch.update(
                        firestore.collection("chats")
                            .document(chatId)
                            .collection("messages")
                            .document(messageId),
                        mapOf("readBy" to FieldValue.arrayUnion(userId))
                    )
                }

                // Reset unread count
                batch.update(
                    firestore.collection("chats").document(chatId),
                    mapOf("unreadCounts.$userId" to 0)
                )
            }.await()

            Log.d(TAG, "Marked ${messageIds.size} messages as read")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error marking messages as read", e)
            Result.failure(e)
        }
    }

    // ==================== TYPING INDICATORS ====================

    /**
     * Update typing status
     */
    suspend fun setTyping(
        chatId: String,
        userId: String,
        isTyping: Boolean
    ): Result<Unit> {
        return try {
            val update = if (isTyping) {
                mapOf("typingUsers" to FieldValue.arrayUnion(userId))
            } else {
                mapOf("typingUsers" to FieldValue.arrayRemove(userId))
            }

            firestore.collection("chats")
                .document(chatId)
                .update(update)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating typing status", e)
            Result.failure(e)
        }
    }

    // ==================== DELETE CHAT ====================

    /**
     * Delete a chat (for cleanup)
     */
    suspend fun deleteChat(chatId: String): Result<Unit> {
        return try {
            // Delete all messages first
            val messages = firestore.collection("chats")
                .document(chatId)
                .collection("messages")
                .get()
                .await()

            firestore.runBatch { batch ->
                messages.documents.forEach { doc ->
                    batch.delete(doc.reference)
                }
                batch.delete(firestore.collection("chats").document(chatId))
            }.await()

            Log.d(TAG, "Chat deleted: $chatId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting chat", e)
            Result.failure(e)
        }
    }
}