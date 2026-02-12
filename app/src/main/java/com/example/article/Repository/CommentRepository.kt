package com.example.article

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

object CommentRepository {

    private val firestore = FirebaseFirestore.getInstance()

    fun addComment(
        postId: String,
        authorId: String,
        author: String,
        text: String,
        onComplete: () -> Unit,
        onError: (String) -> Unit
    ) {
        val now = System.currentTimeMillis()

        val postRef = firestore.collection("posts").document(postId)
        val commentRef = postRef.collection("comments").document()

        firestore.runBatch { batch ->
            batch.set(
                commentRef,
                mapOf(
                    "authorId" to authorId,
                    "author" to author,
                    "text" to text,
                    "createdAt" to now
                )
            )

            // 🔢 Atomic, crash-safe increment
            batch.update(postRef, mapOf(
                "commentCount" to FieldValue.increment(1)
            ))
        }.addOnSuccessListener {
            onComplete()
        }.addOnFailureListener {
            onError(it.localizedMessage ?: "Failed to add comment")
        }
    }
    fun deleteComment(
        postId: String,
        commentId: String,
        onError: (String) -> Unit = {}
    ) {
        val postRef = firestore.collection("posts").document(postId)
        val commentRef = postRef.collection("comments").document(commentId)

        firestore.runBatch { batch ->
            batch.delete(commentRef)
            batch.update(postRef, mapOf(
                "commentCount" to FieldValue.increment(-1)
            ))
        }.addOnFailureListener {
            onError(it.localizedMessage ?: "Failed to delete comment")
        }
    }
}
