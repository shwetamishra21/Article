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
        val commentRef = firestore
            .collection("posts")
            .document(postId)
            .collection("comments")
            .document()

        val postRef = firestore
            .collection("posts")
            .document(postId)

        firestore.runBatch { batch ->
            batch.set(
                commentRef,
                mapOf(
                    "authorId" to authorId,
                    "author" to author,
                    "text" to text,
                    "createdAt" to System.currentTimeMillis()
                )
            )

            batch.update(
                postRef,
                "commentCount",
                FieldValue.increment(1)
            )
        }.addOnSuccessListener {
            onComplete()
        }.addOnFailureListener {
            onError(it.localizedMessage ?: "Failed to add comment")
        }
    }
}
