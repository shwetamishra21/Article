package com.example.article

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

object CommentRepository {

    private val firestore = FirebaseFirestore.getInstance()

    fun addComment(
        postId: String,
        authorId: String,
        authorName: String,
        authorPhotoUrl: String,
        content: String,
        onComplete: () -> Unit,
        onError: (String) -> Unit
    ) {

        val postRef = firestore.collection("posts").document(postId)
        val commentRef = postRef.collection("comments").document()

        firestore.runBatch { batch ->

            batch.set(
                commentRef,
                mapOf(
                    "id" to commentRef.id,
                    "postId" to postId,
                    "authorId" to authorId,
                    "authorName" to authorName,
                    "authorPhotoUrl" to authorPhotoUrl,
                    "content" to content.trim(),
                    "createdAt" to Timestamp.now()
                )
            )

            batch.update(
                postRef,
                mapOf("commentCount" to FieldValue.increment(1))
            )

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
            batch.update(
                postRef,
                mapOf("commentCount" to FieldValue.increment(-1))
            )
        }.addOnFailureListener {
            onError(it.localizedMessage ?: "Failed to delete comment")
        }
    }
}
