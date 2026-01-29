package com.example.article.Repository

import com.example.article.FeedItem
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object FeedRepository {

    private val db = FirebaseFirestore.getInstance()

    suspend fun fetchFeed(): List<FeedItem> {
        val snapshot = db.collection("posts")
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            when (doc.getString("type")) {

                "announcement" -> FeedItem.Announcement(
                    id = doc.id,
                    title = doc.getString("title") ?: return@mapNotNull null,
                    message = doc.getString("content") ?: "",
                    time = doc.getLong("createdAt") ?: 0L
                )

                "post" -> FeedItem.Post(
                    id = doc.id,
                    author = doc.getString("authorName") ?: "Unknown",
                    content = doc.getString("content") ?: "",
                    time = doc.getLong("createdAt") ?: 0L,
                    likes = (doc.getLong("likes") ?: 0L).toInt(),
                    commentCount = (doc.getLong("commentCount") ?: 0L).toInt()
                )

                else -> null
            }
        }
    }
}
