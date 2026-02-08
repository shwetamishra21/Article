package com.example.article.Repository

import com.example.article.ChatThread
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

object ChatRepository {

    private val db = FirebaseFirestore.getInstance()

    /**
     * Real-time inbox stream for current user
     *
     * ✅ FIXED: Changed "members" to "participants" to match Firestore structure
     */
    fun observeInbox(userId: String): Flow<List<ChatThread>> = callbackFlow {

        val listener = db.collection("chats")
            .whereArrayContains("participants", userId)  // ✅ FIXED: was "members"
            .orderBy("lastMessageAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->

                if (error != null || snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val chats = snapshot.documents.mapNotNull { doc ->
                    try {
                        ChatThread(
                            id = doc.id,
                            title = doc.getString("title") ?: "Conversation",
                            lastMessage = doc.getString("lastMessage") ?: "",
                            updatedAt = doc.getLong("lastMessageAt") ?: 0L,
                            type = doc.getString("type") ?: "member"
                        )
                    } catch (e: Exception) {
                        null  // Skip malformed documents
                    }
                }

                trySend(chats)
            }

        awaitClose { listener.remove() }
    }
}