package com.example.article.Repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object UserRepository {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    fun ensureUserProfile(
        onComplete: (String) -> Unit,
        onError: () -> Unit = {}
    ) {
        val user = auth.currentUser ?: run {
            onError()
            return
        }

        val ref = db.collection("users").document(user.uid)

        ref.get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val role = doc.getString("role") ?: "member"
                    onComplete(role)
                } else {
                    val data = mapOf(
                        "uid" to user.uid,
                        "email" to user.email,
                        "role" to "member",
                        "name" to "",
                        "bio" to "",
                        "photoUrl" to "",
                        "neighbourhoodId" to "",
                        "createdAt" to System.currentTimeMillis()
                    )

                    ref.set(data)
                        .addOnSuccessListener { onComplete("member") }
                        .addOnFailureListener { onComplete("member") } // fail-safe
                }
            }
            .addOnFailureListener {
                onComplete("member") // never crash UI
            }
    }
}
