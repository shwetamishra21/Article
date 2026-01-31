package com.example.article.Repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

object PostRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun toggleLike(
        postId: String,
        isLiked: Boolean,
        onError: (String) -> Unit = {}
    ) {
        val uid = auth.currentUser?.uid ?: return
        val postRef = firestore.collection("posts").document(postId)

        firestore.runBatch { batch ->
            if (isLiked) {
                batch.update(postRef, mapOf(
                    "likes" to FieldValue.increment(1),
                    "likedBy" to FieldValue.arrayUnion(uid)
                ))
            } else {
                batch.update(postRef, mapOf(
                    "likes" to FieldValue.increment(-1),
                    "likedBy" to FieldValue.arrayRemove(uid)
                ))
            }
        }.addOnFailureListener {
            onError(it.localizedMessage ?: "Failed to update like")
        }
    }
}
