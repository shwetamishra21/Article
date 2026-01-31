package com.example.article.Repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

object LikeRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth get() = FirebaseAuth.getInstance()

    fun toggleLike(
        postId: String,
        isCurrentlyLiked: Boolean,
        onError: (String) -> Unit = {}
    ) {
        val uid = auth.currentUser?.uid ?: return
        val postRef = db.collection("posts").document(postId)

        db.runTransaction { tx ->
            val snap = tx.get(postRef)

            val likedBy =
                snap.get("likedBy") as? Map<*, *> ?: emptyMap<Any, Any>()

            if (isCurrentlyLiked) {
                // UNLIKE
                if (likedBy.containsKey(uid)) {
                    tx.update(
                        postRef,
                        mapOf(
                            "likes" to FieldValue.increment(-1),
                            "likedBy.$uid" to FieldValue.delete()
                        )
                    )
                }
            } else {
                // LIKE (dedupe-safe)
                if (!likedBy.containsKey(uid)) {
                    tx.update(
                        postRef,
                        mapOf(
                            "likes" to FieldValue.increment(1),
                            "likedBy.$uid" to true
                        )
                    )
                }
            }
        }.addOnFailureListener {
            onError(it.localizedMessage ?: "Like failed")
        }
    }
}
