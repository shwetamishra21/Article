package com.example.article.Repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

data class ChatThread(
    val id: String,
    val memberId: String,
    val providerId: String,
    val lastMessage: String,
    val lastMessageAt: Long,
    val requestId: String?
)

object ChatRepository {

    private val db = FirebaseFirestore.getInstance()

    /**
     * Real-time inbox stream for current user
     */
    fun observeInbox(userId: String): Flow<List<ChatThread>> = callbackFlow {

        val listener = db.collection("chats")
            .whereArrayContains("members", userId)
            .orderBy("lastMessageAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->

                if (error != null || snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val chats = snapshot.documents.mapNotNull { doc ->
                    ChatThread(
                        id = doc.id,
                        memberId = doc.getString("memberId") ?: return@mapNotNull null,
                        providerId = doc.getString("providerId") ?: return@mapNotNull null,
                        lastMessage = doc.getString("lastMessage") ?: "",
                        lastMessageAt = doc.getLong("lastMessageAt") ?: 0L,
                        requestId = doc.getString("requestId")
                    )
                }

                trySend(chats)
            }

        awaitClose { listener.remove() }
    }
}
