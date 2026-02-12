package com.example.article.Repository

import com.example.article.FeedItem
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import com.google.firebase.auth.FirebaseAuth


object FeedRepository {

    private val db = FirebaseFirestore.getInstance()

    /* ---------- FETCH FEED (UNCHANGED) ---------- */

    suspend fun fetchFeed(): List<FeedItem> {
        val snapshot = db.collection("posts")
            .orderBy(
                "createdAt",
                com.google.firebase.firestore.Query.Direction.DESCENDING
            )
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

                "post" -> {
                    val currentUid = FirebaseAuth.getInstance().currentUser?.uid

                    val likedBy =
                        doc.get("likedBy") as? Map<*, *> ?: emptyMap<Any, Any>()

                    val likedByMe =
                        currentUid != null && likedBy.containsKey(currentUid)

                    FeedItem.Post(
                        id = doc.id,
                        author = doc.getString("authorName") ?: "Unknown",
                        authorId = doc.getString("authorId") ?: "",
                        content = doc.getString("content") ?: "",
                        time = doc.getLong("createdAt") ?: 0L,
                        likes = (doc.getLong("likes") ?: 0L).toInt(),
                        commentCount = (doc.getLong("commentCount") ?: 0L).toInt(),
                        likedByMe = likedByMe,
                        imageUrl = doc.getString("imageUrl")
                    )
                }


                else -> null
            }
        }
    }

    /* ---------- DELETE POST ---------- */

    suspend fun deletePost(postId: String) {
        db.collection("posts")
            .document(postId)
            .delete()
            .await()
    }

    /* ---------- DELETE ANNOUNCEMENT ---------- */

    suspend fun deleteAnnouncement(announcementId: String) {
        db.collection("posts")
            .document(announcementId)
            .delete()
            .await()
    }
}