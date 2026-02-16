package com.example.article.Repository

import android.util.Log
import com.example.article.ChatMessage
import com.example.article.ChatThread
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

object ChatRepository {

    private val db = FirebaseFirestore.getInstance()
    private const val TAG = "ChatRepository"

    /**
     * REAL-TIME inbox with PROPER unread counts per user
     */
    fun observeInbox(userId: String): Flow<List<ChatThread>> = callbackFlow {
        Log.d(TAG, "🔥 Starting REAL-TIME inbox for user: $userId")

        val listener = db.collection("chats")
            .whereArrayContains("participants", userId)
            .orderBy("lastMessageAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->

                if (error != null) {
                    Log.e(TAG, "❌ Inbox error", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                if (snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val chats = snapshot.documents.mapNotNull { doc ->
                    try {
                        val participants = doc.get("participants") as? List<*>
                        val participantsList = participants?.mapNotNull { it as? String } ?: emptyList()

                        // V2: Get unread count from map
                        val unreadCounts = doc.get("unreadCounts") as? Map<*, *>
                        val unreadCount = (unreadCounts?.get(userId) as? Long)?.toInt() ?: 0

                        ChatThread(
                            id = doc.id,
                            title = doc.getString("title") ?: "Conversation",
                            lastMessage = doc.getString("lastMessage") ?: "",
                            lastMessageAt = doc.getLong("lastMessageAt") ?: System.currentTimeMillis(),
                            type = doc.getString("type") ?: "member",
                            participants = participantsList,
                            unreadCount = unreadCount,
                            lastMessageSenderId = doc.getString("lastMessageSenderId") ?: ""
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing chat: ${doc.id}", e)
                        null
                    }
                }

                Log.d(TAG, "✅ REAL-TIME UPDATE: ${chats.size} chats, total unread: ${chats.sumOf { it.unreadCount }}")
                trySend(chats)
            }

        awaitClose {
            Log.d(TAG, "Closing inbox listener")
            listener.remove()
        }
    }

    /**
     * REAL-TIME messages stream
     */
    fun observeMessages(chatId: String): Flow<List<ChatMessage>> = callbackFlow {
        Log.d(TAG, "🔥 Starting REAL-TIME messages for chat: $chatId")

        val listener = db.collection("chats")
            .document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->

                if (error != null) {
                    Log.e(TAG, "❌ Messages error", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                if (snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val messages = snapshot.documents.mapNotNull { doc ->
                    try {
                        ChatMessage(
                            id = doc.id,
                            text = doc.getString("text") ?: "",
                            senderId = doc.getString("senderId") ?: "",
                            timestamp = doc.getLong("timestamp") ?: 0L,
                            read = doc.getBoolean("read") ?: false
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing message: ${doc.id}", e)
                        null
                    }
                }

                Log.d(TAG, "✅ REAL-TIME UPDATE: ${messages.size} messages")
                trySend(messages)
            }

        awaitClose {
            Log.d(TAG, "Closing messages listener")
            listener.remove()
        }
    }

    /**
     * V2: Send message with BATCHED WRITE (atomic, no race conditions)
     */
    suspend fun sendMessage(
        chatId: String,
        text: String,
        senderId: String
    ): Result<String> {
        return try {
            Log.d(TAG, "📤 Sending message to chat: $chatId")

            val batch = db.batch()
            val chatRef = db.collection("chats").document(chatId)
            val messageRef = chatRef.collection("messages").document()
            val timestamp = System.currentTimeMillis()

            // 1. Add message to subcollection
            val messageData = mapOf(
                "text" to text,
                "senderId" to senderId,
                "timestamp" to timestamp,
                "read" to false,
                "type" to "text",
                "status" to "sent"  // ✅ NEW: Delivery status
            )
            batch.set(messageRef, messageData)

            // 2. Update chat metadata
            batch.update(chatRef, mapOf(
                "lastMessage" to text,
                "lastMessageAt" to timestamp,
                "lastMessageSenderId" to senderId
            ))

            // 3. Reset sender's unread count to 0
            batch.update(chatRef, "unreadCounts.$senderId", 0)

            // 4. Increment unread count for ALL other participants
            val chatSnapshot = chatRef.get().await()
            val participants = chatSnapshot.get("participants") as? List<*>
            val participantsList = participants?.mapNotNull { it as? String } ?: emptyList()

            participantsList.filter { it != senderId }.forEach { otherUser ->
                batch.update(chatRef, "unreadCounts.$otherUser", FieldValue.increment(1))
            }

            // COMMIT EVERYTHING ATOMICALLY
            batch.commit().await()

            Log.d(TAG, "✅ Message sent atomically: ${messageRef.id}")
            Result.success(messageRef.id)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Send failed", e)
            Result.failure(e)
        }
    }

    /**
     * ✅ NEW: Delete a single message
     */
    suspend fun deleteMessage(
        chatId: String,
        messageId: String,
        userId: String
    ): Result<Unit> {
        return try {
            Log.d(TAG, "🗑️ Deleting message: $messageId")

            val messageRef = db.collection("chats")
                .document(chatId)
                .collection("messages")
                .document(messageId)

            // Verify user is the sender
            val messageDoc = messageRef.get().await()
            val senderId = messageDoc.getString("senderId")

            if (senderId != userId) {
                return Result.failure(Exception("You can only delete your own messages"))
            }

            // Delete the message
            messageRef.delete().await()

            // Update chat's lastMessage if this was the last message
            val chatRef = db.collection("chats").document(chatId)
            val remainingMessages = chatRef.collection("messages")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()

            if (remainingMessages.isEmpty) {
                chatRef.update(
                    mapOf(
                        "lastMessage" to "",
                        "lastMessageAt" to System.currentTimeMillis()
                    )
                ).await()
            } else {
                val lastMsg = remainingMessages.documents.first()
                chatRef.update(
                    mapOf(
                        "lastMessage" to (lastMsg.getString("text") ?: ""),
                        "lastMessageAt" to (lastMsg.getLong("timestamp") ?: System.currentTimeMillis())
                    )
                ).await()
            }

            Log.d(TAG, "✅ Message deleted successfully")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Delete message failed", e)
            Result.failure(e)
        }
    }

    /**
     * ✅ NEW: Delete entire chat for a user
     */
    suspend fun deleteChat(chatId: String, userId: String): Result<Unit> {
        return try {
            Log.d(TAG, "🗑️ Deleting chat: $chatId for user: $userId")

            val chatRef = db.collection("chats").document(chatId)
            val chatDoc = chatRef.get().await()

            val participants = chatDoc.get("participants") as? List<*>
            val participantsList = participants?.mapNotNull { it as? String } ?: emptyList()

            if (participantsList.size <= 2) {
                // Only 2 users - delete everything
                val messagesSnapshot = chatRef.collection("messages").get().await()
                messagesSnapshot.documents.forEach { it.reference.delete().await() }
                chatRef.delete().await()
            } else {
                // Group chat - just remove user from participants
                chatRef.update(
                    "participants", FieldValue.arrayRemove(userId)
                ).await()
            }

            Log.d(TAG, "✅ Chat deleted successfully")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Delete chat failed", e)
            Result.failure(e)
        }
    }

    /**
     * V2: Mark chat as read - resets unread count and updates lastReadAt
     */
    suspend fun markChatAsRead(chatId: String, userId: String): Result<Unit> {
        return try {
            db.collection("chats")
                .document(chatId)
                .update(
                    mapOf(
                        "unreadCounts.$userId" to 0,
                        "lastReadAt.$userId" to System.currentTimeMillis()
                    )
                )
                .await()

            Log.d(TAG, "✅ Chat marked as read for user: $userId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Mark read failed", e)
            Result.failure(e)
        }
    }

    /**
     * Get or create chat between users (V2: includes unreadCounts)
     */
    suspend fun getOrCreateChat(
        currentUserId: String,
        otherUserId: String,
        otherUserName: String,
        type: String = "member"
    ): Result<String> {
        return try {
            // Check if chat exists
            val snapshot = db.collection("chats")
                .whereArrayContains("participants", currentUserId)
                .whereEqualTo("type", type)
                .get()
                .await()

            val existingChat = snapshot.documents.firstOrNull { doc ->
                val participants = doc.get("participants") as? List<*>
                participants?.contains(otherUserId) == true
            }

            if (existingChat != null) {
                Log.d(TAG, "✅ Found existing chat: ${existingChat.id}")
                return Result.success(existingChat.id)
            }

            // Create new chat with V2 structure
            val chatData = hashMapOf(
                "participants" to listOf(currentUserId, otherUserId),
                "title" to otherUserName,
                "type" to type,
                "lastMessage" to "",
                "lastMessageAt" to System.currentTimeMillis(),
                "lastMessageSenderId" to "",
                "unreadCounts" to mapOf(
                    currentUserId to 0,
                    otherUserId to 0
                ),
                "lastReadAt" to mapOf(
                    currentUserId to System.currentTimeMillis(),
                    otherUserId to System.currentTimeMillis()
                ),
                "createdAt" to System.currentTimeMillis()
            )

            val chatRef = db.collection("chats").add(chatData).await()
            Log.d(TAG, "✅ Created new chat: ${chatRef.id}")
            Result.success(chatRef.id)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Get/create chat failed", e)
            Result.failure(e)
        }
    }

    /**
     * Create service chat (V2: includes unreadCounts)
     */
    suspend fun createServiceChat(
        memberId: String,
        providerId: String,
        serviceRequestId: String,
        providerName: String
    ): Result<String> {
        return try {
            val chatData = hashMapOf(
                "participants" to listOf(memberId, providerId),
                "memberId" to memberId,
                "providerId" to providerId,
                "requestId" to serviceRequestId,
                "title" to "Chat with $providerName",
                "type" to "service",
                "lastMessage" to "",
                "lastMessageAt" to System.currentTimeMillis(),
                "lastMessageSenderId" to "",
                "unreadCounts" to mapOf(
                    memberId to 0,
                    providerId to 0
                ),
                "lastReadAt" to mapOf(
                    memberId to System.currentTimeMillis(),
                    providerId to System.currentTimeMillis()
                ),
                "createdAt" to System.currentTimeMillis()
            )

            val chatRef = db.collection("chats").add(chatData).await()
            Log.d(TAG, "✅ Service chat created: ${chatRef.id}")
            Result.success(chatRef.id)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Service chat failed", e)
            Result.failure(e)
        }
    }
}