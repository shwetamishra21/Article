package com.example.article.Repository

import com.example.article.ChatThread
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object InboxRepository {


    suspend fun loadInbox(
        userId: String,
        type: String
    ): List<ChatThread> {

        val firestore = FirebaseFirestore.getInstance()

        val snapshot = firestore
            .collection("chats")
            .whereArrayContains("participants", userId)
            .orderBy("lastMessageAt")
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->

            val memberId = doc.getString("memberId") ?: return@mapNotNull null
            val providerId = doc.getString("providerId") ?: return@mapNotNull null
            val requestId = doc.getString("requestId") ?: ""

            ChatThread(
                id = doc.id,
                title = doc.getString("title") ?: "Conversation",
                lastMessage = doc.getString("lastMessage") ?: "",
                updatedAt = doc.getLong("updatedAt") ?: 0L,
                type = type
            )
        }
    }
}
